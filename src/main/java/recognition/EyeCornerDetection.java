package recognition;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.CLAHE;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.opencv.core.MatOfPoint;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import static org.bytedeco.javacv.Java2DFrameUtils.toBufferedImage;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class EyeCornerDetection {

    public static void main(String[] args) {
//        detectOpenedEyes();
//        detectTiltedUpDownHead();
        detectEyes();
    }

    private static void detectTiltedUpDownHead() {
        nu.pattern.OpenCV.loadLocally();

        String faceCascadePath = "src/main/resources/haarcascade_frontalface_default.xml";
        // Load the facial landmarks model
        CascadeClassifier faceCascade = new CascadeClassifier(faceCascadePath);

        VideoCapture cap = new VideoCapture(0); // initialize video capture

        if (!cap.isOpened()) {
            System.out.println("Failed to open the camera.");
            return;
        }

        // Create a window to display the video feed
        namedWindow("Facial Landmarks Detection", WINDOW_NORMAL);
        resizeWindow("Facial Landmarks Detection", 800, 600);

        RectVector faces = new RectVector();

        Mat frame = new Mat();
        while (cap.read(frame)) {
            // Convert frame to grayscale for processing
            Mat grayFrame = new Mat();
            cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

            // Detect faces
            faces = new RectVector();
            faceCascade.detectMultiScale(grayFrame, faces);

            // Iterate over each detected face
            for (Rect face : faces.get()) {
                // Calculate the vertical position of the face
                int faceCenterY = face.y() + (face.height() / 2);
                int imageCenterY = frame.rows() / 2;
                String orientation;
                if (faceCenterY < imageCenterY) {
                    orientation = "Head is up";
                } else {
                    orientation = "Head is down";
                }

                // Calculate the angle of the face
                int faceCenterX = face.x() + (face.width() / 2);
                int imageCenterX = frame.cols() / 2;
                int angle = (int) Math.toDegrees(Math.atan2(faceCenterY - imageCenterY, faceCenterX - imageCenterX));

                // Print the results
                System.out.println(orientation);
                System.out.println("Angle: " + angle + " degrees");
            }

            // Display the processed frame
            showFrame(frame);
        }

        // Release resources
        cap.release();
        destroyAllWindows();
    }

    private static void detectTiltedRightLeftHead() {

        nu.pattern.OpenCV.loadLocally();

        String faceCascadePath = "src/main/resources/haarcascade_frontalface_default.xml";
        // Load the facial landmarks model
        CascadeClassifier faceCascade = new CascadeClassifier(faceCascadePath);

        VideoCapture cap = new VideoCapture(0); // initialize video capture

        if (!cap.isOpened()) {
            System.out.println("Failed to open the camera.");
            return;
        }

        // Create a window to display the video feed
        namedWindow("Facial Landmarks Detection", WINDOW_NORMAL);
        resizeWindow("Facial Landmarks Detection", 800, 600);

        RectVector faces = new RectVector();
        RectVector eyes = new RectVector();

        Mat frame = new Mat();
        while (cap.read(frame)) {
            // Convert frame to grayscale for processing
            Mat grayFrame = new Mat();
            cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

            // Detect faces
            faces = new RectVector();
            faceCascade.detectMultiScale(grayFrame, faces);

            // Iterate over each detected face
            for (Rect face : faces.get()) {
                // Calculate the center of the face
                Point faceCenter = new Point(face.x() + face.width() / 2, face.y() + face.height() / 2);

                // Calculate the angle of tilt
                double angle = Math.atan2(faceCenter.y() - frame.rows() / 2.0, faceCenter.x() - frame.cols() / 2.0);
                angle = Math.toDegrees(angle);

                // Adjust the angle to be positive
                if (angle < 0) {
                    angle += 360;
                }

                // Determine if the head is tilted up or down
                String tiltDirection;
                if (angle < 180) {
                    tiltDirection = "Tilted Up";
                } else {
                    tiltDirection = "Tilted Down";
                }

                // Print the tilt direction and angle
                System.out.println("Head is " + tiltDirection + " with an angle of " + Math.abs(angle) + " degrees.");
            }

            // Display the processed frame
            showFrame(frame);
        }

        // Release resources
        cap.release();
        destroyAllWindows();
    }

    private static CanvasFrame canvasFrame;

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

    private static void detectEyes() {
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

        // Create a window to display the video feed
        namedWindow("Facial Landmarks Detection", WINDOW_NORMAL);
        resizeWindow("Facial Landmarks Detection", 800, 600);

        RectVector faces = new RectVector();
        RectVector eyes = new RectVector();

        Mat frame = new Mat();
        while (cap.read(frame)) {
            // Convert frame to grayscale for processing
            Mat grayFrame = new Mat();
            cvtColor(frame, grayFrame, COLOR_BGR2GRAY);

            faceCascade.detectMultiScale(grayFrame, faces);

            // Process each detected face
            for (int i = 0; i < faces.size(); i++) {
                Rect faceRect = faces.get(i);
                rectangle(frame, faceRect, new Scalar(0, 255, 0, 0));

                // Detect eyes within the face region
                eyes = new RectVector();
                Mat faceROI = grayFrame.apply(faceRect);
                eyeCascade.detectMultiScale(faceROI, eyes);

                // Process each detected eye
                for (int j = 0; j < eyes.size(); j++) {
                    Rect eyeRect = eyes.get(j);
                    // Apply Gaussian blur to reduce noise
                    Mat blurred = new Mat();
                    GaussianBlur(faceROI, blurred, new Size(5, 5), 0);

                    // Perform Canny edge detection
                    Mat edges = new Mat();
                    double threshold1 = 50;  // Adjust these thresholds as needed
                    double threshold2 = 100;
                    Canny(blurred, edges, threshold1, threshold2);

                    // Find contours in the image
                    Mat hierarchy = new Mat();
                    MatVector contours = new MatVector();
                    findContours(edges, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

                    // Find the largest contour (assumed to be the iris)
                    Mat largestContour = null;
                    double maxArea = 0;
                    for (int k = 0; k < contours.size(); k++) {
                        Mat contour = new Mat(contours.get(k));
                        double area = contourArea(contour);
                        if (area > maxArea) {
                            largestContour = contour;
                            maxArea = area;
                        }
                    }

                    // Draw the iris contour on the original image
                    drawContours(frame, new MatVector(largestContour), -1, new Scalar(0, 255, 0, 0));
                }
            }

            // Display the processed frame
            showFrame(frame);
        }

        // Release resources
        cap.release();
        destroyAllWindows();
    }

    private static void detectHeadTurns() {
        nu.pattern.OpenCV.loadLocally();
        // Load the Haar cascade classifier for eye detection
        // Load the face detection and facial landmarks models
        // Load the face detection model
        String faceCascadePath = "src/main/resources/haarcascade_frontalface_default.xml";
        String eyeCascadePath = "src/main/resources/haarcascade_eye.xml";
        // Load the facial landmarks model
        String landmarksPath = "src/main/resources/shape_predictor_68_face_landmarks.dat";
        CascadeClassifier faceCascade = new CascadeClassifier(faceCascadePath);
        CascadeClassifier eyeCascade = new CascadeClassifier(eyeCascadePath);

        // Initialize the video capture
        VideoCapture capture = new VideoCapture(0); // 0 represents the default camera

        if (!capture.isOpened()) {
            System.out.println("Failed to open camera!");
            return;
        }

        // Create a window to display the video feed
        namedWindow("Facial Landmarks Detection", WINDOW_NORMAL);
        resizeWindow("Facial Landmarks Detection", 800, 600);

        Mat frame = new Mat();
        Mat gray = new Mat();
        RectVector faces = new RectVector();
        RectVector eyes = new RectVector();

        while (true) {
            // Grab a frame from the video capture
            capture.read(frame);

            // Convert the frame to grayscale
            cvtColor(frame, gray, COLOR_BGR2GRAY);

            // Detect faces in the grayscale frame
            faceCascade.detectMultiScale(gray, faces);

            for (int i = 0; i < faces.size(); i++) {
                Rect faceRect = faces.get(i);

                // Draw a rectangle around the face
                rectangle(frame, faceRect, new Scalar(0, 255, 0, 0), 2, LINE_AA, 0);

                // Get the region of interest (ROI) for the face
                Mat faceROI = new Mat(gray, faceRect);

                // Detect eyes within the face ROI
                eyeCascade.detectMultiScale(faceROI, eyes);

                // Store the eye coordinates
                Rect eye1 = null;
                Rect eye2 = null;

                for (int j = 0; j < eyes.size(); j++) {
                    Rect eyeRect = eyes.get(j);
                    rectangle(frame, new Rect(faceRect.x() + eyeRect.x(), faceRect.y() + eyeRect.y(), eyeRect.width(), eyeRect.height()), new Scalar(0, 0, 255, 0), 2, LINE_AA, 0);

                    if (j == 0) {
                        eye1 = eyeRect;
                    } else if (j == 1) {
                        eye2 = eyeRect;
                    }
                }

                // Check if both eyes are detected
                if (eye1 != null && eye2 != null) {
                    Rect leftEye, rightEye;

                    // Determine the left and right eye
                    if (eye1.x() < eye2.x()) {
                        leftEye = eye1;
                        rightEye = eye2;
                    } else {
                        leftEye = eye2;
                        rightEye = eye1;
                    }

                    // Calculate the eye centers
                    Point leftEyeCenter = new Point(faceRect.x() + leftEye.x() + leftEye.width() / 2, faceRect.y() + leftEye.y() + leftEye.height() / 2);
                    Point rightEyeCenter = new Point(faceRect.x() + rightEye.x() + rightEye.width() / 2, faceRect.y() + rightEye.y() + rightEye.height() / 2);

                    // Calculate the angle between the eyes
                    double deltaX = rightEyeCenter.x() - leftEyeCenter.x();
                    double deltaY = rightEyeCenter.y() - leftEyeCenter.y();
                    double angle = Math.atan2(deltaY, deltaX) * 180 / Math.PI;

                    // Classify the head tilt direction
                    String tiltDirection;
                    if (angle > 10) {
                        tiltDirection = "RIGHT TILT: " + (int) angle + " degrees";
                    } else if (angle < -10) {
                        tiltDirection = "LEFT TILT: " + (int) angle + " degrees";
                    } else {
                        tiltDirection = "STRAIGHT";
                    }

                    // Display the tilt direction on the frame
                    putText(frame, tiltDirection, new Point(20, 30), FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0, 0), 2, LINE_AA, false);
                }
            }

            // Display the frame
            imshow("Facial Landmarks Detection", frame);

            // Check for the escape key (ASCII code 27) to exit the loop
            if (waitKey(1) == 27) {
                break;
            }
        }

        // Release the video capture and destroy the window
        capture.release();
        destroyAllWindows();
    }
}