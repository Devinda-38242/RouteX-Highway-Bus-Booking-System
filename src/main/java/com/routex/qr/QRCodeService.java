package com.routex.qr;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

@Service
public class QRCodeService {

    /**
     * Generates a QR code PNG as a Base64 string.
     * Content encodes the booking reference and key details for scanning at the bus bay.
     */
    public String generateQRCodeBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    /**
     * Builds the QR content string for a booking.
     */
    public String buildQRContent(String bookingRef, String passengerName,
                                  String route, String date, String seats) {
        return String.format(
            "ROUTEX-TICKET\nRef:%s\nName:%s\nRoute:%s\nDate:%s\nSeats:%s",
            bookingRef, passengerName, route, date, seats
        );
    }
}
