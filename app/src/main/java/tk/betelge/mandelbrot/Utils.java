package tk.betelge.mandelbrot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Objects;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Toast;

public class Utils {
	public static Bitmap takeScreenshot(Activity activity, int ResourceID) {
		View view = activity.getWindow().getDecorView().findViewById(ResourceID);
		return takeScreenshot(activity, view);
	}
	
	public static Bitmap takeScreenshot(Activity activity, View view) {
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight()); 
		view.setDrawingCacheEnabled(true);
		final Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
		view.setDrawingCacheEnabled(false);
		
		return saveBitmapToFile(activity, bitmap);
	}
	
	public static void displayFileError(Activity activity) {
		Toast.makeText(activity, "Can't write image to gallery", Toast.LENGTH_LONG).show();
	}
	
	public static void displaySuccesToast(Activity activity) {
		Toast.makeText(activity, "Image saved to gallery", Toast.LENGTH_SHORT).show();
	}
	
	public static Bitmap saveBitmapToFile(Activity activity, Bitmap bitmap) {
		Calendar cal = Calendar.getInstance();
		String date = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DATE)
				+ "-" + cal.get(Calendar.HOUR) + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ContentResolver resolver = activity.getContentResolver();
			ContentValues contentValues = new ContentValues();
			contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "mandelbrot_" + date + ".png");
			contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
			contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES
					+ File.separator + "Fractals");
			contentValues.put(MediaStore.MediaColumns.IS_PENDING, true);
			Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
			try {
				OutputStream fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));

				bitmap.compress(CompressFormat.PNG, 90, fos);
				fos.flush();
				fos.close();

				contentValues.put(MediaStore.MediaColumns.IS_PENDING, false);
				resolver.update(imageUri, contentValues, null, null);

				displaySuccesToast(activity);
			} catch (FileNotFoundException e) {
				displayFileError(activity);
				e.printStackTrace();
			} catch (IOException e) {
				displayFileError(activity);
				e.printStackTrace();
			}


		}
		else {
			String mPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
			File imageFile = new File(mPath);
			boolean create = imageFile.mkdirs();
			boolean canWrite = imageFile.canWrite();

			String filename = null;
			int i = 0;
			while (imageFile.exists()) {
				i++;
				filename = date + "_mandelbrot" + i + ".png";
				imageFile = new File(mPath, filename);
				boolean canWrite2 = imageFile.canWrite();

			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			/*resultB*/
			bitmap.compress(CompressFormat.PNG, 90, bos);
			byte[] bitmapdata = bos.toByteArray();

			try {
				//write the bytes in file
				FileOutputStream fos = new FileOutputStream(imageFile);
				fos.write(bitmapdata);
				fos.flush();
				fos.close();

				Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				intent.setData(Uri.fromFile(imageFile));
				activity.sendBroadcast(intent);

				displaySuccesToast(activity);
			} catch (FileNotFoundException e) {
				displayFileError(activity);
				e.printStackTrace();
			} catch (IOException e) {
				displayFileError(activity);
				e.printStackTrace();
			}

		}
		return bitmap;
	}
}
