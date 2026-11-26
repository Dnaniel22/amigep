import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * AMIGEP - Pruebas funcionales y de seguridad de la API REST (Proyecto Final).
 *
 * Requisitos: JDK 17 o superior y el backend en ejecución. No usa librerías externas.
 *
 * Uso (desde la carpeta raíz del repositorio):
 *   java pruebas/PruebasAmigep.java                                  -> http://localhost:8080, entorno de pruebas
 *   java pruebas/PruebasAmigep.java http://192.168.1.20:8080         -> otro servidor
 *   java pruebas/PruebasAmigep.java http://localhost:8080 --produccion
 *        -> además exige la configuración de producción (perfil prod): sin código de recuperación en la
 *           respuesta, sin Swagger público y sin la clave JWT de desarrollo.
 *
 * Credenciales: por defecto usa los usuarios de los datos de prueba (README). Se pueden cambiar con las
 * variables de entorno AMIGEP_ADMIN_CORREO, AMIGEP_ADMIN_CLAVE, AMIGEP_ENTRENADOR_CORREO,
 * AMIGEP_ENTRENADOR_CLAVE, AMIGEP_CLIENTE_CORREO y AMIGEP_CLIENTE_CLAVE.
 *
 * Crea un cliente de prueba ("Prueba QA"), ejecuta las pruebas y al final lo desactiva (baja lógica).
 * Genera el reporte pruebas/reportes/reporte-pruebas-AAAAMMDD-HHMMSS.html con resultados y métricas.
 */
public class PruebasAmigep {

    // ------------------------------------------------------------------ configuración

    static final String CLAVE_JWT_DESARROLLO = "amigep-clave-de-desarrollo-cambiar-en-produccion-2026";
    static final long UMBRAL_MS = 2000; // RNF-06: tiempo de respuesta aceptable por solicitud

    static String base = "http://localhost:8080";
    static boolean produccion = false;

    static String env(String nombre, String porDefecto) {
        String v = System.getenv(nombre);
        return v == null || v.isBlank() ? porDefecto : v;
    }

    static final String ADMIN_CORREO = env("AMIGEP_ADMIN_CORREO", "admin@angelsgym.pe");
    static final String ADMIN_CLAVE = env("AMIGEP_ADMIN_CLAVE", "Admin123*");
    static final String ENT_CORREO = env("AMIGEP_ENTRENADOR_CORREO", "carlos.bustamante@angelsgym.pe");
    static final String ENT_CLAVE = env("AMIGEP_ENTRENADOR_CLAVE", "Entrenador123*");
    static final String CLI_CORREO = env("AMIGEP_CLIENTE_CORREO", "luis.atocsa@amigep.pe");
    static final String CLI_CLAVE = env("AMIGEP_CLIENTE_CLAVE", "Cliente123*");

    // ------------------------------------------------------------------ resultados y métricas

    enum Estado { PASA, FALLA, ADVERTENCIA, OMITIDA }

    record Obtenido(Estado estado, String detalle) {
    }

    record Resultado(String id, String tipo, String referencia, String descripcion, String esperado,
                     Estado estado, String obtenido, long ms) {
    }

    record Medicion(String etiqueta, int status, long ms) {
    }

    static final List<Resultado> resultados = new ArrayList<>();
    static final List<Medicion> mediciones = new ArrayList<>();
    static long msPruebaActual;

    static Obtenido pasa(String d) { return new Obtenido(Estado.PASA, d); }
    static Obtenido falla(String d) { return new Obtenido(Estado.FALLA, d); }
    static Obtenido advertencia(String d) { return new Obtenido(Estado.ADVERTENCIA, d); }
    static Obtenido omitida(String d) { return new Obtenido(Estado.OMITIDA, d); }

    static void prueba(String id, String tipo, String referencia, String descripcion, String esperado,
                       Supplier<Obtenido> cuerpo) {
        msPruebaActual = 0;
        Obtenido o;
        try {
            o = cuerpo.get();
        } catch (RuntimeException e) {
            o = falla("Error al ejecutar la prueba: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        Resultado r = new Resultado(id, tipo, referencia, descripcion, esperado, o.estado(), o.detalle(), msPruebaActual);
        resultados.add(r);
        System.out.printf("[%-11s] %-6s %-14s %s%n", o.estado(), id, referencia, descripcion);
        if (o.estado() != Estado.PASA) System.out.println("              -> " + o.detalle());
        if (EN_GITHUB && (o.estado() == Estado.FALLA || o.estado() == Estado.ADVERTENCIA)) {
            System.out.println("::" + (o.estado() == Estado.FALLA ? "error" : "warning") + " title=" + anotacion(id + " " + descripcion)
                    + "::" + anotacion(o.detalle()));
        }
    }

    /** En GitHub Actions las fallas y advertencias se muestran como anotaciones del pull request. */
    static final boolean EN_GITHUB = "true".equals(System.getenv("GITHUB_ACTIONS"));

    static String anotacion(String t) {
        return t == null ? "" : t.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A").replace(":", "%3A").replace(",", "%2C");
    }

    // ------------------------------------------------------------------ HTTP

    static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    record Resp(int status, String body, Map<String, List<String>> headers, long ms) {
        Object json() {
            try {
                return Json.parse(body);
            } catch (RuntimeException e) {
                return null;
            }
        }

        String header(String nombre) {
            for (var e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(nombre) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }

        String resumen() {
            String b = body == null ? "" : body.replaceAll("\\s+", " ");
            return "HTTP " + status + (b.isEmpty() ? "" : " " + (b.length() > 160 ? b.substring(0, 160) + "\u2026" : b));
        }
    }

    static Resp llamar(String metodo, String ruta, String token, Object cuerpo, String etiqueta) {
        return llamarCon(metodo, ruta, token == null ? null : "Bearer " + token, cuerpo == null ? null : Json.escribir(cuerpo), etiqueta);
    }

    static Resp llamarCon(String metodo, String ruta, String authorization, String cuerpoTexto, String etiqueta) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json");
        if (authorization != null) b.header("Authorization", authorization);
        if (cuerpoTexto != null) {
            b.header("Content-Type", "application/json; charset=UTF-8");
            b.method(metodo, HttpRequest.BodyPublishers.ofString(cuerpoTexto, StandardCharsets.UTF_8));
        } else {
            b.method(metodo, HttpRequest.BodyPublishers.noBody());
        }
        long inicio = System.nanoTime();
        Resp r;
        try {
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            r = new Resp(res.statusCode(), res.body(), res.headers().map(), ms);
        } catch (IOException e) {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            r = new Resp(0, "Sin conexi\u00f3n: " + e.getMessage(), Map.of(), ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        msPruebaActual += r.ms();
        if (r.status() > 0) mediciones.add(new Medicion(etiqueta, r.status(), r.ms()));
        return r;
    }

    static String q(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    static Obtenido estado(Resp r, int... esperados) {
        for (int e : esperados) if (r.status() == e) return pasa(r.resumen());
        return falla(r.resumen());
    }

    // ------------------------------------------------------------------ JSON mínimo (sin librerías)

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String texto) {
            if (texto == null || texto.isBlank()) return null;
            Json p = new Json(texto.trim());
            Object v = p.valor();
            p.espacios();
            if (p.i != p.s.length()) throw new IllegalArgumentException("JSON con datos sobrantes");
            return v;
        }

        private void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private Object valor() {
            espacios();
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return arreglo();
            if (c == '"') return cadena();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numero();
        }

        private Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacios();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacios();
                String k = cadena();
                espacios();
                if (s.charAt(i) != ':') throw new IllegalArgumentException("Se esperaba ':'");
                i++;
                m.put(k, valor());
                espacios();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("Se esperaba ',' o '}'");
            }
        }

        private List<Object> arreglo() {
            List<Object> l = new ArrayList<>();
            i++;
            espacios();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor());
                espacios();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("Se esperaba ',' o ']'");
            }
        }

        private String cadena() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("Se esperaba una cadena");
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Object numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(ini, i);
            if (n.isEmpty()) throw new IllegalArgumentException("Valor JSON no v\u00e1lido");
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
            return Long.parseLong(n);
        }

        static String escribir(Object o) {
            if (o == null) return "null";
            if (o instanceof String t) return "\"" + escapar(t) + "\"";
            if (o instanceof Number || o instanceof Boolean) return String.valueOf(o);
            if (o instanceof Map<?, ?> m) {
                StringBuilder sb = new StringBuilder("{");
                boolean primero = true;
                for (var e : m.entrySet()) {
                    if (!primero) sb.append(',');
                    primero = false;
                    sb.append('"').append(escapar(String.valueOf(e.getKey()))).append("\":").append(escribir(e.getValue()));
                }
                return sb.append('}').toString();
            }
            if (o instanceof List<?> l) {
                StringBuilder sb = new StringBuilder("[");
                for (int k = 0; k < l.size(); k++) {
                    if (k > 0) sb.append(',');
                    sb.append(escribir(l.get(k)));
                }
                return sb.append(']').toString();
            }
            return "\"" + escapar(o.toString()) + "\"";
        }

        static String escapar(String t) {
            StringBuilder sb = new StringBuilder();
            for (char c : t.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.toString();
        }
    }

    /** Navega un JSON: en("resumen.codigo"), en("0.idCliente"). Devuelve null si no existe. */
    static Object en(Object json, String ruta) {
        Object actual = json;
        for (String parte : ruta.split("\\.")) {
            if (actual instanceof Map<?, ?> m) {
                actual = m.get(parte);
            } else if (actual instanceof List<?> l && parte.matches("\\d+")) {
                int k = Integer.parseInt(parte);
                actual = k < l.size() ? l.get(k) : null;
            } else {
                return null;
            }
        }
        return actual;
    }

    static String texto(Object json, String ruta) {
        Object v = en(json, ruta);
        return v == null ? null : String.valueOf(v);
    }

    static Integer entero(Object json, String ruta) {
        Object v = en(json, ruta);
        return v instanceof Number n ? n.intValue() : null;
    }

    static double numero(Object json, String ruta) {
        Object v = en(json, ruta);
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    static List<?> lista(Object json, String ruta) {
        Object v = ruta == null || ruta.isEmpty() ? json : en(json, ruta);
        return v instanceof List<?> l ? l : List.of();
    }

    static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int k = 0; k + 1 < pares.length; k += 2) m.put(String.valueOf(pares[k]), pares[k + 1]);
        return m;
    }

    // ------------------------------------------------------------------ JWT de prueba (para las pruebas de seguridad)

    static String b64(String t) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(t.getBytes(StandardCharsets.UTF_8));
    }

    static String firmarHs256(String cabecera, String carga, String clave) {
        try {
            String datos = b64(cabecera) + "." + b64(carga);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String firma = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
            return datos + "." + firma;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String cargaJwt(Integer id, String rol, long expiraEpoch) {
        long ahora = System.currentTimeMillis() / 1000;
        return Json.escribir(mapa("sub", String.valueOf(id == null ? 1 : id), "rol", rol, "nombre", "Prueba QA",
                "email", "qa@amigep.pe", "iat", ahora, "exp", expiraEpoch));
    }

    // ------------------------------------------------------------------ estado compartido entre pruebas

    static String tokenAdmin, tokenEntrenador, tokenCliente, tokenQa;
    static Integer idAdmin, idEntrenador, idClienteDemo, idPlan, idClienteQa, idRutinaQa, idPrediccionQa, idOtroCliente;
    static String codigoQrQa;
    static final String SUFIJO = String.valueOf(System.currentTimeMillis() % 10_000_000);
    static final String DNI_QA = "9" + String.format("%07d", Long.parseLong(SUFIJO));
    static final String CORREO_QA = "prueba.qa" + SUFIJO + "@amigep.pe";
    static final String CLAVE_QA = "PruebaQa123*";

    static Resp login(String correo, String clave, String rol, String etiqueta) {
        return llamar("POST", "/api/auth/login", null, mapa("correo", correo, "contrasena", clave, "rol", rol), etiqueta);
    }

    static Map<String, Object> clienteQa(String nombre, Integer entrenador, Integer plan) {
        return mapa("nombre", nombre, "apellido", "Prueba QA", "dni", DNI_QA, "email", CORREO_QA,
                "telefono", "987654321", "objetivo", "P\u00e9rdida de grasa", "nivel", "INTERMEDIO",
                "altura", 1.72, "fechaNacimiento", "1998-05-14", "idEntrenador", entrenador,
                "contrasena", CLAVE_QA, "idPlan", plan, "metodoPago", "EFECTIVO");
    }

    // ------------------------------------------------------------------ pruebas funcionales

    static void funcionales() {
        String F = "Funcional";

        prueba("PF-01", F, "RF-01", "Inicio de sesi\u00f3n del administrador", "200 con token y rol ADMINISTRADOR", () -> {
            Resp r = login(ADMIN_CORREO, ADMIN_CLAVE, "ADMINISTRADOR", "POST /api/auth/login");
            Object j = r.json();
            tokenAdmin = texto(j, "token");
            idAdmin = entero(j, "id");
            return r.status() == 200 && tokenAdmin != null && "ADMINISTRADOR".equals(texto(j, "rol"))
                    ? pasa("HTTP 200, token emitido, expira en " + texto(j, "expiraEnSegundos") + " s") : falla(r.resumen());
        });

        prueba("PF-02", F, "RF-01", "Inicio de sesi\u00f3n del entrenador", "200 con token y rol ENTRENADOR", () -> {
            Resp r = login(ENT_CORREO, ENT_CLAVE, "ENTRENADOR", "POST /api/auth/login");
            tokenEntrenador = texto(r.json(), "token");
            idEntrenador = entero(r.json(), "id");
            return r.status() == 200 && tokenEntrenador != null ? pasa("HTTP 200, rol " + texto(r.json(), "rol")) : falla(r.resumen());
        });

        prueba("PF-03", F, "RF-01", "Inicio de sesi\u00f3n del cliente", "200 con token y rol CLIENTE", () -> {
            Resp r = login(CLI_CORREO, CLI_CLAVE, "CLIENTE", "POST /api/auth/login");
            tokenCliente = texto(r.json(), "token");
            idClienteDemo = entero(r.json(), "id");
            return r.status() == 200 && tokenCliente != null ? pasa("HTTP 200, cliente " + texto(r.json(), "nombre")) : falla(r.resumen());
        });

        prueba("PF-04", F, "RF-01", "Contrase\u00f1a incorrecta", "401 \u00abCorreo o contrase\u00f1a incorrectos\u00bb", () -> {
            Resp r = login(ADMIN_CORREO, "ClaveEquivocada1", "ADMINISTRADOR", "POST /api/auth/login");
            return r.status() == 401 ? pasa("HTTP 401: " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });

        prueba("PF-05", F, "RF-02", "Datos del usuario de la sesi\u00f3n", "200 con el rol del token", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/auth/yo", tokenAdmin, null, "GET /api/auth/yo");
            return r.status() == 200 && "ADMINISTRADOR".equals(texto(r.json(), "rol")) ? pasa("HTTP 200, rol ADMINISTRADOR") : falla(r.resumen());
        });

        prueba("PF-06", F, "RF-04", "Cat\u00e1logo de planes", "200 con al menos un plan", () -> {
            Resp r = llamar("GET", "/api/planes", null, null, "GET /api/planes");
            List<?> planes = lista(r.json(), "");
            if (!planes.isEmpty()) idPlan = entero(planes.get(0), "idPlan");
            return r.status() == 200 && idPlan != null ? pasa("HTTP 200, " + planes.size() + " planes") : falla(r.resumen());
        });

        prueba("PF-07", F, "RF-07", "Lista de entrenadores", "200 con el entrenador de prueba", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/admin/trabajadores?rol=ENTRENADOR", tokenAdmin, null, "GET /api/admin/trabajadores");
            List<?> l = lista(r.json(), "");
            if (idEntrenador == null) {
                for (Object t : l) if (ENT_CORREO.equalsIgnoreCase(texto(t, "email"))) idEntrenador = entero(t, "idTrabajador");
            }
            return r.status() == 200 && !l.isEmpty() ? pasa("HTTP 200, " + l.size() + " entrenadores") : falla(r.resumen());
        });

        prueba("PF-08", F, "RF-03, RF-05", "Registrar cliente con plan y primer pago", "201 con c\u00f3digo QR y membres\u00eda", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("POST", "/api/admin/clientes", tokenAdmin, clienteQa("Cliente", idEntrenador, idPlan), "POST /api/admin/clientes");
            Object j = r.json();
            idClienteQa = entero(j, "resumen.idCliente");
            codigoQrQa = texto(j, "codigoQR");
            int membresias = lista(j, "membresias").size();
            return r.status() == 201 && idClienteQa != null && codigoQrQa != null && membresias > 0
                    ? pasa("HTTP 201, cliente " + texto(j, "resumen.codigo") + ", membres\u00eda " + texto(j, "resumen.estadoMembresia"))
                    : falla(r.resumen());
        });

        prueba("PF-09", F, "RNF-05", "Validaci\u00f3n del formulario de cliente vac\u00edo", "400 con mensajes por campo", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("POST", "/api/admin/clientes", tokenAdmin, mapa(), "POST /api/admin/clientes");
            Object campos = en(r.json(), "campos");
            return r.status() == 400 && campos instanceof Map<?, ?> m && m.containsKey("dni") && m.containsKey("email")
                    ? pasa("HTTP 400, campos observados: " + ((Map<?, ?>) campos).keySet()) : falla(r.resumen());
        });

        prueba("PF-10", F, "RF-03", "DNI duplicado", "400 indicando el campo dni", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Map<String, Object> c = clienteQa("Duplicado", idEntrenador, null);
            c.put("email", "otro" + CORREO_QA);
            Resp r = llamar("POST", "/api/admin/clientes", tokenAdmin, c, "POST /api/admin/clientes");
            return (r.status() == 400 || r.status() == 409) ? pasa(r.resumen()) : falla(r.resumen());
        });

        prueba("PF-11", F, "RF-03", "Buscar cliente por DNI", "200 con exactamente un resultado", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("GET", "/api/admin/clientes?q=" + DNI_QA, tokenAdmin, null, "GET /api/admin/clientes");
            int n = lista(r.json(), "").size();
            return r.status() == 200 && n == 1 ? pasa("HTTP 200, 1 cliente") : falla("HTTP " + r.status() + ", " + n + " resultados");
        });

        prueba("PF-12", F, "RF-03", "Editar datos del cliente", "200 con el tel\u00e9fono actualizado", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Map<String, Object> c = clienteQa("Cliente", idEntrenador, null);
            c.put("telefono", "912345678");
            c.remove("contrasena");
            Resp r = llamar("PUT", "/api/admin/clientes/" + idClienteQa, tokenAdmin, c, "PUT /api/admin/clientes/{id}");
            return r.status() == 200 && "912345678".equals(texto(r.json(), "resumen.telefono")) ? pasa("HTTP 200, tel\u00e9fono 912345678") : falla(r.resumen());
        });

        prueba("PF-13", F, "RF-05", "Registrar pago de renovaci\u00f3n", "201 con pago VALIDADO", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("POST", "/api/admin/pagos", tokenAdmin,
                    mapa("idCliente", idClienteQa, "idPlan", idPlan, "metodoPago", "YAPE_PLIN"), "POST /api/admin/pagos");
            return r.status() == 201 && "VALIDADO".equals(texto(r.json(), "pago.estado"))
                    ? pasa("HTTP 201, " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });

        prueba("PF-14", F, "RF-05", "Consultar pagos del cliente", "200 con los 2 pagos registrados", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("GET", "/api/admin/pagos?q=" + q("Prueba QA"), tokenAdmin, null, "GET /api/admin/pagos");
            int n = 0;
            for (Object p : lista(r.json(), "")) if (idClienteQa.equals(entero(p, "idCliente"))) n++;
            return r.status() == 200 && n >= 2 ? pasa("HTTP 200, " + n + " pagos") : falla("HTTP " + r.status() + ", " + n + " pagos");
        });

        prueba("PF-15", F, "RF-06, RF-17", "Ingreso con c\u00f3digo QR", "200 con acceso permitido", () -> {
            if (tokenAdmin == null || codigoQrQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("POST", "/api/admin/accesos/qr", tokenAdmin, mapa("codigoQR", codigoQrQa), "POST /api/admin/accesos/qr");
            return r.status() == 200 && Boolean.TRUE.equals(en(r.json(), "permitido"))
                    ? pasa("HTTP 200, " + texto(r.json(), "motivo") + ": " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });

        prueba("PF-16", F, "RF-17", "Segundo ingreso del d\u00eda por DNI", "200 con motivo YA_REGISTRADO", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("POST", "/api/admin/accesos/manual", tokenAdmin, mapa("dni", DNI_QA), "POST /api/admin/accesos/manual");
            return r.status() == 200 && "YA_REGISTRADO".equals(texto(r.json(), "motivo")) ? pasa("HTTP 200, YA_REGISTRADO") : falla(r.resumen());
        });

        prueba("PF-17", F, "RF-17", "C\u00f3digo QR inexistente", "200 con acceso denegado (NO_IDENTIFICADO)", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("POST", "/api/admin/accesos/qr", tokenAdmin, mapa("codigoQR", "QR-NO-EXISTE-" + SUFIJO), "POST /api/admin/accesos/qr");
            return r.status() == 200 && Boolean.FALSE.equals(en(r.json(), "permitido")) ? pasa("HTTP 200, " + texto(r.json(), "motivo")) : falla(r.resumen());
        });

        prueba("PF-18", F, "RF-15", "Panel general (dashboard)", "200 con indicadores", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/admin/dashboard", tokenAdmin, null, "GET /api/admin/dashboard");
            return r.status() == 200 && en(r.json(), "clientesActivos") != null
                    ? pasa("HTTP 200, " + texto(r.json(), "clientesActivos") + " clientes activos, " + texto(r.json(), "asistenciasHoy") + " asistencias hoy")
                    : falla(r.resumen());
        });

        prueba("PF-19", F, "RF-01", "Inicio de sesi\u00f3n del cliente nuevo", "200 con token", () -> {
            if (idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = login(CORREO_QA, CLAVE_QA, "CLIENTE", "POST /api/auth/login");
            tokenQa = texto(r.json(), "token");
            return r.status() == 200 && tokenQa != null ? pasa("HTTP 200") : falla(r.resumen());
        });

        prueba("PF-20", F, "RF-10, RF-11", "Generar rutina personalizada con IA", "200, rutina IA con 4 d\u00edas", () -> {
            if (tokenQa == null) return omitida("Requiere PF-19");
            Resp r = llamar("POST", "/api/cliente/rutinas/generar", tokenQa,
                    mapa("objetivo", "P\u00e9rdida de grasa", "nivel", "INTERMEDIO", "sesionesPorSemana", 4, "peso", 84.5, "altura", 1.72),
                    "POST /api/cliente/rutinas/generar");
            Object j = r.json();
            idRutinaQa = entero(j, "idRutina");
            int dias = lista(j, "contenido.dias").size();
            return r.status() == 200 && "IA".equals(texto(j, "origen")) && dias == 4
                    ? pasa("HTTP 200, \u00ab" + texto(j, "nombre") + "\u00bb, " + dias + " d\u00edas, origen IA") : falla(r.resumen());
        });

        prueba("PF-21", F, "RF-09", "Registrar sesi\u00f3n de entrenamiento completada", "200 con la sesi\u00f3n", () -> {
            if (tokenQa == null || idRutinaQa == null) return omitida("Requiere PF-20");
            Resp r = llamar("POST", "/api/cliente/rutinas/" + idRutinaQa + "/sesiones", tokenQa,
                    mapa("completada", true, "observaciones", "Sesi\u00f3n de prueba"), "POST /api/cliente/rutinas/{id}/sesiones");
            return estado(r, 200, 201);
        });

        prueba("PF-22", F, "RF-13", "Generar plan alimenticio", "200 con meta cal\u00f3rica", () -> {
            if (tokenQa == null) return omitida("Requiere PF-19");
            Resp r = llamar("POST", "/api/cliente/alimentacion/generar", tokenQa, mapa("objetivo", "P\u00e9rdida de grasa"),
                    "POST /api/cliente/alimentacion/generar");
            Integer kcal = entero(r.json(), "plan.caloriasObjetivo");
            return r.status() == 200 && kcal != null && kcal > 0 ? pasa("HTTP 200, meta " + kcal + " kcal") : falla(r.resumen());
        });

        prueba("PF-23", F, "RF-12", "Registrar comida que excede la meta", "200 con alerta y sugerencia", () -> {
            if (tokenQa == null) return omitida("Requiere PF-19");
            Resp r = llamar("POST", "/api/cliente/alimentacion/comidas", tokenQa,
                    mapa("tipoComida", "DESAYUNO", "descripcion", "Desayuno de prueba", "calorias", 2500,
                            "proteinas", 20, "carbohidratos", 300, "grasas", 90), "POST /api/cliente/alimentacion/comidas");
            String msg = texto(r.json(), "mensaje");
            return r.status() == 200 && Boolean.FALSE.equals(en(r.json(), "cumpleObjetivo")) && msg != null && msg.startsWith("Alerta")
                    ? pasa("HTTP 200: " + msg) : falla(r.resumen());
        });

        prueba("PF-24", F, "RF-14", "Registrar medici\u00f3n de progreso", "200 con el peso actual", () -> {
            if (tokenQa == null) return omitida("Requiere PF-19");
            Resp r = llamar("POST", "/api/cliente/progreso", tokenQa, mapa("peso", 83.9, "grasaCorporal", 24.5, "observaciones", "Medici\u00f3n de prueba"),
                    "POST /api/cliente/progreso");
            return r.status() == 200 && en(r.json(), "pesoActual") != null ? pasa("HTTP 200, peso actual " + texto(r.json(), "pesoActual") + " kg") : falla(r.resumen());
        });

        prueba("PF-25", F, "RF-07, RF-16", "Entrenador consulta sus clientes", "200 con el cliente nuevo en la lista", () -> {
            if (tokenEntrenador == null) return omitida("Sin token de entrenador");
            Resp r = llamar("GET", "/api/entrenador/clientes", tokenEntrenador, null, "GET /api/entrenador/clientes");
            boolean esta = false;
            for (Object c : lista(r.json(), "clientes")) if (idClienteQa != null && idClienteQa.equals(entero(c, "idCliente"))) esta = true;
            return r.status() == 200 && (esta || idClienteQa == null) ? pasa("HTTP 200, " + texto(r.json(), "total") + " clientes asignados") : falla(r.resumen());
        });

        prueba("PF-26", F, "RF-08", "Entrenador asigna una rutina", "200 con rutina de origen ENTRENADOR", () -> {
            if (tokenEntrenador == null || idClienteQa == null) return omitida("Requiere PF-02 y PF-08");
            Map<String, Object> ejercicio = mapa("nombre", "Sentadilla", "series", 4, "repeticiones", "12", "descansoSeg", 60);
            Map<String, Object> dia = mapa("nombre", "Pierna", "ejercicios", List.of(ejercicio));
            Resp r = llamar("POST", "/api/entrenador/clientes/" + idClienteQa + "/rutinas", tokenEntrenador,
                    mapa("nombre", "Rutina de prueba QA", "objetivo", "P\u00e9rdida de grasa", "nivel", "INTERMEDIO",
                            "sesionesPorSemana", 3, "semanas", 4, "dias", List.of(dia), "notas", List.of("Prueba")),
                    "POST /api/entrenador/clientes/{id}/rutinas");
            return (r.status() == 200 || r.status() == 201) && "ENTRENADOR".equals(texto(r.json(), "origen"))
                    ? pasa("HTTP " + r.status() + ", rutina \u00ab" + texto(r.json(), "nombre") + "\u00bb") : falla(r.resumen());
        });

        prueba("PF-27", F, "RF-16", "Entrenador consulta la alimentaci\u00f3n del cliente", "200", () -> {
            if (tokenEntrenador == null || idClienteQa == null) return omitida("Requiere PF-02 y PF-08");
            return estado(llamar("GET", "/api/entrenador/clientes/" + idClienteQa + "/alimentacion", tokenEntrenador, null,
                    "GET /api/entrenador/clientes/{id}/alimentacion"), 200);
        });

        prueba("PF-28", F, "RF-18", "Actualizar predicciones de abandono", "200 con clientes evaluados", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("POST", "/api/admin/retencion/predicciones/actualizar", tokenAdmin, null, "POST /api/admin/retencion/predicciones/actualizar");
            return r.status() == 200 && entero(r.json(), "clientesEvaluados") != null
                    ? pasa("HTTP 200, " + texto(r.json(), "clientesEvaluados") + " clientes evaluados " + texto(r.json(), "porEstado")) : falla(r.resumen());
        });

        prueba("PF-29", F, "RF-18", "Consultar clientes en riesgo", "200 con probabilidad y factores", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/admin/retencion/predicciones", tokenAdmin, null, "GET /api/admin/retencion/predicciones");
            List<?> l = lista(r.json(), "");
            for (Object p : l) {
                if (idClienteQa != null && idClienteQa.equals(entero(p, "idCliente"))) idPrediccionQa = entero(p, "idPrediccion");
            }
            if (idPrediccionQa == null && !l.isEmpty()) idPrediccionQa = entero(l.get(0), "idPrediccion");
            return r.status() == 200 && !l.isEmpty() ? pasa("HTTP 200, " + l.size() + " predicciones") : falla(r.resumen());
        });

        prueba("PF-30", F, "RF-19", "Registrar acci\u00f3n de retenci\u00f3n con notificaci\u00f3n", "201 y el cliente recibe la notificaci\u00f3n", () -> {
            if (tokenAdmin == null || idPrediccionQa == null) return omitida("Requiere PF-29");
            String titulo = "Te esperamos " + SUFIJO;
            Resp r = llamar("POST", "/api/admin/retencion/acciones", tokenAdmin,
                    mapa("idPrediccion", idPrediccionQa, "tipoAccion", "Mensaje motivacional", "descripcion", "Acci\u00f3n de prueba",
                            "enviarNotificacion", true, "tituloNotificacion", titulo, "mensajeNotificacion", "Mensaje de prueba QA"),
                    "POST /api/admin/retencion/acciones");
            if (r.status() != 201) return falla(r.resumen());
            if (tokenQa == null || idClienteQa == null || !idClienteQa.equals(entero(r.json(), "idCliente"))) return pasa("HTTP 201, acci\u00f3n registrada");
            Resp n = llamar("GET", "/api/cliente/notificaciones", tokenQa, null, "GET /api/cliente/notificaciones");
            boolean recibida = false;
            for (Object x : lista(n.json(), "")) if (titulo.equals(texto(x, "titulo"))) recibida = true;
            return recibida ? pasa("HTTP 201; el cliente ve la notificaci\u00f3n \u00ab" + titulo + "\u00bb") : falla("La notificaci\u00f3n no lleg\u00f3 al cliente: " + n.resumen());
        });

        String[][] reportes = {{"RP-01", "admin"}, {"RP-02", "admin"}, {"RP-03", "admin"}, {"RP-04", "admin"},
                {"RP-05", "admin"}, {"RP-06", "cliente"}, {"RP-07", "cliente"}, {"RP-08", "admin"}};
        int k = 31;
        for (String[] rp : reportes) {
            String codigo = rp[0];
            boolean admin = rp[1].equals("admin");
            prueba("PF-" + k++, F, "RF-20", "Reporte " + codigo + (admin ? " (administrador)" : " (cliente)"), "200 con columnas y filas", () -> {
                String token = admin ? tokenAdmin : (tokenQa != null ? tokenQa : tokenCliente);
                if (token == null) return omitida("Sin token");
                Resp r = llamar("GET", "/api/reportes/" + codigo, token, null, "GET /api/reportes/{codigo}");
                return r.status() == 200 && en(r.json(), "columnas") != null
                        ? pasa("HTTP 200, \u00ab" + texto(r.json(), "titulo") + "\u00bb, " + lista(r.json(), "filas").size() + " filas") : falla(r.resumen());
            });
        }

        prueba("PF-39", F, "RF-20", "Exportar reporte RP-03 a CSV", "200 con Content-Type text/csv", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/reportes/RP-03?formato=csv", tokenAdmin, null, "GET /api/reportes/{codigo}");
            String tipo = r.header("Content-Type");
            return r.status() == 200 && tipo != null && tipo.startsWith("text/csv") ? pasa("HTTP 200, " + tipo) : falla(r.resumen());
        });
    }

    // ------------------------------------------------------------------ pruebas de seguridad (OWASP Top 10:2021)

    static void seguridad() {
        String S = "Seguridad";

        prueba("PS-01", S, "A01 Control de acceso", "Endpoint protegido sin token", "401", () ->
                estado(llamar("GET", "/api/admin/dashboard", null, null, "GET /api/admin/dashboard"), 401));

        prueba("PS-02", S, "A01 Control de acceso", "Cliente intenta usar el panel del administrador", "403", () -> {
            if (tokenCliente == null) return omitida("Sin token de cliente");
            return estado(llamar("GET", "/api/admin/clientes", tokenCliente, null, "GET /api/admin/clientes"), 403);
        });

        prueba("PS-03", S, "A01 Control de acceso", "Entrenador intenta usar el panel del administrador", "403", () -> {
            if (tokenEntrenador == null) return omitida("Sin token de entrenador");
            return estado(llamar("GET", "/api/admin/pagos", tokenEntrenador, null, "GET /api/admin/pagos"), 403);
        });

        prueba("PS-04", S, "A01 Control de acceso", "Cliente intenta usar endpoints del entrenador", "403", () -> {
            if (tokenCliente == null) return omitida("Sin token de cliente");
            return estado(llamar("GET", "/api/entrenador/clientes", tokenCliente, null, "GET /api/entrenador/clientes"), 403);
        });

        prueba("PS-05", S, "A01 Control de acceso", "Administrador intenta usar endpoints del cliente", "403", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            return estado(llamar("GET", "/api/cliente/inicio", tokenAdmin, null, "GET /api/cliente/inicio"), 403);
        });

        prueba("PS-06", S, "A01 Control de acceso", "Entrenador consulta un cliente que no tiene asignado (IDOR)", "403", () -> {
            if (tokenEntrenador == null || tokenAdmin == null) return omitida("Requiere tokens de entrenador y administrador");
            Resp todos = llamar("GET", "/api/admin/clientes", tokenAdmin, null, "GET /api/admin/clientes");
            for (Object c : lista(todos.json(), "")) {
                Integer ent = entero(c, "idEntrenador");
                if (ent == null || !ent.equals(idEntrenador)) { idOtroCliente = entero(c, "idCliente"); break; }
            }
            if (idOtroCliente == null) return omitida("No hay clientes de otro entrenador");
            return estado(llamar("GET", "/api/entrenador/clientes/" + idOtroCliente, tokenEntrenador, null, "GET /api/entrenador/clientes/{id}"), 403);
        });

        prueba("PS-07", S, "A01 Control de acceso", "Cliente pide un reporte reservado al administrador (RP-01)", "403", () -> {
            if (tokenCliente == null) return omitida("Sin token de cliente");
            return estado(llamar("GET", "/api/reportes/RP-01", tokenCliente, null, "GET /api/reportes/{codigo}"), 403);
        });

        prueba("PS-08", S, "A01 Control de acceso", "Cliente marca como le\u00edda una notificaci\u00f3n de otro cliente", "403", () -> {
            if (tokenCliente == null || tokenAdmin == null) return omitida("Requiere tokens de cliente y administrador");
            Resp todas = llamar("GET", "/api/admin/notificaciones", tokenAdmin, null, "GET /api/admin/notificaciones");
            Integer ajena = null;
            for (Object n : lista(todas.json(), "")) {
                Integer duenio = entero(n, "idCliente");
                if (duenio != null && !duenio.equals(idClienteDemo)) { ajena = entero(n, "idNotificacion"); break; }
            }
            if (ajena == null) return omitida("No hay notificaciones de otros clientes");
            return estado(llamar("PATCH", "/api/cliente/notificaciones/" + ajena + "/leida", tokenCliente, null,
                    "PATCH /api/cliente/notificaciones/{id}/leida"), 403);
        });

        prueba("PS-09", S, "A07 Autenticaci\u00f3n", "Token alterado: se cambia el rol a ADMINISTRADOR sin volver a firmar", "401", () -> {
            if (tokenCliente == null) return omitida("Sin token de cliente");
            String[] partes = tokenCliente.split("\\.");
            String carga = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                    .replace("\"CLIENTE\"", "\"ADMINISTRADOR\"");
            String alterado = partes[0] + "." + b64(carga) + "." + partes[2];
            return estado(llamar("GET", "/api/admin/dashboard", alterado, null, "GET /api/admin/dashboard"), 401);
        });

        prueba("PS-10", S, "A07 Autenticaci\u00f3n", "Token sin firma (alg: none)", "401", () -> {
            long exp = System.currentTimeMillis() / 1000 + 3600;
            String token = b64("{\"alg\":\"none\"}") + "." + b64(cargaJwt(idAdmin, "ADMINISTRADOR", exp)) + ".";
            return estado(llamar("GET", "/api/admin/dashboard", token, null, "GET /api/admin/dashboard"), 401);
        });

        prueba("PS-11", S, "A07 Autenticaci\u00f3n", "Token vencido", "401", () -> {
            long exp = System.currentTimeMillis() / 1000 - 3600;
            String token = firmarHs256("{\"alg\":\"HS256\"}", cargaJwt(idAdmin, "ADMINISTRADOR", exp), CLAVE_JWT_DESARROLLO);
            return estado(llamar("GET", "/api/admin/dashboard", token, null, "GET /api/admin/dashboard"), 401);
        });

        prueba("PS-12", S, "A02 Fallas criptogr\u00e1ficas", "Token falsificado con la clave JWT de desarrollo (publicada en el repositorio)",
                produccion ? "401" : "401 en producci\u00f3n (en desarrollo se acepta)", () -> {
            long exp = System.currentTimeMillis() / 1000 + 3600;
            String token = firmarHs256("{\"alg\":\"HS256\"}", cargaJwt(idAdmin, "ADMINISTRADOR", exp), CLAVE_JWT_DESARROLLO);
            Resp r = llamar("GET", "/api/admin/dashboard", token, null, "GET /api/admin/dashboard");
            if (r.status() == 401) return pasa("HTTP 401: el servidor usa una clave JWT propia");
            String d = "HTTP " + r.status() + ": el servidor usa la clave de desarrollo. En producci\u00f3n defina JWT_SECRET.";
            return produccion ? falla(d) : advertencia(d);
        });

        prueba("PS-13", S, "A03 Inyecci\u00f3n", "Inyecci\u00f3n SQL en el inicio de sesi\u00f3n", "400 o 401, nunca 200", () -> {
            Resp r = login("' OR '1'='1' --", "' OR '1'='1' --", "ADMINISTRADOR", "POST /api/auth/login");
            Resp r2 = login("admin@angelsgym.pe' --", "x' OR 'x'='x", "ADMINISTRADOR", "POST /api/auth/login");
            boolean ok = (r.status() == 400 || r.status() == 401) && (r2.status() == 400 || r2.status() == 401);
            return ok ? pasa("HTTP " + r.status() + " y HTTP " + r2.status() + ": no se obtiene sesi\u00f3n") : falla(r.resumen() + " / " + r2.resumen());
        });

        prueba("PS-14", S, "A03 Inyecci\u00f3n", "Inyecci\u00f3n SQL en la b\u00fasqueda de clientes", "200 sin resultados ni error 500", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/admin/clientes?q=" + q("' OR '1'='1' --"), tokenAdmin, null, "GET /api/admin/clientes");
            int n = lista(r.json(), "").size();
            return r.status() == 200 && n == 0 ? pasa("HTTP 200, 0 resultados: el texto se trata como dato") : falla("HTTP " + r.status() + ", " + n + " resultados");
        });

        prueba("PS-15", S, "A03 Inyecci\u00f3n", "Script en un campo de texto (XSS almacenado)", "Se devuelve como JSON con nosniff", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            String script = "<script>alert('xss')</script>";
            Map<String, Object> c = clienteQa(script, idEntrenador, null);
            c.remove("contrasena");
            Resp r = llamar("PUT", "/api/admin/clientes/" + idClienteQa, tokenAdmin, c, "PUT /api/admin/clientes/{id}");
            String tipo = r.header("Content-Type");
            String nosniff = r.header("X-Content-Type-Options");
            Map<String, Object> original = clienteQa("Cliente", idEntrenador, null);
            original.remove("contrasena");
            llamar("PUT", "/api/admin/clientes/" + idClienteQa, tokenAdmin, original, "PUT /api/admin/clientes/{id}");
            boolean ok = (r.status() == 200 || r.status() == 400) && tipo != null && tipo.startsWith("application/json") && "nosniff".equalsIgnoreCase(nosniff);
            return ok ? pasa("HTTP " + r.status() + ", " + tipo + ", X-Content-Type-Options: nosniff (el panel adem\u00e1s escapa el texto)")
                    : falla("HTTP " + r.status() + ", Content-Type " + tipo + ", nosniff " + nosniff);
        });

        prueba("PS-16", S, "A02 Fallas criptogr\u00e1ficas", "Las respuestas no exponen contrase\u00f1as ni hashes", "Sin \u00abcontrasena\u00bb ni \u00ab$2a$\u00bb", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            StringBuilder todo = new StringBuilder();
            todo.append(llamar("GET", "/api/admin/trabajadores", tokenAdmin, null, "GET /api/admin/trabajadores").body());
            if (idClienteQa != null) todo.append(llamar("GET", "/api/admin/clientes/" + idClienteQa, tokenAdmin, null, "GET /api/admin/clientes/{id}").body());
            String t = todo.toString();
            boolean expone = t.contains("$2a$") || t.contains("$2b$") || t.toLowerCase(Locale.ROOT).contains("contrasena");
            return expone ? falla("La respuesta contiene datos de contrase\u00f1a") : pasa("Ninguna respuesta incluye la contrase\u00f1a ni su hash BCrypt");
        });

        prueba("PS-17", S, "A05 Configuraci\u00f3n insegura", "Cabeceras de seguridad en la API", "nosniff, X-Frame-Options y no-cache", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/auth/yo", tokenAdmin, null, "GET /api/auth/yo");
            String a = r.header("X-Content-Type-Options"), b = r.header("X-Frame-Options"), c = r.header("Cache-Control");
            boolean ok = "nosniff".equalsIgnoreCase(a) && b != null && c != null && c.contains("no-cache");
            String d = "X-Content-Type-Options: " + a + " \u00b7 X-Frame-Options: " + b + " \u00b7 Cache-Control: " + c;
            return ok ? pasa(d) : falla(d);
        });

        prueba("PS-18", S, "A05 Configuraci\u00f3n insegura", "Errores sin detalles internos (JSON mal formado)", "400 sin traza de Java", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamarCon("POST", "/api/admin/clientes", "Bearer " + tokenAdmin, "{\"nombre\": ", "POST /api/admin/clientes");
            boolean filtra = r.body() != null && (r.body().contains("Exception") || r.body().contains("at pe.edu") || r.body().contains("\"trace\""));
            return r.status() == 400 && !filtra ? pasa("HTTP 400: " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });

        prueba("PS-19", S, "A05 Configuraci\u00f3n insegura", "Recurso inexistente", "404 sin traza de Java", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/api/admin/clientes/999999999", tokenAdmin, null, "GET /api/admin/clientes/{id}");
            boolean filtra = r.body() != null && (r.body().contains("Exception") || r.body().contains("\"trace\""));
            return r.status() == 404 && !filtra ? pasa("HTTP 404: " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });

        prueba("PS-20", S, "A04 Dise\u00f1o inseguro", "Pago con monto negativo", "400", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            return estado(llamar("POST", "/api/admin/pagos", tokenAdmin,
                    mapa("idCliente", idClienteQa, "idPlan", idPlan, "metodoPago", "EFECTIVO", "monto", -50), "POST /api/admin/pagos"), 400);
        });

        prueba("PS-21", S, "A07 Autenticaci\u00f3n", "Contrase\u00f1a nueva d\u00e9bil (menos de 8 caracteres)", "400", () -> {
            if (tokenQa == null) return omitida("Requiere PF-19");
            return estado(llamar("PUT", "/api/auth/contrasena", tokenQa, mapa("actual", CLAVE_QA, "nueva", "123"), "PUT /api/auth/contrasena"), 400);
        });

        prueba("PS-22", S, "A07 Autenticaci\u00f3n", "Recuperar contrase\u00f1a no revela el c\u00f3digo en la respuesta",
                produccion ? "Respuesta sin codigoDesarrollo" : "Sin c\u00f3digo en producci\u00f3n (en desarrollo se muestra)", () -> {
            Resp r = llamar("POST", "/api/auth/recuperar", null, mapa("correo", CLI_CORREO, "rol", "CLIENTE"), "POST /api/auth/recuperar");
            if (r.status() != 200) return falla(r.resumen());
            if (en(r.json(), "codigoDesarrollo") == null) return pasa("HTTP 200: la respuesta no incluye el c\u00f3digo");
            String d = "La respuesta incluye el c\u00f3digo de recuperaci\u00f3n. En producci\u00f3n use el perfil prod (amigep.recuperacion.mostrar-codigo=false).";
            return produccion ? falla(d) : advertencia(d);
        });

        prueba("PS-23", S, "A07 Autenticaci\u00f3n", "Recuperar contrase\u00f1a no revela si el correo existe",
                "Misma respuesta para correos existentes e inexistentes", () -> {
            Resp a = llamar("POST", "/api/auth/recuperar", null, mapa("correo", CLI_CORREO, "rol", "CLIENTE"), "POST /api/auth/recuperar");
            Resp b = llamar("POST", "/api/auth/recuperar", null, mapa("correo", "noexiste" + SUFIJO + "@amigep.pe", "rol", "CLIENTE"), "POST /api/auth/recuperar");
            Object ka = a.json() instanceof Map<?, ?> m ? m.keySet() : null;
            Object kb = b.json() instanceof Map<?, ?> m ? m.keySet() : null;
            if (a.status() == b.status() && ka != null && ka.equals(kb)) return pasa("Ambas respuestas son iguales: " + ka);
            String d = "Las respuestas difieren (" + ka + " frente a " + kb + "): se puede saber qu\u00e9 correos existen.";
            return produccion ? falla(d) : advertencia(d);
        });

        prueba("PS-24", S, "A07 Autenticaci\u00f3n", "Intentos fallidos repetidos de inicio de sesi\u00f3n", "Bloqueo o l\u00edmite tras varios intentos", () -> {
            int bloqueos = 0;
            for (int n = 0; n < 10; n++) {
                Resp r = login(CLI_CORREO, "Incorrecta" + n, "CLIENTE", "POST /api/auth/login");
                if (r.status() == 429 || r.status() == 423) bloqueos++;
            }
            return bloqueos > 0 ? pasa("El servidor limit\u00f3 los intentos")
                    : advertencia("10 intentos fallidos seguidos sin bloqueo ni demora: no hay protecci\u00f3n contra fuerza bruta (ver plan de mantenimiento)");
        });

        prueba("PS-25", S, "A05 Configuraci\u00f3n insegura", "Documentaci\u00f3n Swagger/OpenAPI expuesta",
                produccion ? "404 (deshabilitada)" : "Deshabilitada en producci\u00f3n (en desarrollo disponible)", () -> {
            Resp r = llamar("GET", "/v3/api-docs", null, null, "GET /v3/api-docs");
            if (r.status() == 404 || r.status() == 401 || r.status() == 403) return pasa("HTTP " + r.status() + ": no est\u00e1 publicada");
            String d = "HTTP " + r.status() + ": la documentaci\u00f3n de la API es p\u00fablica. En producci\u00f3n use el perfil prod.";
            return produccion ? falla(d) : advertencia(d);
        });

        prueba("PS-26", S, "A09 Registro y monitoreo", "Estado del servicio (Actuator) p\u00fablico", "200 con status UP", () -> {
            Resp r = llamar("GET", "/actuator/health", null, null, "GET /actuator/health");
            if (r.status() == 404) return advertencia("Actuator no est\u00e1 instalado en esta versi\u00f3n");
            return r.status() == 200 && "UP".equals(texto(r.json(), "status")) ? pasa("HTTP 200, status UP") : falla(r.resumen());
        });

        prueba("PS-27", S, "A01 Control de acceso", "M\u00e9tricas de Actuator sin token", "401", () -> {
            Resp r = llamar("GET", "/actuator/metrics", null, null, "GET /actuator/metrics");
            if (r.status() == 404) return advertencia("Actuator no est\u00e1 instalado en esta versi\u00f3n");
            return estado(r, 401);
        });

        prueba("PS-28", S, "A09 Registro y monitoreo", "M\u00e9tricas de Actuator con token de administrador", "200 con http.server.requests", () -> {
            if (tokenAdmin == null) return omitida("Sin token de administrador");
            Resp r = llamar("GET", "/actuator/metrics/http.server.requests", tokenAdmin, null, "GET /actuator/metrics/{nombre}");
            if (r.status() == 404) return advertencia("Actuator no est\u00e1 instalado en esta versi\u00f3n");
            if (r.status() != 200) return falla(r.resumen());
            String cuenta = null;
            for (Object m : lista(r.json(), "measurements")) if ("COUNT".equals(texto(m, "statistic"))) cuenta = texto(m, "value");
            return pasa("HTTP 200, solicitudes atendidas desde el inicio: " + cuenta);
        });
    }

    // ------------------------------------------------------------------ cierre de las pruebas funcionales

    static void cierre() {
        String F = "Funcional";
        prueba("PF-40", F, "RF-03", "Baja l\u00f3gica del cliente de prueba", "200 con activo = false", () -> {
            if (tokenAdmin == null || idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = llamar("PATCH", "/api/admin/clientes/" + idClienteQa + "/estado", tokenAdmin, mapa("activo", false),
                    "PATCH /api/admin/clientes/{id}/estado");
            return r.status() == 200 && Boolean.FALSE.equals(en(r.json(), "resumen.activo")) ? pasa("HTTP 200, cliente desactivado") : falla(r.resumen());
        });

        prueba("PF-41", F, "RF-01", "Un cliente desactivado no puede iniciar sesi\u00f3n", "401", () -> {
            if (idClienteQa == null) return omitida("Requiere PF-08");
            Resp r = login(CORREO_QA, CLAVE_QA, "CLIENTE", "POST /api/auth/login");
            return r.status() == 401 ? pasa("HTTP 401: " + texto(r.json(), "mensaje")) : falla(r.resumen());
        });
    }

    // ------------------------------------------------------------------ reporte

    record Estadistica(String etiqueta, int n, double promedio, long p95, long maximo, int errores) {
    }

    static List<Estadistica> estadisticas() {
        Map<String, List<Medicion>> grupos = new LinkedHashMap<>();
        for (Medicion m : mediciones) grupos.computeIfAbsent(m.etiqueta(), x -> new ArrayList<>()).add(m);
        List<Estadistica> l = new ArrayList<>();
        for (var e : grupos.entrySet()) l.add(estadistica(e.getKey(), e.getValue()));
        l.sort((a, b) -> Long.compare(b.p95(), a.p95()));
        return l;
    }

    static Estadistica estadistica(String etiqueta, List<Medicion> ms) {
        List<Long> t = new ArrayList<>();
        int errores = 0;
        for (Medicion m : ms) {
            t.add(m.ms());
            if (m.status() >= 500) errores++;
        }
        t.sort(Long::compare);
        double prom = t.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = t.get(Math.max(0, (int) Math.ceil(t.size() * 0.95) - 1));
        return new Estadistica(etiqueta, t.size(), prom, p95, t.get(t.size() - 1), errores);
    }

    static String h(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static Path escribirReporte(String inicio, String fin) throws IOException {
        Map<Estado, Integer> conteo = new LinkedHashMap<>();
        for (Estado e : Estado.values()) conteo.put(e, 0);
        for (Resultado r : resultados) conteo.merge(r.estado(), 1, Integer::sum);
        Estadistica total = mediciones.isEmpty() ? null : estadistica("Todas", mediciones);

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><title>AMIGEP \u00b7 Reporte de pruebas</title><style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;margin:32px;color:#1f2328;background:#fff}h1{margin:0 0 4px}h2{margin-top:32px;border-bottom:2px solid #1c0770;padding-bottom:4px;color:#1c0770}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px;margin-top:8px}th{background:#1c0770;color:#fff;text-align:left;padding:6px 8px}td{border-bottom:1px solid #e1e3ea;padding:6px 8px;vertical-align:top}")
                .append(".PASA{color:#0a7a3e;font-weight:600}.FALLA{color:#b42318;font-weight:600}.ADVERTENCIA{color:#a15c00;font-weight:600}.OMITIDA{color:#6b7280;font-weight:600}")
                .append(".tarjetas{display:flex;gap:12px;margin:16px 0}.t{border:1px solid #e1e3ea;border-radius:8px;padding:10px 16px;min-width:120px}.t b{font-size:24px;display:block}.meta{color:#5a5f7d}</style></head><body>");
        sb.append("<h1>AMIGEP \u00b7 Reporte de pruebas funcionales y de seguridad</h1>");
        sb.append("<div class=\"meta\">Servidor: ").append(h(base)).append(" \u00b7 Modo: ").append(produccion ? "producci\u00f3n" : "entorno de pruebas")
                .append(" \u00b7 Inicio: ").append(h(inicio)).append(" \u00b7 Fin: ").append(h(fin)).append(" \u00b7 Java ").append(h(System.getProperty("java.version"))).append("</div>");
        sb.append("<div class=\"tarjetas\">");
        sb.append("<div class=\"t\"><b>").append(resultados.size()).append("</b>pruebas</div>");
        for (var e : conteo.entrySet()) sb.append("<div class=\"t\"><b class=\"").append(e.getKey()).append("\">").append(e.getValue()).append("</b>").append(e.getKey().name().toLowerCase(Locale.ROOT)).append("</div>");
        sb.append("</div>");
        for (String tipo : List.of("Funcional", "Seguridad")) {
            sb.append("<h2>Pruebas ").append(tipo.equals("Funcional") ? "funcionales" : "de seguridad").append("</h2><table><tr><th>ID</th><th>")
                    .append(tipo.equals("Funcional") ? "Requerimiento" : "OWASP Top 10").append("</th><th>Caso</th><th>Resultado esperado</th><th>Resultado obtenido</th><th>Estado</th><th>ms</th></tr>");
            for (Resultado r : resultados) {
                if (!r.tipo().equals(tipo)) continue;
                sb.append("<tr><td>").append(h(r.id())).append("</td><td>").append(h(r.referencia())).append("</td><td>").append(h(r.descripcion()))
                        .append("</td><td>").append(h(r.esperado())).append("</td><td>").append(h(r.obtenido())).append("</td><td class=\"").append(r.estado())
                        .append("\">").append(r.estado()).append("</td><td>").append(r.ms()).append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("<h2>M\u00e9tricas de tiempo de respuesta (RNF-06)</h2>");
        if (total != null) {
            sb.append("<p>").append(total.n()).append(" solicitudes \u00b7 promedio ").append(String.format(Locale.US, "%.0f", total.promedio()))
                    .append(" ms \u00b7 percentil 95: ").append(total.p95()).append(" ms \u00b7 m\u00e1ximo ").append(total.maximo()).append(" ms \u00b7 errores 5xx: ").append(total.errores())
                    .append(" \u00b7 umbral por solicitud: ").append(UMBRAL_MS).append(" ms</p>");
        }
        sb.append("<table><tr><th>Endpoint</th><th>Solicitudes</th><th>Promedio (ms)</th><th>p95 (ms)</th><th>M\u00e1ximo (ms)</th><th>Errores 5xx</th><th>Dentro del umbral</th></tr>");
        for (Estadistica e : estadisticas()) {
            sb.append("<tr><td>").append(h(e.etiqueta())).append("</td><td>").append(e.n()).append("</td><td>").append(String.format(Locale.US, "%.0f", e.promedio()))
                    .append("</td><td>").append(e.p95()).append("</td><td>").append(e.maximo()).append("</td><td>").append(e.errores()).append("</td><td class=\"")
                    .append(e.p95() <= UMBRAL_MS ? "PASA\">S\u00ed" : "FALLA\">No").append("</td></tr>");
        }
        sb.append("</table><p class=\"meta\">Generado por pruebas/PruebasAmigep.java. Las pruebas crean un cliente \u00abPrueba QA\u00bb (DNI ")
                .append(DNI_QA).append(") que queda desactivado al final.</p></body></html>");

        Path dir = Path.of("pruebas", "reportes");
        Files.createDirectories(dir);
        Path archivo = dir.resolve("reporte-pruebas-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".html");
        Files.writeString(archivo, sb.toString(), StandardCharsets.UTF_8);
        return archivo;
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws IOException {
        for (String a : args) {
            if (a.equals("--produccion")) produccion = true;
            else if (a.startsWith("http")) base = a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
        }
        String inicio = LocalDateTime.now().withNano(0).toString().replace('T', ' ');
        System.out.println("AMIGEP \u00b7 Pruebas funcionales y de seguridad");
        System.out.println("Servidor: " + base + (produccion ? " (modo producci\u00f3n)" : " (entorno de pruebas)"));
        // Espera a que el backend termine la carga inicial (readiness de Actuator), hasta 3 minutos
        for (int intento = 0; intento < 90; intento++) {
            Resp listo = llamar("GET", "/actuator/health/readiness", null, null, "GET /actuator/health/readiness");
            if (listo.status() != 0 && listo.status() != 503) break;
            if (intento == 0) System.out.println("Esperando a que el backend termine de iniciar...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        mediciones.clear();
        Resp ping = llamar("GET", "/api/planes", null, null, "GET /api/planes");
        if (ping.status() == 0) {
            System.out.println("No se pudo conectar con " + base + ". Inicie el backend (./mvnw spring-boot:run) e intente de nuevo.");
            System.exit(2);
        }
        System.out.println();
        funcionales();
        seguridad();
        cierre();
        String fin = LocalDateTime.now().withNano(0).toString().replace('T', ' ');

        Map<Estado, Integer> conteo = new LinkedHashMap<>();
        for (Estado e : Estado.values()) conteo.put(e, 0);
        for (Resultado r : resultados) conteo.merge(r.estado(), 1, Integer::sum);
        System.out.println();
        System.out.println("Resumen: " + resultados.size() + " pruebas \u00b7 " + conteo);
        if (!mediciones.isEmpty()) {
            Estadistica total = estadistica("Todas", mediciones);
            System.out.printf(Locale.US, "M\u00e9tricas: %d solicitudes \u00b7 promedio %.0f ms \u00b7 p95 %d ms \u00b7 m\u00e1ximo %d ms%n",
                    total.n(), total.promedio(), total.p95(), total.maximo());
        }
        Path reporte = escribirReporte(inicio, fin);
        System.out.println("Reporte: " + reporte.toAbsolutePath());
        if (EN_GITHUB) {
            System.out.println("::notice title=Pruebas funcionales y de seguridad::" + anotacion(resultados.size() + " pruebas \u00b7 " + conteo));
            String resumen = System.getenv("GITHUB_STEP_SUMMARY");
            if (resumen != null) {
                StringBuilder md = new StringBuilder("## Pruebas funcionales y de seguridad\n\n")
                        .append(resultados.size()).append(" pruebas \u00b7 ").append(conteo).append("\n\n| ID | Referencia | Caso | Estado | Obtenido |\n|---|---|---|---|---|\n");
                for (Resultado r : resultados) {
                    md.append("| ").append(r.id()).append(" | ").append(r.referencia()).append(" | ").append(r.descripcion()).append(" | ")
                            .append(r.estado()).append(" | ").append(r.obtenido().replace("|", "/")).append(" |\n");
                }
                Files.writeString(Path.of(resumen), md.toString(), StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            }
        }
        System.exit(conteo.get(Estado.FALLA) > 0 ? 1 : 0);
    }
}
