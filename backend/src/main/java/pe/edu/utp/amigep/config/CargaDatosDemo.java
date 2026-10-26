package pe.edu.utp.amigep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.modelo.enums.*;
import pe.edu.utp.amigep.repositorio.*;
import pe.edu.utp.amigep.servicio.ia.GeneradorPlanAlimenticio;
import pe.edu.utp.amigep.servicio.ia.GeneradorRutinas;
import pe.edu.utp.amigep.servicio.ia.PerfilFisico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Datos de prueba del piloto (Angels GYM) con ~6 meses de historial, relativos a la fecha actual:
 * clientes regulares, clientes que dejan de asistir, membresías por vencer, rutinas, comidas y progreso.
 * Se ejecuta una sola vez, cuando la tabla Cliente está vacía y amigep.datos-demo=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CargaDatosDemo {

    public static final String CLAVE_CLIENTE = "Cliente123*";
    public static final String CLAVE_ENTRENADOR = "Entrenador123*";

    private final TrabajadorRepositorio trabajadorRepo;
    private final ClienteRepositorio clienteRepo;
    private final PlanRepositorio planRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final AccesoRepositorio accesoRepo;
    private final RutinaRepositorio rutinaRepo;
    private final SesionEntrenamientoRepositorio sesionRepo;
    private final PlanAlimenticioRepositorio planAlimRepo;
    private final RegistroComidaRepositorio comidaRepo;
    private final ProgresoRepositorio progresoRepo;
    private final NotificacionRepositorio notificacionRepo;
    private final PasswordEncoder encoder;
    private final GeneradorRutinas generadorRutinas;
    private final GeneradorPlanAlimenticio generadorPlan;
    private final Clock reloj;

    private final Random rnd = new Random(2026);

    /** Comportamiento simulado del cliente. */
    enum Perfil { REGULAR, POR_VENCER, DECAE, ABANDONO, NUEVO, EXCLIENTE }

    /**
     * @param diasRegistro   hace cuántos días se registró
     * @param finCobertura   días desde hoy hasta el fin de su membresía (negativo = vencida hace N días)
     * @param ultimaVisita   hace cuántos días asistió por última vez (para DECAE / ABANDONO)
     * @param semanaRutina   semana actual de su rutina activa (0 = sin rutina)
     */
    record Semilla(String nombre, String apellido, String plan, Perfil perfil, Objetivo objetivo, Nivel nivel,
                   double altura, double peso, int edad, int entrenador, int diasRegistro, int finCobertura,
                   int ultimaVisita, int semanaRutina, int semanasRutina, int sesionesSemana, boolean rutinaIa) {
    }

    @Transactional
    public void cargar() {
        LocalDate hoy = LocalDate.now(reloj);
        log.info("Cargando datos de demostración (Angels GYM)...");

        List<Trabajador> entrenadores = List.of(
                trabajador("Carlos", "Bustamante Ríos", "40112233", "carlos.bustamante@angelsgym.pe", "Hipertrofia y fuerza"),
                trabajador("Andrea", "Quispe Lazo", "41223344", "andrea.quispe@angelsgym.pe", "Funcional y pérdida de grasa"),
                trabajador("Miguel", "Torres Vidal", "42334455", "miguel.torres@angelsgym.pe", "Resistencia y acondicionamiento"));

        Map<String, Plan> planes = new HashMap<>();
        planRepo.findAll().forEach(p -> planes.put(p.getNombre(), p));

        List<Semilla> semillas = new ArrayList<>(List.of(
                // Clientes que aparecen en los prototipos (C2-C6, E1-E2, A1-A3)
                new Semilla("Luis Fernando", "Atocsa Tapia", "VIP", Perfil.REGULAR, Objetivo.MASA_MUSCULAR, Nivel.INTERMEDIO, 1.75, 90, 22, 0, 170, 48, 0, 3, 8, 4, false),
                new Semilla("Percy", "Gonzales Vera", "VIP", Perfil.POR_VENCER, Objetivo.PERDIDA_GRASA, Nivel.INTERMEDIO, 1.70, 84, 24, 0, 150, 2, 0, 7, 8, 4, false),
                new Semilla("Daniel", "Franco Herrera", "Estándar", Perfil.REGULAR, Objetivo.FUERZA, Nivel.AVANZADO, 1.78, 82, 23, 0, 140, 21, 0, 1, 6, 4, false),
                new Semilla("Julio", "Kong San Inciso", "VIP", Perfil.DECAE, Objetivo.MASA_MUSCULAR, Nivel.PRINCIPIANTE, 1.72, 70, 22, 0, 120, -6, 5, 0, 0, 3, false),
                new Semilla("César", "Huatangari De La Cruz", "Básico", Perfil.POR_VENCER, Objetivo.RESISTENCIA, Nivel.INTERMEDIO, 1.68, 66, 25, 0, 110, 5, 0, 0, 0, 3, false),
                new Semilla("Fabricio", "Hernández Alvarado", "Estándar", Perfil.ABANDONO, Objetivo.FUERZA, Nivel.INTERMEDIO, 1.80, 78, 23, 0, 160, -3, 12, 6, 6, 4, false),
                new Semilla("María", "Ríos Paredes", "Estándar", Perfil.POR_VENCER, Objetivo.PERDIDA_GRASA, Nivel.PRINCIPIANTE, 1.60, 68, 27, 0, 90, 4, 0, 2, 6, 3, false),
                new Semilla("Jorge", "Salas Medina", "VIP", Perfil.POR_VENCER, Objetivo.MASA_MUSCULAR, Nivel.AVANZADO, 1.82, 88, 30, 1, 175, 7, 0, 5, 8, 5, true),
                new Semilla("Valeria", "Cárdenas Soto", "VIP", Perfil.REGULAR, Objetivo.PERDIDA_GRASA, Nivel.INTERMEDIO, 1.63, 64, 28, 1, 130, 19, 0, 4, 8, 4, true),
                new Semilla("Renzo", "Paredes Luján", "Estándar", Perfil.REGULAR, Objetivo.MASA_MUSCULAR, Nivel.PRINCIPIANTE, 1.74, 72, 20, 0, 60, 26, 0, 2, 6, 3, false),
                new Semilla("Camila", "Torres Benavides", "Básico", Perfil.REGULAR, Objetivo.RESISTENCIA, Nivel.PRINCIPIANTE, 1.58, 55, 21, 2, 45, 15, 0, 3, 6, 3, true),
                new Semilla("Diego", "Ramírez Castro", "VIP", Perfil.REGULAR, Objetivo.FUERZA, Nivel.AVANZADO, 1.85, 95, 33, 0, 178, 11, 0, 5, 8, 5, false),
                new Semilla("Lucía", "Mendoza Flores", "Estándar", Perfil.DECAE, Objetivo.PERDIDA_GRASA, Nivel.PRINCIPIANTE, 1.62, 74, 35, 1, 100, 9, 6, 0, 0, 3, false),
                new Semilla("Sebastián", "Rojas Aguilar", "Básico", Perfil.ABANDONO, Objetivo.MASA_MUSCULAR, Nivel.PRINCIPIANTE, 1.70, 65, 19, 2, 95, -20, 24, 0, 0, 3, false),
                new Semilla("Ana Lucía", "Vargas Pinto", "Estándar", Perfil.NUEVO, Objetivo.PERDIDA_GRASA, Nivel.PRINCIPIANTE, 1.65, 70, 26, 1, 12, 18, 0, 0, 0, 3, false)));

        String[][] nombres = {
                {"Rosa", "Chávez Huamán"}, {"Kevin", "Salazar Ortiz"}, {"Gabriela", "Núñez Ramos"}, {"Jhon", "Espinoza Tello"},
                {"Milagros", "Gutiérrez Vega"}, {"Bruno", "Castillo Arana"}, {"Fiorella", "León Matos"}, {"Óscar", "Palacios Ruiz"},
                {"Daniela", "Campos Yupanqui"}, {"Ricardo", "Medina Zapata"}, {"Paola", "Herrera Quispe"}, {"Álvaro", "Silva Montes"},
                {"Ximena", "Ortega Cruz"}, {"Martín", "Delgado Rivas"}, {"Claudia", "Fernández Lara"}, {"Joel", "Guzmán Tapia"},
                {"Sofía", "Reyes Carrasco"}, {"André", "Mejía Bravo"}, {"Karen", "Villanueva Ponce"}, {"Luis Ángel", "Cabrera Sosa"},
                {"Natalia", "Aguirre Díaz"}, {"Piero", "Arias Calderón"}, {"Mariana", "Zevallos Peña"}, {"Hugo", "Linares Ochoa"},
                {"Elena", "Morales Cueva"}, {"Franco", "Salinas Nieto"}, {"Alessandra", "Ibarra Loayza"}, {"Gustavo", "Pacheco Ríos"}};
        Objetivo[] objetivos = Objetivo.values();
        Nivel[] niveles = Nivel.values();
        String[] nombresPlan = {"Básico", "Estándar", "Estándar", "VIP", "VIP"};
        for (int i = 0; i < nombres.length; i++) {
            double r = rnd.nextDouble();
            Perfil perfil = r < 0.58 ? Perfil.REGULAR : r < 0.70 ? Perfil.POR_VENCER : r < 0.80 ? Perfil.DECAE
                    : r < 0.90 ? Perfil.ABANDONO : Perfil.NUEVO;
            boolean mujer = i % 2 == 0;
            double altura = mujer ? 1.55 + rnd.nextDouble() * 0.15 : 1.66 + rnd.nextDouble() * 0.18;
            double peso = (20 + rnd.nextDouble() * 8) * altura * altura;
            int registro = perfil == Perfil.NUEVO ? 5 + rnd.nextInt(20) : 50 + rnd.nextInt(130);
            int fin = switch (perfil) {
                case REGULAR -> 8 + rnd.nextInt(22);
                case POR_VENCER -> 1 + rnd.nextInt(7);
                case DECAE -> -5 + rnd.nextInt(15);
                case ABANDONO, EXCLIENTE -> -(5 + rnd.nextInt(40));
                case NUEVO -> 30 - registro;
            };
            int ultima = perfil == Perfil.ABANDONO ? 10 + rnd.nextInt(30) : perfil == Perfil.DECAE ? 3 + rnd.nextInt(6) : 0;
            boolean conRutina = perfil != Perfil.ABANDONO && rnd.nextDouble() < 0.8;
            int semanas = rnd.nextBoolean() ? 8 : 6;
            semillas.add(new Semilla(nombres[i][0], nombres[i][1], nombresPlan[rnd.nextInt(nombresPlan.length)], perfil,
                    objetivos[rnd.nextInt(objetivos.length)], niveles[rnd.nextInt(niveles.length)],
                    Math.round(altura * 100) / 100.0, Math.round(peso), 18 + rnd.nextInt(30), i < 5 ? 0 : 1 + rnd.nextInt(2),
                    registro, fin, ultima, conRutina ? 1 + rnd.nextInt(semanas) : 0, semanas, 3 + rnd.nextInt(3), rnd.nextBoolean()));
        }

        // Ex clientes: abandonaron hace 2-4 meses (baja lógica). Aportan casos de abandono al entrenamiento del modelo.
        String[][] exClientes = {
                {"Patricia", "Saavedra Gómez"}, {"Raúl", "Cornejo Luna"}, {"Verónica", "Bustos Alarcón"}, {"Iván", "Quiroz Peralta"},
                {"Lorena", "Valdivia Ccori"}, {"Mauricio", "Benites Solís"}, {"Carla", "Rengifo Pardo"}, {"Edwin", "Mamani Choque"},
                {"Tatiana", "Olivares Cano"}, {"Rodrigo", "Neyra Falcón"}, {"Silvia", "Prado Huerta"}, {"Javier", "Lozano Vilca"}};
        for (String[] n : exClientes) {
            int registro = 150 + rnd.nextInt(60);
            int ultima = 50 + rnd.nextInt(60);
            semillas.add(new Semilla(n[0], n[1], nombresPlan[rnd.nextInt(nombresPlan.length)], Perfil.EXCLIENTE,
                    objetivos[rnd.nextInt(objetivos.length)], niveles[rnd.nextInt(niveles.length)], 1.55 + rnd.nextDouble() * 0.3,
                    60 + rnd.nextInt(30), 20 + rnd.nextInt(25), rnd.nextInt(3), registro, -(ultima - rnd.nextInt(15)), ultima,
                    0, 0, 3, rnd.nextBoolean()));
        }

        int creados = 0;
        for (Semilla s : semillas) {
            crearCliente(s, entrenadores, planes, hoy);
            creados++;
        }
        log.info("Datos de demostración cargados: {} clientes ({} dados de baja), {} entrenadores", creados, exClientes.length, entrenadores.size());
    }

    private Trabajador trabajador(String nombre, String apellido, String dni, String email, String especialidad) {
        Trabajador t = new Trabajador();
        t.setNombre(nombre);
        t.setApellido(apellido);
        t.setDni(dni);
        t.setEmail(email);
        t.setContrasenaHash(encoder.encode(CLAVE_ENTRENADOR));
        t.setRol(Rol.ENTRENADOR);
        t.setEspecialidad(especialidad);
        t.setActivo(true);
        return trabajadorRepo.save(t);
    }

    private void crearCliente(Semilla s, List<Trabajador> entrenadores, Map<String, Plan> planes, LocalDate hoy) {
        LocalDate registro = hoy.minusDays(s.diasRegistro());
        Cliente c = new Cliente();
        c.setNombre(s.nombre());
        c.setApellido(s.apellido());
        c.setDni(String.valueOf(70000000 + rnd.nextInt(9999999)));
        c.setEmail(correo(s.nombre(), s.apellido()));
        c.setContrasenaHash(encoder.encode(CLAVE_CLIENTE));
        c.setTelefono("9" + (10000000 + rnd.nextInt(89999999)));
        c.setCodigoQR("AMIGEP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        c.setObjetivo(s.objetivo().getEtiqueta());
        c.setNivel(s.nivel());
        c.setAltura(BigDecimal.valueOf(s.altura()).setScale(2, RoundingMode.HALF_UP));
        c.setFechaNacimiento(hoy.minusYears(s.edad()).minusDays(rnd.nextInt(300)));
        c.setFechaRegistro(registro);
        c.setEntrenador(entrenadores.get(s.entrenador()));
        c.setActivo(true);
        c = clienteRepo.save(c);

        // Membresías mensuales encadenadas desde el registro hasta el fin de cobertura
        Plan plan = planes.get(s.plan());
        LocalDate finCobertura = hoy.plusDays(s.finCobertura());
        LocalDate inicio = registro;
        while (inicio.isBefore(finCobertura)) {
            LocalDate fin = inicio.plusDays(plan.getDuracionDias());
            if (fin.isAfter(finCobertura)) {
                fin = finCobertura;
                if (!fin.isAfter(inicio)) break;
            }
            Suscripcion sus = new Suscripcion();
            sus.setCliente(c);
            sus.setPlan(plan);
            sus.setFechaInicio(inicio);
            sus.setFechaCaducidad(fin);
            sus.setActivo(true);
            sus = suscripcionRepo.save(sus);
            if (rnd.nextDouble() < 0.04) pago(sus, inicio, EstadoPago.RECHAZADO);
            pago(sus, inicio, EstadoPago.VALIDADO);
            inicio = fin.plusDays(1);
        }

        // Asistencias (accesos) según el perfil
        LocalDate ultimaVisita = s.ultimaVisita() > 0 ? hoy.minusDays(s.ultimaVisita()) : hoy;
        LocalDate inicioDeclive = s.perfil() == Perfil.DECAE ? hoy.minusDays(30) : null;
        double frecuencia = Math.min(0.9, s.sesionesSemana() / 6.5 + 0.08);
        List<Acceso> accesos = new ArrayList<>();
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = registro; !d.isAfter(ultimaVisita); d = d.plusDays(1)) {
            if (d.isAfter(finCobertura) && s.perfil() != Perfil.REGULAR) break;
            double p = d.getDayOfWeek() == DayOfWeek.SUNDAY ? 0.1 : frecuencia;
            if (inicioDeclive != null && d.isAfter(inicioDeclive)) p *= 0.35;
            boolean esHoy = d.equals(hoy);
            if (esHoy && (s.apellido().startsWith("Atocsa") || rnd.nextDouble() < 0.5)) p = 1;
            if (rnd.nextDouble() >= p && !(d.equals(ultimaVisita) && s.ultimaVisita() > 0)) continue;
            LocalTime hora = LocalTime.of(6 + rnd.nextInt(15), rnd.nextInt(60));
            LocalDateTime fh = d.atTime(hora);
            if (esHoy && fh.isAfter(LocalDateTime.now(reloj))) fh = LocalDateTime.now(reloj).minusMinutes(20 + rnd.nextInt(120));
            if (fh.toLocalDate().isBefore(d)) continue;
            Acceso a = new Acceso();
            a.setCliente(c);
            a.setFechaHora(fh);
            a.setTipo(rnd.nextDouble() < 0.94 ? TipoAcceso.QR : TipoAcceso.MANUAL);
            accesos.add(a);
            dias.add(d);
        }
        accesoRepo.saveAll(accesos);

        // Progreso: medición mensual con tendencia según el objetivo
        double tendencia = switch (s.objetivo()) {
            case MASA_MUSCULAR -> 0.9;
            case PERDIDA_GRASA -> -1.4;
            case FUERZA -> 0.5;
            case RESISTENCIA -> -0.4;
        };
        int meses = Math.max(1, s.diasRegistro() / 30);
        double pesoInicial = s.peso() - tendencia * (meses - 1);
        for (int m = 0; m < meses; m++) {
            LocalDate fecha = registro.plusDays(m * 30L + 1);
            if (fecha.isAfter(hoy)) break;
            if ((s.perfil() == Perfil.ABANDONO || s.perfil() == Perfil.EXCLIENTE) && fecha.isAfter(ultimaVisita)) break;
            Progreso pr = new Progreso();
            pr.setCliente(c);
            pr.setFecha(fecha);
            double peso = m == meses - 1 ? s.peso() : pesoInicial + tendencia * m + (rnd.nextDouble() - 0.5);
            pr.setPeso(BigDecimal.valueOf(peso).setScale(1, RoundingMode.HALF_UP));
            pr.setGrasaCorporal(BigDecimal.valueOf(14 + rnd.nextDouble() * 12 - (s.objetivo() == Objetivo.PERDIDA_GRASA ? m * 0.6 : 0))
                    .setScale(1, RoundingMode.HALF_UP));
            pr.setMedidas(Map.of("cintura", Math.round(70 + peso * 0.15), "pecho", Math.round(85 + peso * 0.2),
                    "brazo", Math.round(26 + peso * 0.06)));
            progresoRepo.save(pr);
        }

        // Rutinas: una anterior finalizada y la activa
        PerfilFisico perfil = new PerfilFisico(BigDecimal.valueOf(s.peso()), c.getAltura(), s.edad());
        Trabajador entrenador = entrenadores.get(s.entrenador());
        if (s.diasRegistro() > 70) {
            LocalDate ini = registro.plusDays(2);
            Rutina anterior = rutina(c, s.rutinaIa() ? null : entrenador, s.objetivo(), s.nivel(), s.sesionesSemana(), perfil,
                    ini, 6, false, s.rutinaIa());
            anterior.setFechaFinalizacion(ini.plusWeeks(6));
            rutinaRepo.save(anterior);
            sesiones(anterior, dias, ini, ini.plusWeeks(6));
        }
        if (s.semanaRutina() > 0) {
            LocalDate ini = hoy.minusWeeks(s.semanaRutina() - 1).minusDays(rnd.nextInt(5));
            if (ini.isBefore(registro)) ini = registro;
            Rutina activa = rutina(c, s.rutinaIa() ? null : entrenador, s.objetivo(), s.nivel(), s.sesionesSemana(), perfil,
                    ini, s.semanasRutina(), true, s.rutinaIa());
            sesiones(activa, dias, ini, hoy);
        }

        // Plan alimenticio y registro de comidas (planes VIP y parte de Estándar)
        boolean conPlan = s.plan().equals("VIP") || (s.plan().equals("Estándar") && rnd.nextBoolean());
        if (conPlan && s.perfil() != Perfil.ABANDONO && s.perfil() != Perfil.EXCLIENTE) {
            var resultado = generadorPlan.generar(s.objetivo(), perfil, s.sesionesSemana());
            PlanAlimenticio pa = new PlanAlimenticio();
            pa.setCliente(c);
            pa.setObjetivo(s.objetivo().getEtiqueta());
            pa.setRitmo(resultado.ritmo());
            pa.setCaloriasObjetivo(resultado.caloriasObjetivo());
            pa.setContenido(resultado.contenido());
            pa.setFechaCreacion(hoy.minusDays(Math.min(40, s.diasRegistro())));
            pa.setActivo(true);
            planAlimRepo.save(pa);
            comidas(c, resultado, hoy);
        }

        if (s.apellido().startsWith("Atocsa")) notificacionesDeEjemplo(c, hoy);
        if (s.perfil() == Perfil.EXCLIENTE) c.setActivo(false);
    }

    private void pago(Suscripcion sus, LocalDate fecha, EstadoPago estado) {
        double r = rnd.nextDouble();
        Pago p = new Pago();
        p.setSuscripcion(sus);
        p.setMonto(sus.getPlan().getPrecio());
        p.setFechaPago(fecha.isAfter(LocalDate.now(reloj)) ? LocalDate.now(reloj) : fecha);
        p.setMetodoPago(r < 0.5 ? MetodoPago.YAPE_PLIN : r < 0.82 ? MetodoPago.TARJETA : MetodoPago.EFECTIVO);
        p.setEstado(estado);
        pagoRepo.save(p);
    }

    private Rutina rutina(Cliente c, Trabajador entrenador, Objetivo objetivo, Nivel nivel, int sesiones, PerfilFisico perfil,
                          LocalDate inicio, int semanas, boolean activa, boolean ia) {
        Map<String, Object> contenido = new LinkedHashMap<>(generadorRutinas.generar(objetivo, nivel, sesiones, perfil));
        contenido.put("semanas", semanas);
        if (!ia) contenido.remove("generadoPor");
        Rutina r = new Rutina();
        r.setCliente(c);
        r.setTrabajador(ia ? null : entrenador);
        r.setObjetivo(objetivo.getEtiqueta());
        r.setNivel(nivel);
        r.setRitmo(sesiones + " sesiones/semana");
        r.setContenido(contenido);
        r.setOrigen(ia ? OrigenRutina.IA : OrigenRutina.ENTRENADOR);
        r.setFechaCreacion(inicio);
        r.setFechaFinalizacion(inicio.plusWeeks(semanas));
        r.setActivo(activa);
        return rutinaRepo.save(r);
    }

    private void sesiones(Rutina r, List<LocalDate> diasAsistidos, LocalDate desde, LocalDate hasta) {
        List<SesionEntrenamiento> lista = new ArrayList<>();
        int n = 0;
        int totalDias = ((List<?>) r.getContenido().get("dias")).size();
        for (LocalDate d : new TreeSet<>(diasAsistidos)) {
            if (d.isBefore(desde) || d.isAfter(hasta)) continue;
            SesionEntrenamiento s = new SesionEntrenamiento();
            s.setRutina(r);
            s.setFecha(d);
            s.setCompletada(rnd.nextDouble() < 0.9);
            s.setObservaciones("Día " + (n % totalDias + 1));
            if (s.isCompletada()) n++;
            lista.add(s);
        }
        sesionRepo.saveAll(lista);
    }

    @SuppressWarnings("unchecked")
    private void comidas(Cliente c, GeneradorPlanAlimenticio.ResultadoPlan plan, LocalDate hoy) {
        List<Map<String, Object>> sugeridas = (List<Map<String, Object>>) plan.contenido().get("comidas");
        List<RegistroComida> lista = new ArrayList<>();
        for (int d = 8; d >= 0; d--) {
            LocalDate fecha = hoy.minusDays(d);
            for (Map<String, Object> s : sugeridas) {
                TipoComida tipo = TipoComida.valueOf((String) s.get("tipo"));
                if (d == 0 && tipo != TipoComida.DESAYUNO) continue; // hoy solo se registró el desayuno
                if (rnd.nextDouble() < 0.15) continue;
                double factor = 0.8 + rnd.nextDouble() * 0.45;
                RegistroComida r = new RegistroComida();
                r.setCliente(c);
                r.setFecha(fecha);
                r.setTipoComida(tipo);
                r.setDescripcion((String) s.get("descripcion"));
                r.setCalorias((int) Math.round(((Number) s.get("calorias")).doubleValue() * factor));
                r.setProteinas(BigDecimal.valueOf(((Number) s.get("proteinas")).doubleValue() * factor * (d == 0 ? 0.7 : 1)).setScale(1, RoundingMode.HALF_UP));
                r.setCarbohidratos(BigDecimal.valueOf(((Number) s.get("carbohidratos")).doubleValue() * factor).setScale(1, RoundingMode.HALF_UP));
                r.setGrasas(BigDecimal.valueOf(((Number) s.get("grasas")).doubleValue() * factor).setScale(1, RoundingMode.HALF_UP));
                lista.add(r);
            }
        }
        comidaRepo.saveAll(lista);
    }

    private void notificacionesDeEjemplo(Cliente c, LocalDate hoy) {
        Object[][] datos = {
                {TipoNotificacion.RUTINA, "Nueva rutina asignada", "Carlos Bustamante Ríos te asignó la rutina \"Hipertrofia\" (4 sesiones/semana, 8 semanas).", 15, true},
                {TipoNotificacion.VENCIMIENTO, "Membresía renovada", "Tu plan VIP está vigente. ¡Gracias por seguir entrenando con nosotros!", 10, true},
                {TipoNotificacion.RUTINA, "¡Vas muy bien!", "Completaste 3 semanas seguidas de tu rutina. Sigue así.", 1, false}};
        for (Object[] d : datos) {
            Notificacion n = new Notificacion();
            n.setCliente(c);
            n.setTipo((TipoNotificacion) d[0]);
            n.setTitulo((String) d[1]);
            n.setMensaje((String) d[2]);
            n.setFechaEnvio(hoy.minusDays((Integer) d[3]).atTime(9, 0));
            n.setLeido((Boolean) d[4]);
            notificacionRepo.save(n);
        }
    }

    private static String correo(String nombre, String apellido) {
        String n = sinTildes(nombre.split(" ")[0]);
        String a = sinTildes(apellido.split(" ")[0]);
        return (n + "." + a + "@amigep.pe").toLowerCase();
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
