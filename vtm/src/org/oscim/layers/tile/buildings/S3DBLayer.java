package org.oscim.layers.tile.buildings;

import org.oscim.backend.canvas.Color;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.map.Map;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.OffscreenRenderer;
import org.oscim.renderer.OffscreenRenderer.Mode;
import org.oscim.tiling.TileSource;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.ColorsCSS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3DBLayer extends TileLayer {
	static final Logger log = LoggerFactory.getLogger(S3DBLayer.class);
	static final boolean POST_FXAA = true;

	private final static int MAX_CACHE = 32;
	private final static int SRC_ZOOM = 16;

	/* TODO get from theme */
	private final static double HSV_S = 0.7;
	private final static double HSV_V = 1.2;

	private final TileSource mTileSource;

	public S3DBLayer(Map map, TileSource tileSource) {
		super(map, new TileManager(map, SRC_ZOOM, SRC_ZOOM, MAX_CACHE));
		setRenderer(new S3DBRenderer());

		mTileSource = tileSource;
		initLoader(2);
	}

	@Override
	protected S3DBTileLoader createLoader() {
		return new S3DBTileLoader(getManager(), mTileSource);
	}

	public static class S3DBRenderer extends TileRenderer {
		LayerRenderer mRenderer;

		public S3DBRenderer() {
			mRenderer = new BuildingRenderer(this, SRC_ZOOM, SRC_ZOOM, true, false);

			if (POST_FXAA)
				mRenderer = new OffscreenRenderer(Mode.SSAO_FXAA, mRenderer);
		}

		@Override
		public synchronized void update(GLViewport v) {
			super.update(v);
			mRenderer.update(v);
			setReady(mRenderer.isReady());
		}

		@Override
		public synchronized void render(GLViewport v) {
			mRenderer.render(v);
		}

		@Override
		public boolean setup() {
			mRenderer.setup();
			return super.setup();
		}
	}

	static int getColor(String color, boolean roof) {

		if (color.charAt(0) == '#') {
			int c = Color.parseColor(color, Color.CYAN);
			/* hardcoded colors are way too saturated for my taste */
			return ColorUtil.modHsv(c, 1.0, 0.4, HSV_V, true);
		}

		if (roof) {
			if ("brown" == color)
				return Color.get(120, 110, 110);
			if ("red" == color)
				return Color.get(235, 140, 130);
			if ("green" == color)
				return Color.get(150, 200, 130);
			if ("blue" == color)
				return Color.get(100, 50, 200);
		}
		if ("white" == color)
			return Color.get(240, 240, 240);
		if ("black" == color)
			return Color.get(86, 86, 86);
		if ("grey" == color || "gray" == color)
			return Color.get(120, 120, 120);
		if ("red" == color)
			return Color.get(255, 190, 190);
		if ("green" == color)
			return Color.get(190, 255, 190);
		if ("blue" == color)
			return Color.get(190, 190, 255);
		if ("yellow" == color)
			return Color.get(255, 255, 175);
		if ("darkgray" == color || "darkgrey" == color)
			return Color.DKGRAY;
		if ("lightgray" == color || "lightgrey" == color)
			return Color.LTGRAY;

		if ("transparent" == color)
			return Color.get(0, 1, 1, 1);

		Integer css = ColorsCSS.get(color);

		if (css != null)
			return ColorUtil.modHsv(css.intValue(), 1.0, HSV_S, HSV_V, true);

		log.debug("unknown color:{}", color);
		return 0;
	}

	static int getMaterialColor(String material, boolean roof) {

		if (roof) {
			if ("glass" == material)
				return Color.fade(Color.get(130, 224, 255), 0.9f);
		}
		if ("roof_tiles" == material)
			return Color.get(216, 167, 111);
		if ("tile" == material)
			return Color.get(216, 167, 111);

		if ("concrete" == material ||
		        "cement_block" == material)
			return Color.get(210, 212, 212);

		if ("metal" == material)
			return 0xFFC0C0C0;
		if ("tar_paper" == material)
			return 0xFF969998;
		if ("eternit" == material)
			return Color.get(216, 167, 111);
		if ("tin" == material)
			return 0xFFC0C0C0;
		if ("asbestos" == material)
			return Color.get(160, 152, 141);
		if ("glass" == material)
			return Color.get(130, 224, 255);
		if ("slate" == material)
			return 0xFF605960;
		if ("zink" == material)
			return Color.get(180, 180, 180);
		if ("gravel" == material)
			return Color.get(170, 130, 80);
		if ("copper" == material)
			// same as roof color:green
			return Color.get(150, 200, 130);
		if ("wood" == material)
			return Color.get(170, 130, 80);
		if ("grass" == material)
			return 0xFF50AA50;
		if ("stone" == material)
			return Color.get(206, 207, 181);
		if ("plaster" == material)
			return Color.get(236, 237, 181);
		if ("brick" == material)
			return Color.get(255, 217, 191);
		if ("stainless_steel" == material)
			return Color.get(153, 157, 160);
		if ("gold" == material)
			return 0xFFFFD700;

		log.debug("unknown material:{}", material);

		return 0;
	}
}
