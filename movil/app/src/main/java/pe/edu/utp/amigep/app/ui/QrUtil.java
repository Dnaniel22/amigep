package pe.edu.utp.amigep.app.ui;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Genera el código QR personal del cliente (RF-17) que se escanea en recepción. */
public final class QrUtil {

    private QrUtil() {
    }

    public static Bitmap generar(String contenido, int tamano, int colorModulo, int colorFondo) {
        Map<EncodeHintType, Object> opciones = new EnumMap<>(EncodeHintType.class);
        opciones.put(EncodeHintType.MARGIN, 1);
        opciones.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano, opciones);
            int ancho = matriz.getWidth();
            int alto = matriz.getHeight();
            int[] pixeles = new int[ancho * alto];
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    pixeles[y * ancho + x] = matriz.get(x, y) ? colorModulo : colorFondo;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }
}
