package recognition;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import static org.bytedeco.javacv.Java2DFrameUtils.toBufferedImage;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class EyeCornerDetection {

    private static CanvasFrame canvasFrame;

    public static void main(String[] args) {
        detectCheating();
    }

    private static void detectCheating() {
        nu.pattern.OpenCV.loadLocally();

        String faceCascadePath = "src/main/resources/haarcascade_frontalface_default.xml";
        String eyeCascadePath = "src/main/resources/haarcascade_eye.xml";
        // Load the facial landmarks model
        CascadeClassifier faceCascade = new CascadeClassifier(faceCascadePath);
        CascadeClassifier eyeCascade = new CascadeClassifier(eyeCascadePath);

        VideoCapture cap = new VideoCapture(0); // initialize video capture

        if (!cap.isOpened()) {
            System.out.println("Failed to open the camera.");
            return;
        }

        RectVector faces;
        RectVector eyes;

        Mat frame = new Mat();
        List<Double> trainingVertical = new ArrayList<>();
        int verticalLimit = 100;
        double average = Integer.MAX_VALUE;
        int numberOfPossibleCheating = 0;
        boolean horizontalMoves = false;
        boolean verticalMoves = false;
        boolean eyesMoves = false;
        while (cap.read(frame)) {
            // Convert frame to grayscale for processing
            Mat grayFrame = new Mat();
            cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

            // Detect faces
            faces = new RectVector();
            faceCascade.detectMultiScale(grayFrame, faces);

            // Iterate over each detected face
            for (Rect face : faces.get()) {
                // left/right head
                // Calculate the vertical position of the face
                int faceCenterY = face.y() + (face.height() / 2);
                int imageCenterY = frame.rows() / 2;
                String orientation;

                // Calculate the angle of the face
                int faceCenterX = face.x() + (face.width() / 2);
                int imageCenterX = frame.cols() / 2;
                int angle = (int) Math.toDegrees(Math.atan2(faceCenterY - imageCenterY, faceCenterX - imageCenterX));

                if (angle < 40) {
                    horizontalMoves = true;
                    orientation = "Head is left";
                } else if (angle > 60) {
                    horizontalMoves = true;
                    orientation = "Head is right";
                } else {
                    orientation = "Head is straight";
                }

                // up/down head
                // Calculate the center of the face
                Point faceCenter = new Point(face.x() + face.width() / 2, face.y() + face.height() / 2);

                // Calculate the angle of tilt
                double angleVertical = Math.atan2(faceCenter.y() - frame.rows() / 2.0, faceCenter.x() - frame.cols() / 2.0);
                angleVertical = Math.toDegrees(angleVertical);

                // Adjust the angle to be positive
                if (angleVertical < 0) {
                    angleVertical += 360;
                }
                // Determine if the head is tilted up or down
                String tiltDirection = "";
                if(trainingVertical.size() < verticalLimit) {
                    trainingVertical.add(angleVertical);
                    verticalLimit++;
                } else {
                    if(average == Integer.MAX_VALUE) {
                        OptionalDouble v = trainingVertical.stream().mapToDouble(value -> value).average();
                        if(v.isPresent()) {
                            average = v.getAsDouble();
                        }
                    }
                    if(average + 20 < angleVertical) {
                        verticalMoves = true;
                        tiltDirection = "Tilted Up";
                    } else if(average - 20 > angleVertical) {
                        verticalMoves = true;
                        tiltDirection = "Tilted Down";
                    }
                }

                // eyes moving
                rectangle(frame, face, new Scalar(0, 255, 0, 0));

                // Detect eyes within the face region
                eyes = new RectVector();
                Mat faceROI = grayFrame.apply(face);
                eyeCascade.detectMultiScale(faceROI, eyes);

                // Calculate the eye centers
                Point[] eyeCenters = new Point[2];
                int counter = 0;
                String looking = "";
                if (eyes.get().length == 2) {
                    for (Rect eyeRect : eyes.get()) {
                        Point center = new Point((int) (face.x() + eyeRect.x() + eyeRect.width() * 0.5),
                                (int) (face.y() + eyeRect.y() + eyeRect.height() * 0.5));
                        eyeCenters[counter++] = center;
                    }
                    // If the horizontal distance between the eye centers is greater than a threshold,
                    // consider it as looking aside
                    double horizontalDistance = Math.abs(eyeCenters[0].x() - eyeCenters[1].x());
                    double threshold = 60; // Adjust the threshold as needed
                    if (horizontalDistance < threshold) {
                        eyesMoves = true;
                        looking = "Person is looking aside";
                    } else {
                        looking = "Person is looking straight";
                    }
                }

                if(eyesMoves || (verticalMoves && horizontalMoves)) {
                    numberOfPossibleCheating++;
                }

                horizontalMoves = false;
                verticalMoves = false;
                eyesMoves = false;

//                printResultsInConsole(orientation, angle, angleVertical, tiltDirection, looking);
            }
            if(numberOfPossibleCheating >= 200) {
                break;
            }
//            System.out.println(numberOfPossibleCheating);

            // Display the processed frame
            showFrame(frame);
        }

        if(numberOfPossibleCheating >= 200) {
            System.out.println("Cheating detected");
        }

        // Release resources
        cap.release();
        destroyAllWindows();
        System.exit(0);
    }

    private static void printResultsInConsole(String orientation, double angleHorizontal, double angleVertical,
                                              String tiltDirection, String looking) {
        // right/left angle
        System.out.println(orientation);
        System.out.println("Angle (right : left): " + angleHorizontal + " degrees");

        // up/down angle
        System.out.println("Head is " + tiltDirection + " with an angle of " + Math.abs(angleVertical) + " degrees.");

        // eyes positions
        System.out.println(looking);
    }

    private static void showFrame(Mat frame) {
        if (canvasFrame == null) {
            canvasFrame = new CanvasFrame("Eye Detection", 1);
            canvasFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }

        if (!canvasFrame.isVisible()) {
            canvasFrame.setVisible(true);
        }

        BufferedImage image = toBufferedImage(frame);
        canvasFrame.showImage(image);
    }
}