package tk.betelge.mandelbrot;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tk.betelge.alw3d.renderer.RenderMultiPass;
import utils.ShaderLoader;
import utils.StringLoader;
import tk.betelge.alw3d.Alw3dModel;
import tk.betelge.alw3d.Alw3dView;
import tk.betelge.alw3d.math.Vector3f;
import tk.betelge.alw3d.renderer.CameraNode;
import tk.betelge.alw3d.renderer.FBO;
import tk.betelge.alw3d.renderer.Geometry;
import tk.betelge.alw3d.renderer.GeometryNode;
import tk.betelge.alw3d.renderer.Material;
import tk.betelge.alw3d.renderer.Node;
import tk.betelge.alw3d.renderer.QuadRenderPass;
import tk.betelge.alw3d.renderer.ShaderProgram;
import tk.betelge.alw3d.renderer.Texture;
import tk.betelge.alw3d.renderer.Uniform;
import tk.betelge.alw3d.renderer.Alw3dRenderer.OnSurfaceChangedListener;
import tk.betelge.alw3d.renderer.passes.CheckGlErrorPass;
import tk.betelge.alw3d.renderer.passes.ClearPass;
import tk.betelge.alw3d.renderer.passes.RenderPass;
import tk.betelge.alw3d.renderer.passes.SceneRenderPass;
import tk.betelge.alw3d.renderer.passes.RenderPass.OnRenderPassFinishedListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Mandel extends Activity implements OnTouchListener,
OnSeekBarChangeListener, OnSurfaceChangedListener, OnCheckedChangeListener,
CheckGlErrorPass.OnGlErrorListener, RenderPass.OnRenderPassFinishedListener {
	
	private static String TAG = "MANDEL";
	
	private Alw3dModel model;
	private Alw3dView view;
	
	private RenderPass clearPass;
	private RenderPass mandelPass;
	private Material mandelMaterial;
	private Material markMaterial;
	private Material mandelFloatEndMaterial;
	private RenderMode renderMode = RenderMode.SINGLE;
	private boolean renderMosaic = false;
	private int colorShader;
	private ShaderProgram mandelShaderProg;
	private ShaderProgram mandel64ShaderProg;
	private ShaderProgram mandel64ExpShaderProg;
	private ShaderProgram mandelInVertexShader;
	private ShaderProgram mandel64InVertexShader;
	private ShaderProgram mandelFloatShaderProg, mandelFloatEndShaderProg;
	private ShaderProgram mandelFloat64ShaderProg, mandelFloat64EndShaderProg;
	private int FLOAT_DRAW_STEPS = 8;
	private float scale = 1f;
	private float asp = 1f;
	private float scaleMandelX = 1f, scaleMandelY = 1f;
	private double offsetMandelX = -0.75f/2, offsetMandelY = 0f;
	static final private String SCALE = "scale";
	static final private String OFFSET_X = "offsetMandelX";
	static final private String OFFSET_Y = "offsetMandelY";
	static final private String SPLITTER = "splitter";
	static final private String MAX_ITER = "maxIter";
	static final private String RENDER_MODE = "renderMode";
	static final private String COLOR_MODE = "colorMode";
	static final private String RENDER_MOSAIC = "renderMosaic";
	static final private String HUD = "HUD";
	private Uniform scaleMandelUniform;
	private Uniform offsetMandelUniform;
	private Uniform offsetFineMandelUniform;
	
	private SceneRenderPass mandelVertexPass;
	private SceneRenderPass mandelVertexMosaicPass;
	private RenderMultiPass mandelFloatPass;

	private RenderPass finalPass;
	private Material finalMaterial;
	private ShaderProgram finalShaderProg;
	private float scaleFinalX = 1f, scaleFinalY = 1f;
	private float offsetFinalX = 0f, offsetFinalY = 0f;
	private Uniform scaleFinalUniform;
	private Uniform offsetFinalUniform;
	private ShaderProgram directShaderProg;
	
	private Uniform maxInterationsUniform;
	private Uniform splitterFloatUniform;
	
	private Uniform gradUniform;
	private Uniform col1Uniform;
	private Uniform col2Uniform;

	private Uniform currentFloatIterationUniform;
		
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	
	private FBO mandelFBO;
	private FBO mandelFBO2;
	
	private RenderPass mandelFbo2toFboPass;
	
	//private AlertDialog.Builder maxIterDialog;
	//private AlertDialog.Builder splitFloatDialog;
	
	private Toast outOfMemToast;
	private Toast otherGlErrorToast;
	private Toast doneToast;
	private Toast renderMosaicToast;
	
	private TextView posTextView;
	
	private Bitmap screenshotBitmap;
	
	final private int[] allowDouble = {-1};
	final private int[] allowLegacy = {-1};

	private int glVersion;
	private boolean hasFloatBuffers;
	private FBO mandelFloatFBO, mandelFloatPongFBO;

	private SeekBar iterSeekBar;
	final private int maxIterationsOld = 1024, maxIterations = 1024 * 128;

	private RadioGroup renderModeRadioGroup;
	
	static public enum RenderMode {
		SINGLE(0x001),
		EMULATED_DOUBLE(0x002),
		EXP_EMULATED_DOUBLE(0x003),
		SINGLE_IN_VERTEX(0x004),
		EXP_EMULATED_DOUBLE_IN_VERTEX(0x005),
		FLOAT_TEXTURE(0x006),
		FLOAT_TEXTURE_EMULATED_DOUBLE(0x007);

		int value;
		
		RenderMode(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	private boolean useFallbackInsteadOfDynLoop = false;

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Hide status bar and action bar
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	            WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		StringLoader.setContext(this);

		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

		glVersion = configurationInfo.reqGlEsVersion;
		
		// Load shaders
		colorShader = R.raw.pastelhsvcolor;
		loadShaders(colorShader);
		mandelMaterial = new Material(mandelShaderProg);
		
		maxInterationsUniform = new Uniform("MAX_ITER", 200f);
		mandelMaterial.addUniform(maxInterationsUniform);
		
		offsetMandelUniform = new Uniform("offset", (float)offsetMandelX, (float)offsetMandelY);
		mandelMaterial.addUniform(offsetMandelUniform);
		offsetFineMandelUniform = new Uniform("offsetFine", 0f, 0f);
		mandelMaterial.addUniform(offsetFineMandelUniform);
		scaleMandelUniform = new Uniform("scale", scaleMandelX, scaleMandelY);
		mandelMaterial.addUniform(scaleMandelUniform);
		splitterFloatUniform = new Uniform("split", 1025f);
		mandelMaterial.addUniform(splitterFloatUniform);
		gradUniform = new Uniform("grad", 1f);
		mandelMaterial.addUniform(gradUniform);
		Vector3f colVec1 = new Vector3f(0x83, 0x69, 0x53);
		colVec1.normalizeThis();
		col1Uniform = new Uniform("col1", colVec1.x, colVec1.y, colVec1.z);
		mandelMaterial.addUniform(col1Uniform);
		Vector3f colVec2 = new Vector3f(0x53, 0x6d, 0x83);
		colVec2.normalizeThis();
		col2Uniform = new Uniform("col2", colVec2.x, colVec2.y, colVec2.z);
		mandelMaterial.addUniform(col2Uniform);

		// Used for intermediate float draws
		currentFloatIterationUniform = new Uniform("iteration", 0, (float) FLOAT_DRAW_STEPS);
		mandelMaterial.addUniform(currentFloatIterationUniform);

		mandelFloatEndMaterial = new Material(mandelFloatEndShaderProg);
		mandelFloatEndMaterial.addUniform(maxInterationsUniform);
		mandelFloatEndMaterial.addUniform(gradUniform);
		mandelFloatEndMaterial.addUniform(col1Uniform);
		mandelFloatEndMaterial.addUniform(col2Uniform);
		mandelFloatEndMaterial.addUniform(currentFloatIterationUniform);
		mandelFloatEndMaterial.setBlending(true);

		
		mandelVertexPass = new SceneRenderPass(new Node(),
				/*Ignored dummy camera*/ new CameraNode(asp, asp, asp, asp)/*,
				mandelFBO*/);
		mandelVertexMosaicPass = new SceneRenderPass(new Node(),
				/*dummy*/new CameraNode(1f, 1f, 0.01f, 1000f));
		// TODO: Break out into separate file
		ClearPass floatClearPass = new ClearPass(ClearPass.COLOR_BUFFER_BIT);
		ClearPass floatClearPass2 = new ClearPass(ClearPass.COLOR_BUFFER_BIT);
		floatClearPass.setOneTime(true);
		floatClearPass2.setOneTime(true);
		QuadRenderPass interPass = new QuadRenderPass(mandelMaterial);
		interPass.setUseBigTriangle(true);
		QuadRenderPass endPass = new QuadRenderPass(mandelFloatEndMaterial, mandelFBO);
		final List<RenderPass> floatRenderPasses = new ArrayList<>();
		floatRenderPasses.add(floatClearPass);
		floatRenderPasses.add(floatClearPass2);
		floatRenderPasses.add(interPass);
		floatRenderPasses.add(endPass);
		mandelFloatPass = new RenderMultiPass() {
			@Override
			public List<RenderPass> getRenderPasses() {
				return floatRenderPasses;
			}
		};

		scaleDetector = new ScaleGestureDetector(this, new ScaleListener());
		gestureDetector = new GestureDetector(this, new GestureListener());
		
		
		offsetFinalUniform = new Uniform("offset", offsetFinalX, offsetFinalY);
		scaleFinalUniform = new Uniform("scale", scaleFinalX, scaleFinalY);
		
		outOfMemToast = Toast.makeText(this,
				"GPU Shader took too long to run.\n" +
				"Try simpler settings or zoom out.",
				Toast.LENGTH_LONG);
		otherGlErrorToast = Toast.makeText(this,
				"OpenGL error", Toast.LENGTH_SHORT);
		doneToast = Toast.makeText(this, "Done", Toast.LENGTH_SHORT);
		renderMosaicToast = Toast.makeText(this, "Auto-turning on render mosaic", Toast.LENGTH_SHORT);
		
		model = new Alw3dModel();
		int glContextClientVersion = glVersion >= 0x30000 ? 3 : 2;
		view = new Alw3dView(this, model, glContextClientVersion);
		//view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setContentView(R.layout.activity_mandel);
				
		view.setOnSurfaceChangedListener(this);
		FrameLayout frameLayout = (FrameLayout)findViewById(R.id.FrameLayout1);
		frameLayout.addView(view, 0);
		view.setOnTouchListener(this);
		
		iterSeekBar = (SeekBar)findViewById(R.id.iterSeekBar);
		iterSeekBar.setMax(maxIterationsOld);
		iterSeekBar.setProgress(200);
		iterSeekBar.setOnSeekBarChangeListener(this);
		
		SeekBar gradBar = (SeekBar)findViewById(R.id.gradBar);
		gradBar.setOnSeekBarChangeListener(this);
				
		View tempView = findViewById(R.id.linearLayout);
		renderModeRadioGroup = (RadioGroup)tempView.findViewById(R.id.renderModeRadioGroup);
		renderModeRadioGroup.setOnCheckedChangeListener(this);
		RadioGroup colorRadioGroup = (RadioGroup)tempView.findViewById(R.id.colorRadioGroup);
		colorRadioGroup.setOnCheckedChangeListener(this);
		
		if(savedInstanceState != null) {
			offsetMandelX = savedInstanceState.getDouble(OFFSET_X);
		    offsetMandelY = savedInstanceState.getDouble(OFFSET_Y);
		    setOffsetMandelUniforms(offsetFinalX, offsetFinalY);

		    scale = savedInstanceState.getFloat(SCALE);
		    setScaleMandelUniform(scale);
		    
		    splitterFloatUniform.set(savedInstanceState.getFloat(SPLITTER));
		    maxInterationsUniform.set(savedInstanceState.getFloat(MAX_ITER));
		    
		    gradUniform.set(savedInstanceState.getFloat("grad"));
		    
		    renderMode = (RenderMode) savedInstanceState.getSerializable(RENDER_MODE);
		    colorShader = savedInstanceState.getInt(COLOR_MODE);
		    renderMosaic = savedInstanceState.getBoolean(RENDER_MOSAIC);
		    
		    int hudVisibility = savedInstanceState.getInt(HUD);
		    View hudView = findViewById(R.id.HUD);
		    View showButton = findViewById(R.id.showButton);
		    if(hudView != null) {
		    	hudView.setVisibility(hudVisibility);
		    	if(hudVisibility == View.VISIBLE)
		    		showButton.setVisibility(View.GONE);
		    	else
		    		showButton.setVisibility(View.VISIBLE);
		    }
		    
		}
		else {
			//findViewById(R.id.HUD).setVisibility(View.GONE);
			/*findViewById(R.id.linearLayout).findViewById(
					R.id.renderModeRadioGroup).setVisibility(
							View.GONE);*/
		}
		findViewById(R.id.linearLayout).findViewById(
				R.id.renderModeRadioGroup).setVisibility(
						View.GONE);
		findViewById(R.id.linearLayout).findViewById(
				R.id.colorRadioGroup).setVisibility(
						View.GONE);
		findViewById(R.id.linearLayout).findViewById(
				R.id.gradSettings).setVisibility(
						View.GONE);
		
		posTextView = (TextView) findViewById(R.id.posTextView);
		setPosInfo(offsetMandelX, offsetMandelY, scaleMandelX, scaleMandelY);
		
		loadShaders(colorShader);
		
		markMaterial = new Material(ShaderLoader.loadShaderProgram(
				R.raw.simple_v, R.raw.white_f));
		
		setRenderMode(renderMode);
	}

	private void setMaxIterations(int maxIterations) {
		iterSeekBar.setMax(maxIterations);
		float[] values = maxInterationsUniform.getFloats();
		if(values[0] > maxIterations) {
			values[0] = maxIterations;
			maxInterationsUniform.set(values[0]);
		}
	}
	
	private void loadShaders(int colorShader) {
		mandelShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_v, R.raw.mandel_f,
				null, new int[]{R.raw.packfloat, R.raw.hsv2rgb, colorShader});
		mandel64ShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel64_v, R.raw.mandel64_f,
				null, new int[]{R.raw.packfloat, R.raw.hsv2rgb, colorShader});
		mandel64ExpShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel64exp_v, R.raw.mandel64exp_f,
				null, new int[]{R.raw.packfloat, R.raw.doubleemulation, R.raw.hsv2rgb, colorShader});
		mandelInVertexShader = ShaderLoader.loadShaderProgram(R.raw.mandel_in_vertex_v, R.raw.mandel_in_vertex_f,
				new int[]{R.raw.packfloat, R.raw.hsv2rgb, colorShader}, null);
		mandel64InVertexShader = ShaderLoader.loadShaderProgram(R.raw.mandel64exp_in_vert_v, R.raw.mandel64exp_in_vert_f,
				new int[]{R.raw.doubleemulation, R.raw.hsv2rgb, colorShader}, null);
		mandelFloatShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_float_v, R.raw.mandel_float_f,
				null, null);
		mandelFloatEndShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_float_v, R.raw.mandel_float_end_f,
				null, new int[]{R.raw.hsv2rgb, colorShader});
		mandelFloat64ShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_float64_v, R.raw.mandel_float64_f,
				null, new int[]{R.raw.doubleemulation});
		mandelFloat64EndShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_float64_v, R.raw.mandel_float64_end_f,
				null, new int[]{R.raw.doubleemulation, R.raw.hsv2rgb, colorShader});
		
		finalShaderProg = ShaderLoader.loadShaderProgram(R.raw.final_v, R.raw.final_f);
	}

	private void setRenderMode(RenderMode renderMode) {
		switch(renderMode) {
		case SINGLE:
			mandelMaterial.setShaderProgram(mandelShaderProg);
			break;
		case EMULATED_DOUBLE:
			mandelMaterial.setShaderProgram(mandel64ShaderProg);
			break;
		case EXP_EMULATED_DOUBLE:
			mandelMaterial.setShaderProgram(mandel64ExpShaderProg);
			break;
		case SINGLE_IN_VERTEX:
			mandelMaterial.setShaderProgram(mandelInVertexShader);
			break;
		case EXP_EMULATED_DOUBLE_IN_VERTEX:
			mandelMaterial.setShaderProgram(mandel64InVertexShader);
			break;
		case FLOAT_TEXTURE:
			((QuadRenderPass)mandelFloatPass.getRenderPasses().get(2)).setNumBuffers(1);
			mandelMaterial.setShaderProgram(mandelFloatShaderProg);
			mandelFloatEndMaterial.setShaderProgram(mandelFloatEndShaderProg);
			break;
		case FLOAT_TEXTURE_EMULATED_DOUBLE:
			((QuadRenderPass)mandelFloatPass.getRenderPasses().get(2)).setNumBuffers(2);
			mandelMaterial.setShaderProgram(mandelFloat64ShaderProg);
			mandelMaterial.getTextures().clear();
			mandelFloatEndMaterial.setShaderProgram(mandelFloat64EndShaderProg);
			break;
		default:
			mandelMaterial.setShaderProgram(mandelShaderProg);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putDouble(OFFSET_X, offsetMandelX);
		savedInstanceState.putDouble(OFFSET_Y, offsetMandelY);
		
		float s;
		if(asp > 1)
			s = scaleMandelY;
		else
			s = scaleMandelX;
		savedInstanceState.putFloat(SCALE, s);
		
		savedInstanceState.putFloat(SPLITTER, splitterFloatUniform.getFloats()[0]);
		savedInstanceState.putFloat(MAX_ITER, maxInterationsUniform.getFloats()[0]);
		
		savedInstanceState.putFloat("grad", gradUniform.getFloats()[0]);
		savedInstanceState.putFloat("col1", col1Uniform.getFloats()[0]);
		savedInstanceState.putFloat("col2", col2Uniform.getFloats()[0]);
		
		savedInstanceState.putSerializable(RENDER_MODE, renderMode);
		savedInstanceState.putInt(COLOR_MODE, colorShader);
		savedInstanceState.putBoolean(RENDER_MOSAIC, renderMosaic);
		
		savedInstanceState.putInt(HUD, findViewById(R.id.HUD).getVisibility());
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		if(mandelPass != null)
			mandelPass.setSilent(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mandel, menu);
		return true;
	}

	/*@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == R.id.showHUD) {
			if(!item.isChecked()) {
				findViewById(R.id.HUD).setVisibility(View.VISIBLE);
				item.setChecked(true);
			}
			else {
				findViewById(R.id.HUD).setVisibility(View.GONE);
				item.setChecked(false);
			}
		}
		if(id == R.id.chooseMaxIter) {
			maxIterDialog = new AlertDialog.Builder(this);
			maxIterDialog.setTitle("Maximum iteration");
			maxIterDialog.setMessage("Set a value");
			// Set an EditText view to get user input 
			final NumberPicker input = new NumberPicker(this);
			input.setMinValue(0);
			input.setMaxValue(2<<12);
			input.setValue((int)maxInterationsUniform.getFloats()[0]);
			maxIterDialog.setView(input);
			maxIterDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				  	int value = input.getValue();
				  	maxInterationsUniform.set((float)value);
				  	resetVertexMosaic();
				  	mandelPass.setSilent(false);
				}
			});
			maxIterDialog.show();
		}
		if(id == R.id.chooseSplitter) {
			
			splitFloatDialog = new AlertDialog.Builder(this);
			splitFloatDialog.setTitle("Float splitter");
			splitFloatDialog.setMessage("Set a value");
			// Set an EditText view to get user input 
			final NumberPicker input2 = new NumberPicker(this);
			input2.setMinValue(0);
			input2.setMaxValue(1<<23 + 1);
			input2.setValue((int)splitterFloatUniform.getFloats()[0]);
			splitFloatDialog.setView(input2);
			splitFloatDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				  	int value = input2.getValue();
				  	splitterFloatUniform.set((float)value);
				  	resetVertexMosaic();
				  	mandelPass.setSilent(false);
				}
			});
			splitFloatDialog.show();
		}
		return super.onOptionsItemSelected(item);
	}*/

	@Override
	public void onSurfaceChanged(int w, int h) {

		// TODO: This runs on GL thread but should run on main thread !


		if(w == 0 || h == 0) return;
		
		// This will be done first after the view receives it's size in the layout.

		//Pause rendering while we do this
		synchronized (model.getRenderPasses()) {
			model.getRenderPasses().clear();
			// The render passes will be reinitialized at the end of this method
		}
		
		setScaleMandelUniform(scale);
		setOffsetMandelUniforms(offsetMandelX, offsetMandelY);

		hasFloatBuffers = view.getRenderer().hasFloatBuffers();

		// Only use float buffers in combination with OpenGL ES 3
		hasFloatBuffers = hasFloatBuffers && glVersion >= 0x30000;

		if(hasFloatBuffers) {
			setMaxIterations(maxIterations);
			iterSeekBar.setProgress(32 * 1024);
			maxInterationsUniform.set(32 * 1024);

			Texture[] mandelFloatTextures = {
					new Texture(null, Texture.TextureType.TEXTURE_2D,
							w, h, Texture.TexelType.FLOAT, Texture.Format.GL_RGBA32F,
							Texture.Filter.NEAREST, Texture.WrapMode.CLAMP_TO_EDGE),
					new Texture(null, Texture.TextureType.TEXTURE_2D,
							w, h, Texture.TexelType.FLOAT, Texture.Format.GL_R32F,
							Texture.Filter.NEAREST, Texture.WrapMode.CLAMP_TO_EDGE)};
			mandelFloatFBO = new FBO(mandelFloatTextures, w, h);

			Texture[] mandelFloatPongTextures = {
					new Texture(null, Texture.TextureType.TEXTURE_2D,
							w, h, Texture.TexelType.FLOAT, Texture.Format.GL_RGBA32F,
							Texture.Filter.NEAREST, Texture.WrapMode.CLAMP_TO_EDGE),
					new Texture(null, Texture.TextureType.TEXTURE_2D,
							w, h, Texture.TexelType.FLOAT, Texture.Format.GL_R32F,
							Texture.Filter.NEAREST, Texture.WrapMode.CLAMP_TO_EDGE)};
			mandelFloatPongFBO = new FBO(mandelFloatPongTextures, w, h);

			setupFloatPass(false);
		}
		else {
			// Float buffers not supported
			findViewById(R.id.radio32F).setEnabled(false);
			findViewById(R.id.radio64F).setEnabled(false);
		}


		Texture mandelTexture = new Texture(null, Texture.TextureType.TEXTURE_2D,
				w, h, Texture.TexelType.UBYTE, Texture.Format.GL_RGB, Texture.Filter.NEAREST,
				Texture.WrapMode.CLAMP_TO_EDGE);
		// TODO: Are we leaking video memory here when we loose the old FBO?
		mandelFBO = new FBO(mandelTexture, w, h);
		
		// This pass can copy mandelFBO2 back to mandelFBO
		Texture mandelTexture2 = new Texture(null, Texture.TextureType.TEXTURE_2D, view.getWidth(),
				view.getHeight(), Texture.TexelType.UBYTE, Texture.Format.GL_RGB, Texture.Filter.NEAREST,
				Texture.WrapMode.CLAMP_TO_EDGE);
		mandelFBO2 = new FBO(mandelTexture2, w, h);
		directShaderProg = ShaderLoader.loadShaderProgram(R.raw.direct_v, R.raw.direct_f);
		Material directMaterial = new Material(directShaderProg);
		directMaterial.addTexture("tex", mandelTexture2);
		mandelFbo2toFboPass = new QuadRenderPass(directMaterial, mandelFBO);
		
		// Initialize the pixelMesh
		Node vertexRootNode = new Node();
		mandelVertexPass.setRootNode(vertexRootNode);
		mandelVertexPass.setOneTime(true);
		
		final int maxShort = 256*256-1;
		int fullH = h;
		
		// Limit the indices to the size of short
		if(w*h > maxShort) {
			int r = w % maxShort;
			h = (maxShort - r) / w;
		}
		
		// TODO: Use just one piece repeatedly
		// How many short sized pieces do we need?
		int parts = (int)Math.ceil((float)fullH/h);
		
		
		// Generate a pixel mesh of size h*w and the resolution that corresponds
		// a full size view of w*fullH points
		Geometry fullShortPixelMesh = generatePixelMeshGeometry(w, h, w, fullH);
		
		// Add multiple pieces
		for(int p = 0; p < parts; p++) {
			GeometryNode pixelGeometryNode = new GeometryNode(fullShortPixelMesh, mandelMaterial);
			pixelGeometryNode.getTransform().getPosition().setY(2*p/(float)parts);
			vertexRootNode.attach(pixelGeometryNode);
		}
		
		// Create the vertex mosaic pass
		Node vertexMosaicRootNode = new Node();
		mandelVertexMosaicPass.setRootNode(vertexMosaicRootNode);
		setUpVertexMosaic(vertexMosaicRootNode, view.getWidth(), view.getHeight(), 32);
		
		finalMaterial = new Material(finalShaderProg);
		finalMaterial.addTexture("tex", mandelTexture);
		finalMaterial.addUniform(offsetFinalUniform);
		finalMaterial.addUniform(scaleFinalUniform);
		
		// Preload some stuff so it doesn't slow down the app later
		Log.i(TAG, "Loading vertex geometry...");
		view.requestPreload(vertexRootNode);
		Log.i(TAG, "Loading vertex mosaic geometry...");
		view.requestPreload(vertexMosaicRootNode);
		Log.i(TAG, "Loading shaders...");
		view.requestPreload(mandelShaderProg);
		view.requestPreload(mandel64ShaderProg);
		view.requestPreload(mandel64ExpShaderProg);
		view.requestPreload(mandelInVertexShader);
		view.requestPreload(mandel64InVertexShader);
		view.requestPreload(finalMaterial);
		view.requestPreload(directShaderProg);
		view.requestPreload(mandelFBO);
		view.requestPreload(mandelFBO2);
		Log.i(TAG, "Loading finished.");
		
		setAutoMode();
		
		setRenderPasses();
	}
	
	private void setUpVertexMosaic(Node rootNode, int width,
			int height, int partSize) {
		rootNode.getChildren().clear();
		Log.d(TAG, "Creating vertex mosaic...");
		
		Geometry part = generatePixelMeshGeometry(partSize, partSize, width, height);
		
		int partsX = (int) Math.ceil(width/(float)partSize);
		int partsY = (int) Math.ceil(height/(float)partSize);
		
		for(int j = 0; j < partsY; j++) {
			for(int i = 0; i < partsX; i++) {
				GeometryNode gNode = new GeometryNode(part, mandelMaterial);
				gNode.setVisible(false);
				
				float posX = 2*i/(float)partsX;
				float posY = 2*j/(float)partsY;
				
				gNode.getTransform().getPosition().set(posX, posY, 0);
				
				rootNode.attach(gNode);
			}
		}
		
		// Make a single GeometryNode visible too bootstrap the piecewise drawing
		resetVertexMosaic();
		
		Log.d(TAG, "Finished vertex mosaic.");
	}
	
	private void resetVertexMosaic() {
		if(mandelVertexMosaicPass != null) {
			tileCount = -1;
			
			/*Node rootNode = mandelVertexMosaicPass.getRootNode();
			if(rootNode != null) {
				Set<Node> children = rootNode.getChildren();
				if(!children.isEmpty() && children.toArray()[0] instanceof GeometryNode)
					((GeometryNode)children.toArray()[0]).setVisible(true);
			}*/
		}
	}

	private Geometry generatePixelMeshGeometry(int w, int h, int fullW, int fullH) {
		ShortBuffer indices = ShortBuffer.allocate(w*h);
		Geometry.Attribute posAt = new Geometry.Attribute();
		posAt.name = "position";
		posAt.size = 3;
		posAt.type = Geometry.Type.FLOAT;
		posAt.buffer = FloatBuffer.allocate(3*w*h);
		
		for(int i = 0; i < h; i++) {
			for(int j = 0; j < w; j++) {
				((FloatBuffer) posAt.buffer).put(-1 + (2*j + 1)/(float)fullW);
				((FloatBuffer) posAt.buffer).put(-1 + (2*i + 1)/(float)fullH);
				((FloatBuffer) posAt.buffer).put(0);
				
				indices.put((short)(i*w+j));
			}
		}
		posAt.buffer.flip();
		indices.flip();
		
		List<Geometry.Attribute> lat = new ArrayList<Geometry.Attribute>();
		lat.add(posAt);
		return new Geometry(Geometry.PrimitiveType.POINTS, indices, lat);
	}

	private void setRenderPasses() {
		synchronized (model.getRenderPasses()) {
			model.getRenderPasses().clear();
			clearPass = new ClearPass(GLES20.GL_COLOR_BUFFER_BIT, null);
			model.addRenderPass(clearPass);
			mandelMaterial.getTextures().clear();
			setCorrectMandelPass();
			mandelVertexPass.setFbo(mandelFBO);
			mandelVertexMosaicPass.setOnRenderPassFinishedListener(this);
			mandelVertexMosaicPass.setFbo(mandelFBO);
			mandelPass.setOneTime(true);
			mandelPass.setSilent(false);
			List<RenderPass> mandelFloatInternalPasses = mandelFloatPass.getRenderPasses();
			mandelFloatInternalPasses.get(0).setFbo(mandelFloatPongFBO);
			mandelFloatInternalPasses.get(1).setFbo(mandelFloatFBO);
			mandelFloatInternalPasses.get(0).setSilent(false);
			mandelFloatInternalPasses.get(1).setSilent(false);
			mandelFloatInternalPasses.get(mandelFloatInternalPasses.size() - 1).setFbo(mandelFBO);
			mandelFloatPass.setOnRenderPassFinishedListener(this);
			currentFloatIterationUniform.set(0, (float) FLOAT_DRAW_STEPS);
			model.addRenderPass(mandelPass);
			finalPass = new QuadRenderPass(finalMaterial);
			model.addRenderPass(finalPass);
			CheckGlErrorPass errorPass = new CheckGlErrorPass(true);
			model.addRenderPass(errorPass);
			errorPass.setOnGlErrorListener(this);
		}
	}

	private void setupFloatPass(boolean pong) {
		if(mandelFloatFBO == null || mandelFloatPongFBO == null)
			return;
		mandelFloatPass.getRenderPasses().get(2).setFbo(pong ? mandelFloatPongFBO : mandelFloatFBO);
		mandelMaterial.getTextures().clear();
		mandelMaterial.addTexture("tex",
				(Texture) (pong ? mandelFloatFBO.getAttachables()[0] : mandelFloatPongFBO.getAttachables()[0]));
		mandelMaterial.addTexture("counterTex",
				(Texture) (pong ? mandelFloatFBO.getAttachables()[1] : mandelFloatPongFBO.getAttachables()[1]));
		mandelFloatEndMaterial.getTextures().clear();
		mandelFloatEndMaterial.addTexture("tex",
				(Texture) (pong ? mandelFloatPongFBO.getAttachables()[0] : mandelFloatFBO.getAttachables()[0]));
		mandelFloatEndMaterial.addTexture("counterTex",
				(Texture) (pong ? mandelFloatPongFBO.getAttachables()[1] : mandelFloatFBO.getAttachables()[1]));
	}
	
	private void setCorrectMandelPass() {
		if(renderMode == RenderMode.FLOAT_TEXTURE || renderMode == RenderMode.FLOAT_TEXTURE_EMULATED_DOUBLE) {
			mandelPass = mandelFloatPass;
			currentFloatIterationUniform.set(0, (float) FLOAT_DRAW_STEPS);
			mandelFloatPass.getRenderPasses().get(0).setFbo(mandelFloatPongFBO);
			mandelFloatPass.getRenderPasses().get(1).setFbo(mandelFloatFBO);
			mandelFloatPass.getRenderPasses().get(0).setSilent(false);
			mandelFloatPass.getRenderPasses().get(1).setSilent(false);
			setupFloatPass(false);
		}
		else if(renderMode == RenderMode.SINGLE_IN_VERTEX ||
				renderMode == RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX) {
			if(renderMosaic) {
				mandelPass = mandelVertexMosaicPass;
			}
			else
				mandelPass = mandelVertexPass;
		}
		else
			mandelPass = new QuadRenderPass(mandelMaterial, mandelFBO);
	}
	
	private void setOffsetMandelUniforms(double x, double y) {
		offsetMandelUniform.set((float)x, (float)y);
		offsetFineMandelUniform.set((float)(x-(float)x), (float)(y-(float)y));
	}

	private void setScaleMandelUniform(float s) {
		float vert = view.getHeight();
		float hori = view.getWidth();
		asp = hori / vert;
		if( asp > 1 ) {
			scaleMandelY = s;
			scaleMandelX = s*asp;
			
		} else {
			scaleMandelY = s/asp;
			scaleMandelX = s;
		}
		
		scaleMandelUniform.set(scaleMandelX, scaleMandelY);
	}

	@Override
	public void onGlError(int error) {
		if(error == GLES20.GL_OUT_OF_MEMORY) {
			RadioButton rb = (RadioButton)findViewById(R.id.radioA);
			if (rb != null) {
				if(rb.isChecked() && !renderMosaic) {
					renderMosaicToast.show();
					renderMosaic = true;
					fullRedraw();
				}
				else
					outOfMemToast.show();
			}
		}
		else {
			// Have we tried usign the dyn loop fallback yet?
			if(!useFallbackInsteadOfDynLoop) {
				useFallbackInsteadOfDynLoop = true;
				// Redo everything
				onSurfaceChanged(view.getWidth(), view.getHeight());
			}
			else
				otherGlErrorToast.show();
		}
	}
	
	private void setPosInfo(double x, double y, double scaleX, double scaleY) {
		double s;
		if(scaleX < scaleY)
			s = scaleX;
		else
			s = scaleY;
		
		String scaleString = String.valueOf(/*1/*/s);
		posTextView.setText("Position: " + 2*x + " + " + 
				2*y + "i\nScale: " + scaleString/* + "x"*/);
	}
	
	/*@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == R.id.showHUD) {
			if(!item.isChecked()) {
				findViewById(R.id.HUD).setVisibility(View.VISIBLE);
				item.setChecked(true);
			}
			else {
				findViewById(R.id.HUD).setVisibility(View.GONE);
				item.setChecked(false);
			}
		}
		if(id == R.id.chooseMaxIter) {
			maxIterDialog = new AlertDialog.Builder(this);
			maxIterDialog.setTitle("Maximum iteration");
			maxIterDialog.setMessage("Set a value");
			// Set an EditText view to get user input 
			final NumberPicker input = new NumberPicker(this);
			input.setMinValue(0);
			input.setMaxValue(2<<12);
			input.setValue((int)maxInterationsUniform.getFloats()[0]);
			maxIterDialog.setView(input);
			maxIterDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				  	int value = input.getValue();
				  	maxInterationsUniform.set((float)value);
				  	resetVertexMosaic();
				  	mandelPass.setSilent(false);
				}
			});
			maxIterDialog.show();
		}
		if(id == R.id.chooseSplitter) {
			
			splitFloatDialog = new AlertDialog.Builder(this);
			splitFloatDialog.setTitle("Float splitter");
			splitFloatDialog.setMessage("Set a value");
			// Set an EditText view to get user input 
			final NumberPicker input2 = new NumberPicker(this);
			input2.setMinValue(0);
			input2.setMaxValue(1<<23 + 1);
			input2.setValue((int)splitterFloatUniform.getFloats()[0]);
			splitFloatDialog.setView(input2);
			splitFloatDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				  	int value = input2.getValue();
				  	splitterFloatUniform.set((float)value);
				  	resetVertexMosaic();
				  	mandelPass.setSilent(false);
				}
			});
			splitFloatDialog.show();
		}
		return super.onOptionsItemSelected(item);
	}*/
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if( v == view) {
			scaleDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			
			if(event.getActionMasked() == MotionEvent.ACTION_UP)
				if(event.getPointerCount() == 1) {
					// Probably last touch, so redraw the set
					
					drawFinalToFboOnce();
					
					synchronized (model.getRenderPasses()) {
						
						// Put all the transforms in the mandel redraw
						offsetMandelX += offsetFinalX*scaleFinalX*scaleMandelX;
						offsetMandelY += offsetFinalY*scaleFinalY*scaleMandelY;
						scaleMandelX *= scaleFinalX;
						scaleMandelY *= scaleFinalY;
						
						// and reset the final transform
						scaleFinalX = 1f;
						scaleFinalY = 1f;
						offsetFinalX = 0f;
						offsetFinalY = 0f;
						
						scaleMandelUniform.set(scaleMandelX, scaleMandelY);
						setOffsetMandelUniforms(offsetMandelX, offsetMandelY);
						
						scaleFinalUniform.set(scaleFinalX, scaleFinalY);
						offsetFinalUniform.set(offsetFinalX, offsetFinalY);
						
						// Reactivate final drawing and run the mandel pass once
						resetVertexMosaic();
						setAutoMode();
						mandelPass.setSilent(false);
					}
					
					setPosInfo(offsetMandelX, offsetMandelY, scaleMandelX, scaleMandelY);
				}
			
			return true;
		}
		
		return false;
	}

	private void drawFinalToFboOnce() {
		// TODO: Many race conditions here that could lock the code
		
		// Draw the scaled final image into the fbo once
		FBO clearFBO = clearPass.getFbo();
		FBO finalFBO = finalPass.getFbo();
		SetListener waitForIt = new SetListener(null);
		synchronized (model.getRenderPasses()) {
			finalPass.setOnRenderPassFinishedListener(new SetListener(finalFBO, waitForIt));
		}
		while(finalPass.getOnRenderPassFinishedListener() != waitForIt) Thread.yield();
		finalPass.setOnRenderPassFinishedListener(null);
		synchronized (model.getRenderPasses()) {
			clearFBO = clearPass.getFbo();
			finalFBO = finalPass.getFbo();
			
			clearPass.setFbo(mandelFBO2);
			finalPass.setFbo(mandelFBO2);
			clearPass.setOnRenderPassFinishedListener(new SetListener(clearFBO));
			finalPass.setOnRenderPassFinishedListener(new SetListener(finalFBO, waitForIt));
		}
		while(finalPass.getOnRenderPassFinishedListener() != waitForIt) Thread.yield();
		finalPass.setOnRenderPassFinishedListener(null);
		synchronized (model.getRenderPasses()) {
			List<RenderPass> renderPasses = model.getRenderPasses();
			renderPasses.clear();
			mandelFbo2toFboPass.setFbo(mandelFBO);
			mandelFbo2toFboPass.setOnRenderPassFinishedListener(new SetListener(mandelFBO2));
			// This was probably causing the occasional black fill bug
			//renderPasses.add(new ClearPass(GLES20.GL_COLOR_BUFFER_BIT, mandelFBO));
			renderPasses.add(mandelFbo2toFboPass);
		}
		while(mandelFbo2toFboPass.getFbo() != mandelFBO2) Thread.yield();
		setRenderPasses();
	}

	private class SetListener implements OnRenderPassFinishedListener {
		private FBO fbo;
		private SetListener listener;
		
		public SetListener(FBO fbo) {
			this(fbo, null);
		}
		
		public SetListener(FBO fbo, SetListener listener) {
			this.fbo = fbo;
			if(listener != this)
				this.listener = listener;
			else
				this.listener = null;
		}
		
		@Override
		public void onRenderPassFinished(
				RenderPass pass) {
			pass.setFbo(fbo);
			if(listener != null)
				pass.setOnRenderPassFinishedListener(listener);
		}
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		int id = arg0.getId();
		if (id == R.id.iterSeekBar) {
			TextView iterText = (TextView)findViewById(R.id.iterTextView);
			iterText.setText("Iterations: " + arg0.getProgress());
			
			// If in a simple mode change the fractal live
			/*if(renderMode == RenderMode.SINGLE ||
					renderMode == RenderMode.SINGLE_IN_VERTEX) {
				maxInterationsUniform.set(arg0.getProgress());
				mandelPass.setSilent(false);
			}*/
		} else if (id == R.id.gradBar) {
			TextView gradText = (TextView)findViewById(R.id.gradText);
			gradText.setText("Gradient: " + gradFunc(arg0.getProgress()));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		if(arg0.getId() == R.id.iterSeekBar) {
			maxInterationsUniform.set(arg0.getProgress());
			fullRedraw();
		}
		else if(arg0.getId() == R.id.gradBar) {
			gradUniform.set(gradFunc(arg0.getProgress()));
			fullRedraw();
		}
	}

	private float gradFunc(float g) {
		return 0.1f  +  g/50f;
	}

	private int gradFuncInv(float x) {
		return (int) ((x-0.1f)*50f);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if(group.getId() == R.id.renderModeRadioGroup) {
			
			if(checkedId != R.id.radioA)
				((RadioButton)findViewById(R.id.radioA)).setText("Auto");

			if (checkedId == R.id.radioA) {
				setMaxIterations(hasFloatBuffers ? maxIterations : maxIterationsOld);
				setAutoMode();
				fullRedraw();
			} else if (checkedId == R.id.radioS) {
				setLegacyRenderMode(RenderMode.SINGLE);
			} else if (checkedId == R.id.radioED) {
				setLegacyRenderMode(RenderMode.EMULATED_DOUBLE);
			} else if (checkedId == R.id.radioExpED) {
				setLegacyRenderMode(RenderMode.EXP_EMULATED_DOUBLE);
			} else if (checkedId == R.id.radioSV) {
				setLegacyRenderMode(RenderMode.SINGLE_IN_VERTEX);
			} else if (checkedId == R.id.radioEDV) {/*if(allowDouble[0] == -1) {
					allowDouble[0] = 0;
					showWarning();
					break;
				}*/
				setLegacyRenderMode(RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX);
			} else if (checkedId == R.id.radio32F) {
				setMaxIterations(maxIterations);
				renderMode = RenderMode.FLOAT_TEXTURE;
				fullRedraw();
			} else if (checkedId == R.id.radio64F) {
				setMaxIterations(maxIterations);
				renderMode = RenderMode.FLOAT_TEXTURE_EMULATED_DOUBLE;
				fullRedraw();
			} else {
				setLegacyRenderMode(RenderMode.SINGLE);
			}

		} else if(group.getId() == R.id.colorRadioGroup) {

			if (checkedId == R.id.huecircleRadio) {
				colorShader = R.raw.hsvcolor;
			} else if (checkedId == R.id.blueyellowRadio) {
				colorShader = R.raw.blueyellowcolor;
			} else if (checkedId == R.id.pastelhsvRadio) {
				colorShader = R.raw.pastelhsvcolor;
			} else if (checkedId == R.id.gradientRadio) {
				colorShader = R.raw.gradientcolor;
			} else {
				colorShader = R.raw.pastelhsvcolor;
			}
			
			loadShaders(colorShader);
			fullRedraw();
		}
	}

	private void setLegacyRenderMode(RenderMode renderMode) {
		if(allowLegacy[0] == -2) {
			// ignore for radio set check bug work around
		}
		else if(allowLegacy[0] != 1 && (this.renderMode == RenderMode.FLOAT_TEXTURE || this.renderMode == RenderMode.FLOAT_TEXTURE_EMULATED_DOUBLE)) {
			showLegacyWarning(renderMode);
		}
		else {
			this.renderMode = renderMode;
			setMaxIterations(maxIterationsOld);
			this.renderMode = renderMode;
			setRenderMode(renderMode);
			fullRedraw();
		}
	}

	private void fullRedraw() {
		synchronized (model.getRenderPasses()) {
			resetVertexMosaic();
			setRenderMode(renderMode);
			setRenderPasses();
			mandelPass.setSilent(false);
		}
	}

	public void toggleModeSettings(View view) {
		View group = findViewById(R.id.renderModeRadioGroup);
		int vis = group.getVisibility();
		if(vis == View.VISIBLE)
			vis = View.GONE;
		else
			vis = View.VISIBLE;
		group.setVisibility(vis);
	}

	public void toggleColorSettings(View view) {
		View group = findViewById(R.id.colorRadioGroup);
		int vis = group.getVisibility();
		if(vis == View.VISIBLE)
			vis = View.GONE;
		else
			vis = View.VISIBLE;
		group.setVisibility(vis);
	}

	public void toggleGradSettings(View view) {
		View group = findViewById(R.id.gradSettings);
		int vis = group.getVisibility();
		if(vis == View.VISIBLE)
			vis = View.GONE;
		else
			vis = View.VISIBLE;
		group.setVisibility(vis);
	}

	public void resetPos(View view) {
		offsetMandelX = -0.75f/2;
		offsetMandelY = 0;
		scale = 1;
		
		allowDouble[0] = -1; // ask again
		
		setOffsetMandelUniforms(offsetMandelX, offsetMandelY);
		setScaleMandelUniform(scale);
		renderMode = RenderMode.SINGLE;
		setRenderMode(renderMode);
		maxInterationsUniform.set(hasFloatBuffers ? 32 * 1024 : 200);
		((SeekBar)findViewById(R.id.iterSeekBar)).setProgress(
				(int) maxInterationsUniform.getFloats()[0]);
		
		findViewById(R.id.renderModeRadioGroup).setVisibility(View.GONE);
		((RadioButton)findViewById(R.id.radioA)).setChecked(true);
		setAutoMode();
		((CheckBox)findViewById(R.id.mosaicBox)).setChecked(false);
		renderMosaic = false;
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		findViewById(R.id.colorRadioGroup).setVisibility(View.GONE);
		((RadioButton)findViewById(R.id.pastelhsvRadio)).setChecked(true);
		colorShader = R.raw.pastelhsvcolor;
		
		gradUniform.set(1f);
		((SeekBar)findViewById(R.id.gradBar)).setProgress(
				(int) gradFuncInv(gradUniform.getFloats()[0]));
		
		setRenderPasses();
		setPosInfo(offsetMandelX, offsetMandelY, scaleMandelX, scaleMandelY);
		resetVertexMosaic();
		mandelPass.setSilent(false);
	}

	public void setRenderMosaic(View view) {
		renderMosaic = ((CheckBox)view).isChecked();
		if(!renderMosaic) findViewById(R.id.progressBar).setVisibility(View.GONE);
		setRenderPasses();
	}

	public void showHUD(View view) {
		findViewById(R.id.showButton).setVisibility(View.GONE);
		findViewById(R.id.HUD).setVisibility(View.VISIBLE);
	}

	public void hideHUD(View view) {
		findViewById(R.id.HUD).setVisibility(View.GONE);
		findViewById(R.id.showButton).setVisibility(View.VISIBLE);
	}

	public void saveImage(View view) {
		screenshotBitmap = null;
		synchronized (model.getRenderPasses()) {
			model.getRenderPasses().add(new ScreenshotPass());
		}
		Calendar cal = Calendar.getInstance();
		long time = cal.getTimeInMillis();
		
		while (screenshotBitmap == null && time < cal.getTimeInMillis() + 5000)
			Thread.yield();
		
		if(screenshotBitmap == null)
			Utils.displayFileError(this);
		else {
			Utils.saveBitmapToFile(this, screenshotBitmap);
			screenshotBitmap.recycle();
			screenshotBitmap = null;
		}
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
		    float factor = detector.getScaleFactor();
		    scaleFinalX /= factor;
		    scaleFinalY /= factor;
		    scale /= factor;
		    offsetFinalX *= factor;
			offsetFinalY *= factor;
		    scaleFinalUniform.set(scaleFinalX, scaleFinalY);
		    offsetFinalUniform.set(offsetFinalX, offsetFinalY);
		
		    //invalidate();
		    return true;
		}
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			offsetFinalX += 2*distanceX/view.getWidth();
			offsetFinalY -= 2*distanceY/view.getHeight();
			offsetFinalUniform.set(offsetFinalX, offsetFinalY);
			
			return true;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			
			// Only do this if render mosaic is used
			if(renderMosaic && (renderMode == RenderMode.SINGLE_IN_VERTEX
					|| renderMode == RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX)) {
				float oldOffsetFinalX = offsetFinalX;
				float oldOffsetFinalY = offsetFinalY;
				float oldScaleFinalX = scaleFinalX;
				float oldScaleFinalY = scaleFinalY;
				
				// Temporarily change final
				offsetFinalX += 2*scaleFinalX*(e.getX()/view.getWidth()*2 -1);
				offsetFinalY -= 2*scaleFinalY*(e.getY()/view.getHeight()*2 -1);
				scaleFinalX /= 2;
				scaleFinalY /= 2;
				scaleFinalUniform.set(scaleFinalX, scaleFinalY);
				offsetFinalUniform.set(offsetFinalX, offsetFinalY);
				
				drawFinalToFboOnce();
				
				// Reset final
				offsetFinalX = oldOffsetFinalX;
				offsetFinalY = oldOffsetFinalY;
				scaleFinalX = oldScaleFinalX;
				scaleFinalY = oldScaleFinalY;
				offsetFinalUniform.set(offsetFinalX, offsetFinalY);
				scaleFinalUniform.set(scaleFinalX, scaleFinalY);
			}
			
			// Do the changes to mandel instead of final
			offsetMandelX += scaleMandelX*(e.getX()/view.getWidth()*2 -1);
			offsetMandelY -= scaleMandelY*(e.getY()/view.getHeight()*2 -1);
			scaleMandelX /= 2;
			scaleMandelY /= 2;
			scale /= 2;
			scaleMandelUniform.set(scaleMandelX, scaleMandelY);
						
			setPosInfo(offsetMandelX, offsetMandelY, scaleMandelX, scaleMandelY);
			
			setAutoMode();
			return true;
		}
		
		
	}

	private void setAutoMode() {
		
		CheckBox renderBox = (CheckBox)findViewById(R.id.mosaicBox);
		if( renderBox.isChecked() != renderMosaic )
			renderBox.setChecked(renderMosaic);
	
		RadioButton radio = (RadioButton)findViewById(R.id.radioA);
		if(radio.isChecked()) {
			double scale = 1d;
			if(scaleMandelX < scaleMandelY) scale = scaleMandelX;
			else scale = scaleMandelY;
			
			double singleLimit = 0.2d;
			double vertexSingleLimit = 0.00003d;
			
			// Use this to account for bigger resolutions
			int w = view.getWidth();
			if(w == 0) return;
			int W = 800;


			if(hasFloatBuffers) {
				if(scale*w/W < vertexSingleLimit) {
					if(renderMode != RenderMode.FLOAT_TEXTURE_EMULATED_DOUBLE) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(Mandel.this, "Auto-switching to emulated 64 bit precision",
										Toast.LENGTH_SHORT).show();
							}
						});
						renderMode = RenderMode.FLOAT_TEXTURE_EMULATED_DOUBLE;
						fullRedraw();
					}
					radio.setText("Auto (Emu 64 bit in OpenGL ES 3)");
				}
				else {
					if(renderMode != RenderMode.FLOAT_TEXTURE) {
						if(renderMode != RenderMode.SINGLE) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(Mandel.this, "Auto-switching to 32 bit precision",
											Toast.LENGTH_SHORT).show();
								}
							});
						}
						renderMode = RenderMode.FLOAT_TEXTURE;
						fullRedraw();
					}
					radio.setText("Auto (32 bit in OpenGL ES 3)");
				}

				return;
			}
			
			// TODO: Doesn't work!! Wrong value from preferences.
			if(!getSharedPreferences("MANDEL", 0).getBoolean("allowDouble", false)) {
				if (scale * w / W < vertexSingleLimit) {
					if (allowDouble[0] == -1) {
						allowDouble[0] = 0;
						showWarning();
					} else if (allowDouble[0] == 0) {
						vertexSingleLimit = 0;
					}
				}
			}
			
			if(scale*w/W < vertexSingleLimit) {
				// We need double precision
				if(renderMode != RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX) {
					Toast.makeText(this, "Auto-switching to emulated double precision in vertex shader",
							Toast.LENGTH_SHORT).show();
					renderMode = RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX;
					if(maxInterationsUniform.getFloats()[0] <= 200) {
						renderMosaic = false;
						renderBox.setChecked(false);
					}
					fullRedraw();
				}
				radio.setText("Auto (Emu Double in Vert)");
			}
			
			else if(scale*w/W < singleLimit) {
				// We need vertex shader precision
				if(renderMode != RenderMode.SINGLE_IN_VERTEX) {
					Toast.makeText(this, "Auto-switching to vertex shader",
							Toast.LENGTH_SHORT).show();
					renderMode = RenderMode.SINGLE_IN_VERTEX;
					if(maxInterationsUniform.getFloats()[0] <= 100) {
						renderMosaic = false;
						renderBox.setChecked(false);
					}
					fullRedraw();
				}
				radio.setText("Auto (In Vertex)");
			}
			
			else {
				// Normal fragment shader precision is enough
				if(renderMode != RenderMode.SINGLE) {
					Toast.makeText(this, "Auto-switching to fragment shader",
							Toast.LENGTH_SHORT).show();
					renderMode = RenderMode.SINGLE;
					renderMosaic = false;
					renderBox.setChecked(false);
					fullRedraw();
				}
				radio.setText("Auto (Single)");
			}
		}
	}

	private int tileCount = -1;
	
	@Override
	public void onRenderPassFinished(RenderPass pass) {
		if(pass == mandelFloatPass) {
			float maxIterations = maxInterationsUniform.getFloats()[0];
			if(currentFloatIterationUniform.getFloats()[0] < maxIterations) {
				float[] iterations = currentFloatIterationUniform.getFloats();
				iterations[0] += FLOAT_DRAW_STEPS;
				iterations[1] = Math.min(FLOAT_DRAW_STEPS, maxIterations - iterations[0]);
				currentFloatIterationUniform.set(iterations[0], iterations[1]);

				boolean pong = mandelFloatPass.getRenderPasses().get(2).getFbo() == mandelFloatPongFBO;
				setupFloatPass(!pong);

				pass.setSilent(false);
			}
		}
		else if(pass == mandelVertexMosaicPass) {
			Object[] parts =
					((SceneRenderPass)pass).getRootNode().getChildren().toArray();
			
			if(tileCount >= parts.length) {
				tileCount = -1;
				resetVertexMosaic();
				doneToast.show();
				return;
			}
			
			for(int i = 0; i < tileCount; i++) {
				((GeometryNode)parts[i]).setVisible(false);
			}
			
			if(tileCount >= 0) {
				((GeometryNode)parts[tileCount]).setVisible(true);
				((GeometryNode)parts[tileCount]).setMaterial(mandelMaterial);
			}
			
			if(tileCount+1 < parts.length) {
				((GeometryNode)parts[tileCount+1]).setVisible(true);
				((GeometryNode)parts[tileCount+1]).setMaterial(markMaterial);
			}
			
			for(int i = tileCount + 2; i < parts.length; i++)
				((GeometryNode)parts[i]).setVisible(false);
			
			tileCount++;
			pass.setSilent(false);
			
			//for(int i = 0)
			
			/*Set<Node> parts = ((SceneRenderPass)pass).getRootNode().getChildren();
			
			Iterator<Node> it = parts.iterator();
			
			// TODO: If unexpected nodes are added result is undefined
			
			int tiles = parts.size();
			if(tiles == 0) return;
			
			int tileCount = 0;
			
			// Find the first visible GeometryNode
			while(it.hasNext()) {
				Node node = it.next();
				if(node instanceof GeometryNode) {
					GeometryNode gNode = (GeometryNode) node;
					tileCount++;
					if(gNode.isVisible()) {
						gNode.setVisible(false);
						break;
					}
				}
			}
			
			/*ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
			if(tileCount < tiles) {
				bar.setVisibility(View.VISIBLE);
				bar.setMax(tiles);
				bar.setProgress(tileCount-1);
			}
			else {
				bar.setProgress(0);
				bar.setVisibility(View.VISIBLE);
			}*/
			
			// If that was the last Node we are finished
			/*if(!it.hasNext()) {
				// Set a GeometryNode to visible to have somewhere to start next time
				resetVertexMosaic();
				doneToast.show();
				return; // The pass stays silent
			}
			
			// Otherwise set the next GeometryNode to visible
			Node node = it.next();
			if(node instanceof GeometryNode)
				((GeometryNode)node).setVisible(true);
			
			// Set the remaining GeometryNodes to not visible
			while(it.hasNext()) {
				Node node2 = it.next();
				if(node2 instanceof GeometryNode)
					((GeometryNode)node2).setVisible(false);
			}
			
			// Since there were more GeometryNodes let's redraw
			pass.setSilent(false);
			*/
		}
	}
	
	public class ScreenshotPass extends RenderPass {
		
		public ScreenshotPass() {
			super();
			this.setOnRenderPassFinishedListener(new ScreenshotRetriever());
			this.setOneTime(true);
		}
		
		public class ScreenshotRetriever implements OnRenderPassFinishedListener {
			@Override
			public void onRenderPassFinished(RenderPass pass) {
				screenshotBitmap = createBitmapFromGLSurface(0, 0, view.getWidth(), view.getHeight());
			}
		}
		
		private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h)
		        throws OutOfMemoryError {
		    int bitmapBuffer[] = new int[w * h];
		    int bitmapSource[] = new int[w * h];
		    IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
		    intBuffer.position(0);

		    try {
		        GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
		        int offset1, offset2;
		        for (int i = 0; i < h; i++) {
		            offset1 = i * w;
		            offset2 = (h - i - 1) * w;
		            for (int j = 0; j < w; j++) {
		                int texturePixel = bitmapBuffer[offset1 + j];
		                int blue = (texturePixel >> 16) & 0xff;
		                int red = (texturePixel << 16) & 0x00ff0000;
		                int pixel = (texturePixel & 0xff00ff00) | red | blue;
		                bitmapSource[offset2 + j] = pixel;
		            }
		        }
		    } catch (GLException e) {
		        return null;
		    }

		    return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
		}
	}
	
	public void showHelp(View view) {
		String versionName = "X";
		try {
			versionName = getPackageManager()
				    .getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Dialog helpDia = new Dialog(this);
		helpDia.setTitle("GPU Mandelbrot v" + versionName);
		helpDia.setContentView(R.layout.help_dialog);
		helpDia.show();
	}
	
	public void showWarning() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String[] checkText = {"Do not ask again"};
		final boolean[] checkSet = {false};
		builder.setTitle("Warning")
			.setMessage("Would you like to try switching to emulated double precision mode?\n\n" +
				"Some GPU drivers can freeze the system in this mode.")
		   .setCancelable(false)
		   .setMultiChoiceItems(checkText, checkSet, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					checkSet[0] = isChecked;
				}
			})
		   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		       public void onClick(DialogInterface dialog, int id) {
		            allowDouble[0] = 1;
		            setRenderMode(RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX);
		            
		            //if(checkSet[0])
		            	rememberDoubleChoice(true);
		            	
		            	dialog.dismiss();
		       }
		   })
		   .setNegativeButton("No", new DialogInterface.OnClickListener() {
		       public void onClick(DialogInterface dialog, int id) {
		            allowDouble[0] = 0;
		            
		            //if(checkSet[0])
		            	rememberDoubleChoice(false);
		            	
		            	dialog.dismiss();
		       }
		   });
		AlertDialog alert = builder.create();
		alert.show();
    }

	public void showLegacyWarning(final RenderMode renderMode) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final boolean[] checkSet = {false};
		builder.setTitle("Warning")
			.setMessage("Would you like to switch to legacy experimental mode?\n\n" +
				"It is unstable and the OpenGL ES 3 mode you are currently in is superiour in every way.")
		   .setCancelable(true)
		   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		       public void onClick(DialogInterface dialog, int id) {
				allowLegacy[0] = 1;
				setMaxIterations(maxIterationsOld);
				Mandel.this.renderMode = renderMode;
				setRenderMode(renderMode);
				fullRedraw();

				dialog.dismiss();
		       }
		   })
		   .setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					allowLegacy[0] = -2;

					renderModeRadioGroup.check(R.id.radioA);
					allowLegacy[0] = 0;

					dialog.dismiss();
		       }
		   });
		AlertDialog alert = builder.create();
		alert.show();
    }
	
	void rememberDoubleChoice(boolean setTo) {
		SharedPreferences prefs = getSharedPreferences("MANDEL", 0);
		Editor edit = prefs.edit();
		edit.putBoolean("allowDouble", setTo);
		edit.apply();
	}
}


