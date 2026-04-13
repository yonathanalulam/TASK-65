package com.culinarycoach.security.captcha;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;

public class CaptchaGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int WIDTH = 200;
    private static final int HEIGHT = 70;
    private static final int LENGTH = 6;
    private static final Random RANDOM = new Random();

    public record CaptchaData(String answer, String imageBase64) {}

    public static CaptchaData generate() {
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            answer.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Noise lines
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(
                160 + RANDOM.nextInt(80),
                160 + RANDOM.nextInt(80),
                160 + RANDOM.nextInt(80)));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                       RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // Draw characters
        String text = answer.toString();
        Font font = new Font("SansSerif", Font.BOLD, 32);
        g.setFont(font);

        for (int i = 0; i < text.length(); i++) {
            AffineTransform origTransform = g.getTransform();
            int x = 15 + i * 28;
            int y = 40 + RANDOM.nextInt(15);
            double rotation = (RANDOM.nextDouble() - 0.5) * 0.5;

            g.translate(x, y);
            g.rotate(rotation);
            g.setColor(new Color(
                RANDOM.nextInt(100),
                RANDOM.nextInt(100),
                RANDOM.nextInt(100)));
            g.drawString(String.valueOf(text.charAt(i)), 0, 0);
            g.setTransform(origTransform);
        }

        // Noise dots
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(
                RANDOM.nextInt(200),
                RANDOM.nextInt(200),
                RANDOM.nextInt(200)));
            g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64 = "data:image/png;base64," +
                Base64.getEncoder().encodeToString(baos.toByteArray());
            return new CaptchaData(text, base64);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }
}
