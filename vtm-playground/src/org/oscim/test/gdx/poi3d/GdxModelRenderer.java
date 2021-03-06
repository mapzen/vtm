package org.oscim.test.gdx.poi3d;

import org.oscim.backend.GL20;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class GdxModelRenderer extends LayerRenderer {
	static final Logger log = LoggerFactory.getLogger(GdxModelRenderer.class);

	ModelBatch modelBatch;
	public MapCamera cam;
	Map mMap;

	boolean loading;

	public Environment lights;

	public Array<SharedModel> instances = new Array<SharedModel>();

	public Shader shader;
	public RenderContext renderContext;
	public Model model;
	private ModelBatch mBatch = new ModelBatch();

	public GdxModelRenderer(Map map) {
		mMap = map;
	}

	@Override
	public boolean setup() {

		modelBatch = new ModelBatch(new DefaultShaderProvider());

		lights = new Environment();
		lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1.f));
		lights.add(new DirectionalLight().set(0.3f, 0.3f, 0.3f, 0, 1, -0.2f));

		cam = new MapCamera(mMap);

		renderContext =
		        new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

		// shader = new DefaultShader(renderable.material,
		// renderable.mesh.getVertexAttributes(), true, false, 1, 0, 0, 0);
		// shader.init();

		return true;
	}

	@Override
	public synchronized void update(GLViewport v) {
		// if (loading && assets.update())
		// doneLoading();

		if (!isReady()) {
			cam.setPosition(v.pos);
			setReady(true);
		}

		// if (changed) {
		// cam.update(position, matrices);
		// }
	}

	Vector3 tempVector = new Vector3();
	float[] mBox = new float[8];

	Renderable r = new Renderable();

	@Override
	public void render(GLViewport v) {
		if (instances.size == 0)
			return;

		// GLUtils.checkGlError(">" + TAG);

		GL.glDepthMask(true);

		if (v.pos.zoomLevel < 16)
			GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		// set state that is expected after modelBatch.end();
		// modelBatch keeps track of its own state
		GLState.enableVertexArrays(-1, -1);
		GLState.bindTex2D(-1);
		GLState.useProgram(-1);
		GLState.test(false, false);
		GLState.blend(false);

		cam.update(v);

		Viewport p = mMap.viewport();
		p.getMapExtents(mBox, 10);
		float scale = (float) (cam.mMapPosition.scale / v.pos.scale);

		float dx = (float) (cam.mMapPosition.x - v.pos.x)
		        * (Tile.SIZE << cam.mMapPosition.zoomLevel);
		float dy = (float) (cam.mMapPosition.y - v.pos.y)
		        * (Tile.SIZE << cam.mMapPosition.zoomLevel);

		for (int i = 0; i < 8; i += 2) {
			mBox[i] *= scale;
			mBox[i] -= dx;
			mBox[i + 1] *= scale;
			mBox[i + 1] -= dy;
		}

		//int w = mMap.getWidth() / 2;
		//int h = mMap.getHeight() / 2;
		//float sqRadius = (w * w + h * h) / scale;

		synchronized (this) {
			if (instances.size == 0)
				return;

			//renderContext.begin();

			//			if (shader == null) {
			//				r = instances.get(0).getRenderable(r);
			//				DefaultShader.Config c = new DefaultShader.Config();
			//				c.numBones = 0;
			//				c.numDirectionalLights = 1;
			//				r.environment = lights;
			//
			//				shader = new DefaultShader(r, c);
			//				shader.init();
			//			}
			mBatch.begin(cam);
			//shader.begin(cam, renderContext);

			for (SharedModel instance : instances) {
				instance.transform.getTranslation(tempVector);
				//instance.getRenderables(renderables, pool);
				//	if (tempVector.x * tempVector.x + tempVector.y * tempVector.y > sqRadius)
				//	 continue;
				//	tempVector.scl(0.8f, 0.8f, 1);
				//	if (!GeometryUtils.pointInPoly(tempVector.x, tempVector.y, mBox, 8, 0))
				//	continue;

				mBatch.render(instance);

				//shader.render(r);
			}
			mBatch.end();

			//shader.end();
			//renderContext.end();
		}

		GL.glDepthMask(false);
		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
	}

	// @Override
	// public void dispose () {
	// modelBatch.dispose();
	// assets.dispose();
	// assets = null;
	// axesModel.dispose();
	// axesModel = null;
	// }
}
