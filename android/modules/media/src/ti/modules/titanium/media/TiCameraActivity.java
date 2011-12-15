package ti.modules.titanium.media;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.proxy.TiViewProxy;
import ti.modules.titanium.media.MediaModule;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import org.appcelerator.titanium.kroll.KrollCallback;

public class TiCameraActivity extends TiBaseActivity implements SurfaceHolder.Callback {
	private static final String LCAT = "TiCameraActivity";
	private static Camera camera;

	private TiViewProxy localOverlayProxy = null;
	private Uri storageUri;
	private SurfaceView preview;
	private FrameLayout previewLayout;

	public static boolean autohide = true;
	public static TiViewProxy overlayProxy = null;
	public static TiCameraActivity cameraActivity = null;
	public static KrollCallback successCallback = null;
	public static KrollCallback errorCallback = null;
	public static KrollCallback cancelCallback = null;
	public static MediaModule mediaModule = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set picture location
		storageUri = (Uri)getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);

		// create camera preview
		preview = new SurfaceView(this);
		SurfaceHolder previewHolder = preview.getHolder();
		previewHolder.addCallback(this);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// set preview overlay
		localOverlayProxy = overlayProxy;
		overlayProxy = null; // clear the static object once we have a local reference

		// set overall layout - will populate in onResume
		previewLayout = new FrameLayout(this);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(previewLayout);
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(w, h);
		camera.setParameters(parameters);
		camera.startPreview();
	}

	public void surfaceCreated(SurfaceHolder previewHolder) {
		camera = Camera.open();

		// using default preview size may be causing problem on some devices, setting dimensions manually
		/* Parameters cameraParams = camera.getParameters();
		Camera.Size previewSize = cameraParams.getSupportedPreviewSizes().get((cameraParams.getSupportedPreviewSizes().size()) - 1);
		cameraParams.setPreviewSize(previewSize.width, previewSize.height );
		camera.setParameters(cameraParams); */

		try {
			Log.i(LCAT, "setting preview display");
			camera.setPreviewDisplay(previewHolder);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	// make sure to call release() otherwise you will have to force kill the app before 
	// the built in camera will open
	public void surfaceDestroyed(SurfaceHolder previewHolder) {
		camera.release();
		camera = null;
	}

	@Override
	protected void onResume() {
		super.onResume();

		cameraActivity = this;
		previewLayout.addView(preview);
		previewLayout.addView(localOverlayProxy.getView(this).getNativeView(), new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	protected void onPause() {
		super.onPause();

		previewLayout.removeView(preview);
		previewLayout.removeView(localOverlayProxy.getView(this).getNativeView());

		try {
			camera.release();
			camera = null;
		} catch (Throwable t) {
			Log.i(LCAT, "camera is not open, unable to release");
		}

		cameraActivity = null;
	}

	static public void takePicture() {
		Log.i(LCAT, "Taking picture");
		camera.takePicture(shutterCallback, rawCallback, jpegCallback);
	}

	// support user defined callback for this in the future?
	static ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
		}
	};

	// support user defined callback for this in the future?
	static PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
		}
	};

	static PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outputStream = null;
			try {
				// write photo to storage
				outputStream = new FileOutputStream(cameraActivity.storageUri.getPath());
				outputStream.write(data);
				outputStream.close();
				if(autohide) {
					cameraActivity.setResult(Activity.RESULT_OK);
					cameraActivity.finish();
				}
				try {
					if (successCallback != null) {
						successCallback.callAsync(mediaModule.createDictForImage(cameraActivity.storageUri.toString(), "image/jpeg"));
					}
				} catch (OutOfMemoryError e) {
					String msg = "Not enough memory to get image: " + e.getMessage();
					Log.e(LCAT, msg);
				}									
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}


