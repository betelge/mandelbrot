package com.betel.mandelbrot;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import utils.ShaderLoader;
import utils.StringLoader;
import betel.alw3d.Alw3dModel;
import betel.alw3d.Alw3dView;
import betel.alw3d.renderer.CameraNode;
import betel.alw3d.renderer.FBO;
import betel.alw3d.renderer.Geometry;
import betel.alw3d.renderer.GeometryNode;
import betel.alw3d.renderer.Material;
import betel.alw3d.renderer.Node;
import betel.alw3d.renderer.QuadRenderPass;
import betel.alw3d.renderer.RenderMultiPass;
import betel.alw3d.renderer.ShaderProgram;
import betel.alw3d.renderer.Texture;
import betel.alw3d.renderer.Uniform;
import betel.alw3d.renderer.passes.CheckGlErrorPass;
import betel.alw3d.renderer.passes.ClearPass;
import betel.alw3d.renderer.passes.RenderPass;
import betel.alw3d.renderer.passes.SceneRenderPass;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Mandel extends ActionBarActivity implements OnTouchListener,
OnSeekBarChangeListener, OnLayoutChangeListener, OnCheckedChangeListener,
CheckGlErrorPass.OnGlErrorListener {
	
	private static String TAG = "MANDEL";
	
	private Alw3dModel model;
	private Alw3dView view;
	
	private RenderPass clearPass;
	private RenderPass mandelPass;
	private Material mandelMaterial;
	private RenderMode renderMode = RenderMode.SINGLE;
	private ShaderProgram mandelShaderProg;
	private ShaderProgram mandel64ShaderProg;
	private ShaderProgram mandel64ExpShaderProg;
	private ShaderProgram mandelInVertexShader;
	private ShaderProgram mandel64InVertexShader;
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
	static final private String HUD = "HUD";
	private Uniform scaleMandelUniform;
	private Uniform offsetMandelUniform;
	private Uniform offsetFineMandelUniform;
	
	private SceneRenderPass mandelVertexPass;
	
	private RenderPass finalPass;
	private Material finalMaterial;
	private ShaderProgram finalShaderProg;
	private float scaleFinalX = 1f, scaleFinalY = 1f;
	private float offsetFinalX = 0f, offsetFinalY = 0f;
	private Uniform scaleFinalUniform;
	private Uniform offsetFinalUniform;
	
	private Uniform maxInterationsUniform;
	private Uniform splitterFloatUniform;
		
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	
	private FBO mandelFBO;
	
	private AlertDialog.Builder maxIterDialog;
	private AlertDialog.Builder splitFloatDialog;
	
	private Toast outOfMemToast;
	private Toast otherGlErrorToast;
	
	static public enum RenderMode {
		SINGLE(0x001),
		EMULATED_DOUBLE(0x002),
		EXP_EMULATED_DOUBLE(0x003),
		SINGLE_IN_VERTEX(0x004),
		EXP_EMULATED_DOUBLE_IN_VERTEX(0x005);
		
		int value;
		
		RenderMode(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		StringLoader.setContext(this);
		
		mandelShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel_v, R.raw.mandel_f);
		mandel64ShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel64_v, R.raw.mandel64_f);
		mandel64ExpShaderProg = ShaderLoader.loadShaderProgram(R.raw.mandel64exp_v, R.raw.mandel64exp_f);
		mandelInVertexShader = ShaderLoader.loadShaderProgram(R.raw.mandel_in_vertex_v, R.raw.mandel_in_vertex_f);
		mandel64InVertexShader = ShaderLoader.loadShaderProgram(R.raw.mandel64exp_in_vert_v, R.raw.mandel64exp_in_vert_f);
		mandelMaterial = new Material(mandelShaderProg);
		
		maxInterationsUniform = new Uniform("MAX_ITER", 40f);
		mandelMaterial.addUniform(maxInterationsUniform);
		
		offsetMandelUniform = new Uniform("offset", (float)offsetMandelX, (float)offsetMandelY);
		mandelMaterial.addUniform(offsetMandelUniform);
		offsetFineMandelUniform = new Uniform("offsetFine", 0f, 0f);
		mandelMaterial.addUniform(offsetFineMandelUniform);
		scaleMandelUniform = new Uniform("scale", scaleMandelX, scaleMandelY);
		mandelMaterial.addUniform(scaleMandelUniform);
		splitterFloatUniform = new Uniform("split", 1025f);
		mandelMaterial.addUniform(splitterFloatUniform);
		

		scaleDetector = new ScaleGestureDetector(this, new ScaleListener());
		gestureDetector = new GestureDetector(this, new GestureListener());
		
		
		finalShaderProg = ShaderLoader.loadShaderProgram(R.raw.final_v, R.raw.final_f);
		offsetFinalUniform = new Uniform("offset", offsetFinalX, offsetFinalY);
		scaleFinalUniform = new Uniform("scale", scaleFinalX, scaleFinalY);
		
		outOfMemToast = Toast.makeText(this,
				"GPU Shader took too long to run.\n" +
				"Try simpler settings or zoom out.",
				Toast.LENGTH_LONG);
		otherGlErrorToast = Toast.makeText(this,
				"OpenGL error", Toast.LENGTH_SHORT);
		
		model = new Alw3dModel();
		view = new Alw3dView(this, model);
		setContentView(R.layout.activity_mandel);
				
		view.addOnLayoutChangeListener(this);
		FrameLayout frameLayout = (FrameLayout)findViewById(R.id.FrameLayout1);
		frameLayout.addView(view, 0);
		view.setOnTouchListener(this);
		
		SeekBar iterSeekBar = (SeekBar)findViewById(R.id.iterSeekBar);
		iterSeekBar.setMax(500);
		iterSeekBar.setProgress(40);
		iterSeekBar.setOnSeekBarChangeListener(this);
		
		View tempView = findViewById(R.id.linearLayout);
		RadioGroup renderModeRadioGroup = (RadioGroup)tempView.findViewById(R.id.renderModeRadioGroup);
		renderModeRadioGroup.setOnCheckedChangeListener(this);
		
		if(savedInstanceState != null) {
			offsetMandelX = savedInstanceState.getDouble(OFFSET_X);
		    offsetMandelY = savedInstanceState.getDouble(OFFSET_Y);
		    setOffsetMandelUniforms(offsetFinalX, offsetFinalY);

		    scale = savedInstanceState.getFloat(SCALE);
		    
		    splitterFloatUniform.set(savedInstanceState.getFloat(SPLITTER));
		    maxInterationsUniform.set(savedInstanceState.getFloat(MAX_ITER));
		    
		    renderMode = (RenderMode) savedInstanceState.getSerializable(RENDER_MODE);
		    
		    int hudVisibility = savedInstanceState.getInt(HUD);
		    View hudView = findViewById(R.id.HUD);
		    if(hudView != null)
		    	hudView.setVisibility(hudVisibility);
		}
		else {
			findViewById(R.id.HUD).setVisibility(View.GONE);
			findViewById(R.id.linearLayout).findViewById(
					R.id.renderModeRadioGroup).setVisibility(
							View.GONE);
		}
		
		setRenderMode(renderMode);
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
		
		savedInstanceState.putSerializable(RENDER_MODE, renderMode);
		
		savedInstanceState.putInt(HUD, findViewById(R.id.HUD).getVisibility());
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		mandelPass.setSilent(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mandel, menu);
		return true;
	}

	@Override
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
				  	mandelPass.setSilent(false);
				}
			});
			splitFloatDialog.show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if( v == view) {
			scaleDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			
			if(event.getActionMasked() == MotionEvent.ACTION_UP)
				if(event.getPointerCount() == 1) {
					// Probably last touch, so redraw the set
					
					// Pause the final drawing
					clearPass.setSilent(true);
					finalPass.setSilent(true);
					
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
					mandelPass.setSilent(false);
					finalPass.setSilent(false);
					clearPass.setSilent(false);
				}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		TextView iterText = (TextView)findViewById(R.id.iterTextView);
		iterText.setText("Iterations: " + arg0.getProgress());
		
		// If in a simple mode change the fractal live
		if(renderMode == RenderMode.SINGLE ||
				renderMode == RenderMode.SINGLE_IN_VERTEX) {
			maxInterationsUniform.set(arg0.getProgress());
			mandelPass.setSilent(false);
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
			mandelPass.setSilent(false);
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup arg0, int arg1) {
		if(arg0.getId() != R.id.renderModeRadioGroup) return;
		
		switch(arg0.getCheckedRadioButtonId()) {
		case R.id.radioS:
			renderMode = RenderMode.SINGLE;
			setRenderMode(renderMode);
			setRenderPasses();
			mandelPass.setSilent(false);
			break;
		case R.id.radioED:
			renderMode = RenderMode.EMULATED_DOUBLE;
			setRenderMode(renderMode);
			setRenderPasses();
	        mandelPass.setSilent(false);
			break;
		case R.id.radioExpED:
			renderMode = RenderMode.EXP_EMULATED_DOUBLE;
			setRenderMode(renderMode);
			setRenderPasses();
	        mandelPass.setSilent(false);
	        break;
		case R.id.radioSV:
			renderMode = RenderMode.SINGLE_IN_VERTEX;
			setRenderMode(renderMode);
			setRenderPasses();
			mandelPass.setSilent(false);
			break;
		case R.id.radioEDV:
			renderMode = RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX;
			setRenderMode(renderMode);
			setRenderPasses();
			mandelPass.setSilent(false);
			break;
		default:
			renderMode = RenderMode.SINGLE;
			setRenderMode(renderMode);
			setRenderPasses();
			mandelPass.setSilent(false);
			break;
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
	
	public void resetPos(View view) {
		offsetMandelX = -0.75f/2;
		offsetMandelY = 0;
		scale = 1;
		
		setOffsetMandelUniforms(offsetMandelX, offsetMandelY);
		setScaleMandelUniform(scale);
		renderMode = RenderMode.SINGLE;
		maxInterationsUniform.set(40);
		
		mandelPass.setSilent(false);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
		    float factor = detector.getScaleFactor();
		    scaleFinalX /= factor;
		    scaleFinalY /= factor;
		    scaleFinalUniform.set(scaleFinalX, scaleFinalY);
		
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
			offsetMandelX += scaleMandelX*(e.getX()/view.getWidth()*2 -1);
			offsetMandelY -= scaleMandelY*(e.getY()/view.getHeight()*2 -1);
			scaleMandelX /= 2;
			scaleMandelY /= 2;
			scaleMandelUniform.set(scaleMandelX, scaleMandelY);
			return true;
		}
		
		
	}

	@Override
	public void onLayoutChange(View v, int left, int top, int right,
			int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if(view.getHeight() == 0 || view.getWidth() == 0) return;
		
		// This will be done first after the view receives it's size in the layout.
		
		setScaleMandelUniform(scale);
		setOffsetMandelUniforms(offsetMandelX, offsetMandelY);
		
		
		Texture mandelTexture = new Texture(null, Texture.TextureType.TEXTURE_2D, view.getWidth(),
				view.getHeight(), Texture.TexelType.UBYTE, Texture.Format.GL_RGB, Texture.Filter.NEAREST,
				Texture.WrapMode.CLAMP_TO_EDGE);
		// TODO: Are we leaking video memory here when we loose the old FBO?
		mandelFBO = new FBO(mandelTexture, view.getWidth(), view.getHeight());
		
		// Initialize the pixelMesh
		Node vertexRootNode = new Node();
		mandelVertexPass = new SceneRenderPass(vertexRootNode,
				/*Ignored dummy camera*/ new CameraNode(asp, asp, asp, asp)/*,
				mandelFBO*/);
		mandelVertexPass.setOneTime(true);
		
		final int maxShort = 256*256-1;
		int w = view.getWidth();
		int h = view.getHeight();
		int fullH = h;
		
		// Limit the indices to the size of short
		if(w*h > maxShort) {
			int r = w % maxShort;
			h = (maxShort - r) / w;
		}
		
		// TODO: Use just one piece repeatedly
		// How many short sized pieces do we need?
		int parts = (int)Math.ceil((float)fullH/h);
		
		
		ShortBuffer indices = ShortBuffer.allocate(w*h);
		Geometry.Attribute posAt = new Geometry.Attribute();
		posAt.name = "position";
		posAt.size = 3;
		posAt.type = Geometry.Type.FLOAT;
		posAt.buffer = FloatBuffer.allocate(3*w*h);
		
		for(int i = 0; i < h; i++) {
			for(int j = 0; j < w; j++) {
				((FloatBuffer) posAt.buffer).put(-1 + (2*j + 1)/(float)w);
				((FloatBuffer) posAt.buffer).put(-1 + (2*i + 1)/(float)fullH);
				((FloatBuffer) posAt.buffer).put(0);
				
				indices.put((short)(i*w+j));
			}
		}
		posAt.buffer.flip();
		indices.flip();
		
		List<Geometry.Attribute> lat = new ArrayList<Geometry.Attribute>();
		lat.add(posAt);
		Geometry pixelMesh = new Geometry(Geometry.PrimitiveType.POINTS, indices, lat);
		
		// Add multiple pieces
		for(int p = 0; p < parts; p++) {
			GeometryNode pixelGeometryNode = new GeometryNode(pixelMesh, mandelMaterial);
			pixelGeometryNode.getTransform().getPosition().setY(2*p/(float)parts);
			vertexRootNode.attach(pixelGeometryNode);
		}
		
		
		finalMaterial = new Material(finalShaderProg);
		finalMaterial.addTexture("tex", mandelTexture);
		finalMaterial.addUniform(offsetFinalUniform);
		finalMaterial.addUniform(scaleFinalUniform);
		
		setRenderPasses();
	}
	
	private void setRenderPasses() {
		synchronized (model.getRenderPasses()) {
			model.getRenderPasses().clear();
			clearPass = new ClearPass(GLES20.GL_COLOR_BUFFER_BIT, null);
			model.addRenderPass(clearPass);
			setCorrectMandelPass();
			mandelVertexPass.setFbo(mandelFBO);
			mandelPass.setOneTime(true);
			mandelPass.setSilent(false);
			model.addRenderPass(mandelPass);
			finalPass = new QuadRenderPass(finalMaterial);
			model.addRenderPass(finalPass);
			CheckGlErrorPass errorPass = new CheckGlErrorPass(true);
			model.addRenderPass(errorPass);
			errorPass.setOnGlErrorListener(this);
		}
	}
	
	private void setCorrectMandelPass() {
		if(renderMode == RenderMode.SINGLE_IN_VERTEX ||
				renderMode == RenderMode.EXP_EMULATED_DOUBLE_IN_VERTEX)
			mandelPass = mandelVertexPass;
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
		if(error == GLES20.GL_OUT_OF_MEMORY)
			outOfMemToast.show();
		else
			otherGlErrorToast.show();
	}
}


