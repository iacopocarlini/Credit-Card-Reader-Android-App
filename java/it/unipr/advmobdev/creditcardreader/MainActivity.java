package it.unipr.advmobdev.creditcardreader;

/*
Iacopo Carlini, Università di Parma
Progetto per il corso di Advanced Programming of Mobile Systems
A.A. 2019/2020
*/

// IMPORT
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// CLASS
public class MainActivity extends AppCompatActivity
{
    // GLOBAL VARIABLES

    // + Java / Android
    DatabaseHelper mDatabaseHelper;
    Button saveCard;
    private static final String TAG = "CardReaderTag";
    private static final int SELECT_PICTURE = 1, TAKE_PICTURE = 2, MINLENGTH = 20;
    private static final String numberTVplaceholder = "Card Number: ";
    private static final String expirationDateTVplaceholder = "Expiration Date: ";
    private static int MY_PERMISSIONS;
    private String selectedImagePath;
    private String cardNumber = "";
    private String expirationMonth = "";
    private String expirationYear = "";
    private Uri photoURI;
    private TextView numberTextView, expirationDateTextView;
    private ImageButton addImageButton;

    // + OpenCV
    Mat sampledImage = null;
    Mat originalImage = null;

    // + Tesseract OCR
    private TessOCR mTessOCR;
    private ProgressDialog mProgressDialog;

    // Card number patterns (VISA, MASTERCARD, DISCOVER, AMEX, DINERS, JCB)
    private static final String REGEX = ".*(?<visa>4[0-9]{3}\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*|" +
            ".*(?<mastercard>5[1-5][0-9]{2}\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*|" +
            ".*(?<discover>6(?:011|5[0-9]{3})\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*|" +
            ".*(?<amex>3[47][0-9]{2}\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*|" +
            ".*(?<diners>3(?:0[0-5]|[68][0-9])[0-9]\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*|" +
            ".*(?<jcb>(?:2131|1800|35[0-9]{2})\\s[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}).*";

    // Card brands
    private String[] groups = {"visa", "mastercard", "discover", "amex", "diners", "jcb"};

    // + Custom class
    CreditCard card; // Model
    Utility util;

    ////////////////////////////////////////////////////////////////////////////////////


    // METHODS


    // VIEW SETUP AND ANDROID UTILITIES
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully"); // debug
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Utility
        util = new Utility(getApplicationContext());

        // DB
        mDatabaseHelper = new DatabaseHelper(this);

        // Permissions
        askPermissions();

        // Tesseract OCR
        mTessOCR = new TessOCR();

        // Image button
        addImageButton = (ImageButton)findViewById(R.id.picView);
        imageButtonSetup();

        // Text Views
        numberTextView = (TextView) findViewById(R.id.NumberTextView);
        expirationDateTextView = (TextView) findViewById(R.id.ExpirationDateTextView);

        // Save Card Button
        saveCardButtonSetup();

    }


    public void askPermissions()
    {
        // PERMISSIONS: READ SD CARD - WRITE SD CARD - CAMERA
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            // No explanation needed; request all permissions once
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    MY_PERMISSIONS);
        }
        // All permissions granted
    }


    public void imageButtonSetup()
    {
        addImageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final CharSequence[] options = {"Take Photo", "Choose from Gallery"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose picture");

                builder.setItems(options, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int item)
                    {

                        if (options[item].equals("Take Photo"))
                        {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                            if (cameraIntent.resolveActivity(getPackageManager()) != null)
                            {
                                File pictureFile = null;

                                try {
                                    pictureFile = getPictureFile();
                                } catch (IOException ex) {
                                    util.displayToast("Photo file can't be created, please try again");
                                }

                                if (pictureFile != null)
                                {
                                    photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                            "it.unipr.advmobdev.creditcardreader.provider",
                                            pictureFile);
                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                    startActivityForResult(cameraIntent, TAKE_PICTURE);
                                }
                            }
                        }

                        else if (options[item].equals("Choose from Gallery"))
                        {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_PICK);
                            startActivityForResult(Intent.createChooser(intent, "Pick Image"), SELECT_PICTURE);
                        }
                    }
                });
                builder.show();
            }
        });
    }


    public void saveCardButtonSetup()
    {
        saveCard = findViewById(R.id.saveButton);
        saveCard.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (card.isValid())
                    addCard(card);
                else
                    util.displayToast("First, scan a credit card correctly");
            }
        });
    }


    public void addCard(CreditCard c)
    {
        if (mDatabaseHelper.isDuplicate(c))
            util.displayToast("Card already exists...");
        else
        {
            boolean insertData = mDatabaseHelper.addData(c);

            Log.i(TAG, "Saving image in DB with path: " + c.getImagePath()); // Debug

            if (insertData)
                util.displayToast("Data Successfully Inserted!");
            else
                util.displayToast("Something went wrong in saving your data");
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mTessOCR.onDestroy();
    }


    @Override
    // Menu Options: 1) View Card DB
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_ViewCardDB: // Display cards list
                Intent intent = new Intent(this, ListDataActivity.class);
                startActivity(intent);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if (requestCode == SELECT_PICTURE)
            {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);

                Log.i(TAG, "selectedImagePath: " + selectedImagePath); // Debug

                sampledImage = new Mat();
            }

            if (requestCode == TAKE_PICTURE)
            {
                Log.i(TAG, "Captured Path: " + selectedImagePath); // Debug
                sampledImage = new Mat();
            }


            // 1) Get image from file system or capture
            loadImage(selectedImagePath, sampledImage);

            // 2) OpenCv processing
            Mat processedImage = preProcessing(sampledImage);

            // 3) show processing result
            Bitmap bitMap = displayImage(processedImage);

            // 4) perform OCR on result
            doOCR(bitMap);
        }
    }


    private File getPictureFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
        String pictureFile = "CardReader_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        selectedImagePath = image.getAbsolutePath();
        return image;
    }


    private String getPath(Uri uri)
    {
        if (uri == null)
            return null;

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection,
                null, null, null);

        if (cursor != null)
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }

        return uri.getPath();
    }


    private double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight)
    {
        int height = srcImage.height();
        int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth)
        {
            double heightRatio = (double) reqHeight / (double) height;
            double widthRatio = (double) reqWidth / (double) width;

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }


    private void loadImage(String path, Mat sampledImage)
    {
        originalImage = Imgcodecs.imread(path);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        double downSampleRatio = calculateSubSampleSize(rgbImage, width, height);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio, downSampleRatio, Imgproc.INTER_AREA);
        try {
            ExifInterface exif = new ExifInterface(selectedImagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
            // ottieni l'immagine specchiata
                    sampledImage = sampledImage.t();
            // flip lungo l'asse y
                    Core.flip(sampledImage, sampledImage, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
            // ottieni l'immagine "sotto-sopra"
                    sampledImage = sampledImage.t();
            // flip lungo l'asse x
                    Core.flip(sampledImage, sampledImage, 0);
                    break;
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private Bitmap displayImage(Mat image)
    {
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitMap);
        addImageButton.setImageBitmap(bitMap);

        return bitMap;  // gives bitmap back to be processed for OCR
    }


    private void displayOCRResult(CreditCard c)
    {
        // ERROR HANDLING
        if (!c.isValid())
            util.displayToast("Something went wrong - Try to take a better picture");


        String numberTVFiller = numberTVplaceholder + c.getCardNumber();
        numberTextView.setText(numberTVFiller);

        String expDateTVFiller = expirationDateTVplaceholder;
        if (c.getExpirationMonth().length() > 0 && c.getExpirationYear().length() > 0)
            expDateTVFiller += c.getExpirationMonth() + "/" + c.getExpirationYear();

        expirationDateTextView.setText(expDateTVFiller);
    }


    // ****************************************************************************** //


    // IMAGE PROCESSING

    private Mat preProcessing(Mat image)
    {
        Mat rectangleImage = findCard(image); // get card rectangle... (RoI)

        // Region of Interest processing
        Imgproc.cvtColor(rectangleImage, rectangleImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(rectangleImage, rectangleImage, new Size(3, 3), 0);
        Imgproc.threshold(rectangleImage, rectangleImage, 0, 255, Imgproc.THRESH_OTSU);

        return rectangleImage;
    }


    // Looking for card rectangle and straightening Region of Interest
    private Mat findCard(Mat src)
    {

        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve, approxCurveTemp;
        approxCurve = new MatOfPoint2f();

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) // channels
        {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++)
            {
                if (t == 0)
                {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true);
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1);
                }
                else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours)
                {
                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    double area = Imgproc.contourArea(contour);
                    approxCurveTemp = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurveTemp,
                            Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurveTemp.total() == 4 && area >= maxArea)
                    {
                        double maxCosine = 0;

                        List<Point> curves = approxCurveTemp.toList();
                        for (int j = 2; j < 5; j++)
                        {
                            double cosine = Math.abs(angle(curves.get(j % 4), curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3)
                        {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                            approxCurve = approxCurveTemp;
                        }
                    }
                }
            }
        }

        // Region of Interest
        Mat result  = new Mat();

        if (maxId >= 0)
        {
            Mat start = findCorners(approxCurve);
            result = warp(src, start, 960, 720);
        }

        return result;
    }


    // Looking for card corners
    private Mat findCorners(MatOfPoint2f approxCurve)
    {
        double[] tempDouble;

        // P1
        tempDouble = approxCurve.get(0,0);
        Point p1 = new Point(tempDouble[0], tempDouble[1]);

        // P2
        tempDouble = approxCurve.get(1,0);
        Point p2 = new Point(tempDouble[0], tempDouble[1]);

        // P3
        tempDouble = approxCurve.get(2,0);
        Point p3 = new Point(tempDouble[0], tempDouble[1]);

        // P4
        tempDouble = approxCurve.get(3,0);
        Point p4 = new Point(tempDouble[0], tempDouble[1]);

        /* Draw Circle around corners...
        Imgproc.circle(image,p1,30,new Scalar(0,0,255));
        Imgproc.circle(image,p2,30,new Scalar(255,255,255));
        Imgproc.circle(image,p3,30,new Scalar(255,0,0));
        Imgproc.circle(image,p4,30,new Scalar(0,0,255));
        */

        List<Point> source = new ArrayList<Point>();
        source.add(p1);
        source.add(p2);
        source.add(p3);
        source.add(p4);

        // SORTING CORNERS
        List<Point> sortedCorners = sortCorners(source);
        Mat sortedCornersMat = Converters.vector_Point2f_to_Mat(sortedCorners);

        return sortedCornersMat;
    }


    // Straightening Region of Interest
    private Mat warp(Mat inputMat, Mat startM, int width, int height)
    {
        Mat outputMat = new Mat(width, height, CvType.CV_8UC4);

        // Destination points, anti clockwise order
        Point ocvPOut1 = new Point(0, 0); // top left
        Point ocvPOut2 = new Point(0, height); // bottom left
        Point ocvPOut3 = new Point(width, height); // bottom right
        Point ocvPOut4 = new Point(width, 0); // top right

        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat,
                                outputMat,
                                perspectiveTransform,
                                new Size(width, height),
                                Imgproc.INTER_CUBIC);

        return outputMat;
    }


    // Math utility function
    private double angle(Point p1, Point p2, Point p0)
    {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }


    // Sorting a list of points in anti clockwise order
    private List<Point> sortCorners(List<Point> corners)
    {
        List<Point> top = new ArrayList<Point>();
        List<Point> bottom = new ArrayList<Point>();

        Point cornerTL, cornerTR, cornerBL, cornerBR;
        double minX, maxX, minY, maxY;

        Point p = corners.get(0);
        minX = maxX = p.x;
        minY = maxY = p.y;

        // TOP BOTTOM
        for (Point point : corners)
        {
            if (point.x < minX)
                minX = point.x;

            if (point.x > maxX)
                maxX = point.x;

            if (point.y < minY)
                minY = point.y;

            if (point.y > maxY)
                maxY = point.y;
        }

        // Assumption: card has a low rotation angle from ideal horizontal position
        for (Point point : corners)
        {
            if (Math.abs(point.y - minY) < Math.abs(point.y - maxY))
                top.add(point);
            else
                bottom.add(point);
        }

        // TOP
        if (top.get(0).x < top.get(1).x)
        {
            cornerTL = top.get(0);
            cornerTR = top.get(1);
        }
        else {
            cornerTL = top.get(1);
            cornerTR = top.get(0);
        }

        // BOTTOM
        if (bottom.get(0).x < bottom.get(1).x)
        {
            cornerBL = bottom.get(0);
            cornerBR = bottom.get(1);
        }
        else {
            cornerBL = bottom.get(1);
            cornerBR = bottom.get(0);
        }

        List<Point> sortedCorners = new ArrayList<Point>();
        sortedCorners.add(cornerTL);
        sortedCorners.add(cornerBL);
        sortedCorners.add(cornerBR);
        sortedCorners.add(cornerTR);

        return sortedCorners;
    }


    // ****************************************************************************** //


    //  OCR
    private void doOCR(final Bitmap bitmap)
    {
        if (mProgressDialog == null)
            mProgressDialog = ProgressDialog.show(this, "Info",
                    "Loading...", true);
        else
            mProgressDialog.show();


        // Special thread for OCR
        new Thread(new Runnable()
        {
            public void run()
            {
                final String result = mTessOCR.getOCRResult(bitmap);

                // This thread updates UI only after OCR completion
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (result != null && !result.equals(""))
                        {
                            textResultHandling(result);
                        }
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();

    }


    private void textResultHandling(String OCRresult)
    {

        int slashIndex = OCRresult.indexOf('/'); // -1 -> no slash found

        String processedString = ocrProcessing(OCRresult); // replacing "\n" "\r" "." and other if needed...

        // Extracting card number and expiration date

        // 1) CARD NUMBER - different patterns collected in REGEX
        if (processedString.length() >= MINLENGTH)
        {
            // ANDROID OREO 8.0, API >= 26
            // usage of pattern matchers and groups -- (matcher.group("...") not available below API 26)
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                Pattern pattern = Pattern.compile(REGEX);
                Matcher matcher = pattern.matcher(processedString);

                if(matcher.matches())
                {
                    for (int i = 0; i < groups.length; i++)
                    {
                        String match = matcher.group(groups[i]);
                        if (match != null)
                        {
                            cardNumber = match;
                            Log.i(TAG, "Card brand is: " + groups[i]); // Debug
                        }
                    }
                }
                else
                    cardNumber = "";

            }

            // API < 26
            // extracting card number using processed result substring
            else
            {
                processedString = OCRresult.replaceAll("[^\\d]", "");
                cardNumber = processedString.substring(0, 4) +
                            " " +
                            processedString.substring(4, 8) +
                            " " +
                            processedString.substring(8, 12) +
                            " " +
                            processedString.substring(12, 16);
            }
        }
        else
        {
            cardNumber = "";
        }



        // 2) EXPIRATION DATE  Pattern: ##/##, "#" is a digit
        if (OCRresult.indexOf('/') != -1
            && (slashIndex+1) < OCRresult.length()
            && (slashIndex+3) < OCRresult.length())
        {
            // EXPIRATION DATE  Pattern: ##/##, "#" is a digit
            expirationMonth = OCRresult.substring(slashIndex - 2, slashIndex); // left side of the slash
            expirationYear = OCRresult.substring(slashIndex + 1, slashIndex + 3); // right side of the slash
        }
        else
        {
            expirationMonth = "";
            expirationYear = "";
        }


        // +++ CREDIT CARD OBJECT CREATION
        card = new CreditCard(cardNumber, expirationMonth, expirationYear, selectedImagePath);
        // +++


        // put OCR result into views
        displayOCRResult(card);
    }


    private String ocrProcessing(String s)
    {
        Log.i(TAG, "-> OCR processing input is: " + s); // Debug

        String result;
        result = s.replace("\n", "").replace("\r", "");
        result = result.replace(".", "");

        Log.i(TAG, "<- OCR processing output is: " + result); // Debug

        return result;
    }

}