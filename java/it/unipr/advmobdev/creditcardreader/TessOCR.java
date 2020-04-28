package it.unipr.advmobdev.creditcardreader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

public class TessOCR
{
    private TessBaseAPI mTess;
    private static final String TAG = "TessOCR";


    public TessOCR()
    {
        mTess = new TessBaseAPI();
        String dataPath = Environment.getExternalStorageDirectory() + "/tesseract/";
        String language = "eng";
        String url = "https://github.com/tesseract-ocr/tessdata/raw/master/eng.traineddata";

        File dir = new File(dataPath + "tessdata/");
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dataPath + "tessdata/eng.traineddata");
        if(!file.exists())
        {
            // if tesseract file for english detection does not exist,
            // it gets downloaded from the official Tesseract Github repository
            downloadFile(url, file);
        }

        mTess.init(dataPath, language);
    }


    public String getOCRResult(Bitmap bitmap)
    {
        Bitmap convertedBitmap = convert(bitmap, Bitmap.Config.ARGB_8888);

        mTess.setImage(convertedBitmap);
        String result = mTess.getUTF8Text();
        return result;
    }


    public void onDestroy()
    {
        if (mTess != null)
            mTess.end();
    }


    private Bitmap convert(Bitmap bitmap, Bitmap.Config config)
    {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }


    // Should be replaced with Download manager...
    // https://developer.android.com/reference/android/app/DownloadManager
    // <uses-permission android:name="android.permission.INTERNET"/> should be added
    private static void downloadFile(String url, File outputFile)
    {
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            int contentLength = conn.getContentLength();

            DataInputStream stream = new DataInputStream(u.openStream());

            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();

            DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
            fos.write(buffer);
            fos.flush();
            fos.close();
        } catch(FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }
}
