package org.example.projet_pi.Service;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FaceRecognitionService {

    private org.bytedeco.opencv.opencv_objdetect.CascadeClassifier faceDetector;

    public FaceRecognitionService() {
        try {
            // Charger la bibliothèque native OpenCV
            Loader.load(org.bytedeco.opencv.global.opencv_objdetect.class);

            String classifierPath = "src/main/resources/haarcascade_frontalface_default.xml";
            faceDetector = new org.bytedeco.opencv.opencv_objdetect.CascadeClassifier(classifierPath);

            if (faceDetector.isNull()) {
                System.err.println("⚠️ Erreur: Impossible de charger le classificateur de visage");
            } else {
                System.out.println("✅ Classificateur de visage chargé avec succès");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public byte[] extractFaceFeatures(MultipartFile image) throws IOException {
        Mat imageMat = convertToMat(image);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(imageMat, faces);

        if (faces.size() == 0) {
            throw new RuntimeException("Aucun visage détecté dans l'image");
        }

        if (faces.size() > 1) {
            throw new RuntimeException("Plusieurs visages détectés");
        }

        Rect faceRect = faces.get(0);
        Mat faceROI = new Mat(imageMat, faceRect);

        Mat resizedFace = new Mat();
        opencv_imgproc.resize(faceROI, resizedFace, new Size(128, 128));

        Mat grayFace = new Mat();
        opencv_imgproc.cvtColor(resizedFace, grayFace, opencv_imgproc.COLOR_BGR2GRAY);

        double[] features = extractSimpleFeatures(grayFace);

        return serializeFeatures(features);
    }

    private double[] extractSimpleFeatures(Mat grayFace) {
        int rows = grayFace.rows();
        int cols = grayFace.cols();
        int cellSize = 16;
        int numCellsX = cols / cellSize;
        int numCellsY = rows / cellSize;

        List<Double> features = new ArrayList<>();

        for (int i = 0; i < numCellsY; i++) {
            for (int j = 0; j < numCellsX; j++) {
                double sum = 0;
                int count = 0;
                for (int y = i * cellSize; y < (i + 1) * cellSize && y < rows; y++) {
                    for (int x = j * cellSize; x < (j + 1) * cellSize && x < cols; x++) {
                        sum += (grayFace.data().get(y * cols + x) & 0xFF);
                        count++;
                    }
                }
                features.add(sum / count);
            }
        }

        double[] result = new double[features.size()];
        for (int i = 0; i < features.size(); i++) {
            result[i] = features.get(i);
        }
        return result;
    }

    public double compareFaces(byte[] features1, byte[] features2) {
        double[] f1 = deserializeFeatures(features1);
        double[] f2 = deserializeFeatures(features2);

        if (f1.length != f2.length || f1.length == 0) {
            return 0;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < f1.length; i++) {
            dotProduct += f1[i] * f2[i];
            norm1 += f1[i] * f1[i];
            norm2 += f2[i] * f2[i];
        }

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private Mat convertToMat(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        Mat mat = opencv_imgcodecs.imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_COLOR);

        if (mat == null || mat.empty()) {
            throw new IOException("Impossible de décoder l'image");
        }
        return mat;
    }

    private byte[] serializeFeatures(double[] features) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (double f : features) {
                byte[] bytes = doubleToBytes(f);
                baos.write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur de sérialisation", e);
        }
        return baos.toByteArray();
    }

    private double[] deserializeFeatures(byte[] data) {
        int numDoubles = data.length / 8;
        double[] features = new double[numDoubles];
        for (int i = 0; i < numDoubles; i++) {
            byte[] doubleBytes = Arrays.copyOfRange(data, i * 8, (i + 1) * 8);
            features[i] = bytesToDouble(doubleBytes);
        }
        return features;
    }

    private byte[] doubleToBytes(double d) {
        long bits = Double.doubleToLongBits(d);
        return new byte[] {
                (byte) (bits >> 56), (byte) (bits >> 48),
                (byte) (bits >> 40), (byte) (bits >> 32),
                (byte) (bits >> 24), (byte) (bits >> 16),
                (byte) (bits >> 8), (byte) bits
        };
    }

    private double bytesToDouble(byte[] bytes) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= ((long) (bytes[i] & 0xFF)) << (56 - (i * 8));
        }
        return Double.longBitsToDouble(bits);
    }
}