/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer.bucket;

import static org.oscim.backend.GL20.GL_SHORT;
import static org.oscim.backend.GL20.GL_TRIANGLES;
import static org.oscim.backend.GL20.GL_UNSIGNED_SHORT;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MercatorProjection;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.VertexData.Chunk;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.TessJNI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshBucket extends RenderBucket {
	static final Logger log = LoggerFactory.getLogger(MeshBucket.class);
	static final boolean dbgRender = false;

	public AreaStyle area;
	public float heightOffset;

	private TessJNI tess;

	private int numPoints;

	public MeshBucket(int level) {
		super(RenderBucket.MESH);
		this.level = level;
	}

	public void addMesh(GeometryBuffer geom) {
		numPoints += geom.pointPos;
		if (tess == null)
			tess = new TessJNI(8);

		tess.addContour2D(geom.index, geom.points);
	}

	public void addConvexMesh(GeometryBuffer geom) {
		short start = (short) numVertices;

		if (numVertices >= (1 << 16)) {
			return;
		}

		vertexItems.add((short) (geom.points[0] * COORD_SCALE),
		                (short) (geom.points[1] * COORD_SCALE));

		vertexItems.add((short) (geom.points[2] * COORD_SCALE),
		                (short) (geom.points[3] * COORD_SCALE));
		short prev = (short) (start + 1);

		numVertices += 2;

		for (int i = 4; i < geom.index[0]; i += 2) {

			vertexItems.add((short) (geom.points[i + 0] * COORD_SCALE),
			                (short) (geom.points[i + 1] * COORD_SCALE));

			indiceItems.add(start, prev, ++prev);
			numVertices++;

			numIndices += 3;
		}

		//numPoints += geom.pointPos;
		//tess.addContour2D(geom.index, geom.points);
	}

	protected void prepare() {
		if (tess == null)
			return;

		if (numPoints == 0) {
			tess.dispose();
			return;
		}
		if (!tess.tesselate()) {
			tess.dispose();
			log.error("error in tessellation {}", numPoints);
			return;
		}

		int nelems = tess.getElementCount() * 3;

		//int startVertex = vertexItems.countSize();

		for (int offset = indiceItems.countSize(); offset < nelems;) {
			int size = nelems - offset;
			if (size > VertexData.SIZE)
				size = VertexData.SIZE;

			Chunk chunk = indiceItems.obtainChunk();

			tess.getElements(chunk.vertices, offset, size);
			offset += size;

			//if (startVertex != 0)
			// FIXME 

			indiceItems.releaseChunk(size);
		}

		int nverts = tess.getVertexCount() * 2;

		for (int offset = 0; offset < nverts;) {
			int size = nverts - offset;
			if (size > VertexData.SIZE)
				size = VertexData.SIZE;

			Chunk chunk = vertexItems.obtainChunk();

			tess.getVertices(chunk.vertices, offset, size,
			                 MapRenderer.COORD_SCALE);
			offset += size;

			vertexItems.releaseChunk(size);
		}

		this.numIndices += nelems;
		this.numVertices += nverts >> 1;

		tess.dispose();
	}

	public static class Renderer {
		static Shader shader;

		static boolean init() {
			shader = new Shader("mesh_layer_2D");
			return true;
		}

		static class Shader extends GLShader {
			int uMVP, uColor, uHeight, aPos;

			Shader(String shaderFile) {
				if (!create(shaderFile))
					return;

				uMVP = getUniform("u_mvp");
				uColor = getUniform("u_color");
				uHeight = getUniform("u_height");
				aPos = getAttrib("a_pos");
			}
		}

		public static RenderBucket draw(RenderBucket l, GLViewport v) {
			GLState.blend(true);

			Shader s = shader;

			s.useProgram();
			GLState.enableVertexArrays(s.aPos, -1);

			v.mvp.setAsUniform(s.uMVP);

			float heightOffset = 0;
			GL.glUniform1f(s.uHeight, heightOffset);

			int zoom = v.pos.zoomLevel;
			float scale = (float) v.pos.getZoomScale();

			for (; l != null && l.type == MESH; l = l.next) {
				MeshBucket ml = (MeshBucket) l;

				if (ml.heightOffset != heightOffset) {
					heightOffset = ml.heightOffset;

					GL.glUniform1f(s.uHeight, heightOffset /
					        MercatorProjection.groundResolution(v.pos));
				}

				if (ml.area == null)
					GLUtils.setColor(s.uColor, Color.BLUE, 0.4f);
				else {
					setColor(ml.area.current(), s, zoom, scale);
				}
				GL.glVertexAttribPointer(s.aPos, 2, GL_SHORT,
				                         false, 0, ml.vertexOffset);

				GL.glDrawElements(GL_TRIANGLES,
				                  ml.numIndices,
				                  GL_UNSIGNED_SHORT,
				                  ml.indiceOffset);

				if (dbgRender) {
					int c = (ml.area == null) ? Color.BLUE : ml.area.color;
					GL.glLineWidth(1);
					//c = ColorUtil.shiftHue(c, 0.5);
					c = ColorUtil.modHsv(c, 1.1, 1.0, 0.8, true);
					GLUtils.setColor(s.uColor, c, 1);
					GL.glDrawElements(GL20.GL_LINES,
					                  ml.numIndices,
					                  GL_UNSIGNED_SHORT,
					                  ml.vertexOffset);
				}
			}
			return l;
		}

		private static final int OPAQUE = 0xff000000;
		private static final float FADE_START = 1.3f;

		static void setColor(AreaStyle a, Shader s, int zoom, float scale) {
			if (a.fadeScale >= zoom) {
				float f = 1.0f;
				/* fade in/out */
				if (a.fadeScale >= zoom) {
					if (scale > FADE_START)
						f = scale - 1;
					else
						f = FADE_START - 1;
				}
				GLState.blend(true);

				GLUtils.setColor(s.uColor, a.color, f);

			} else if (a.blendScale > 0 && a.blendScale <= zoom) {
				/* blend colors (not alpha) */
				GLState.blend(false);

				if (a.blendScale == zoom)
					GLUtils.setColorBlend(s.uColor, a.color,
					                      a.blendColor, scale - 1.0f);
				else
					GLUtils.setColor(s.uColor, a.blendColor, 1);

			} else {
				/* test if color contains alpha */
				GLState.blend((a.color & OPAQUE) != OPAQUE);

				GLUtils.setColor(s.uColor, a.color, 1);
			}
		}
	}
}
