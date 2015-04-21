package org.oscim.tiling.source;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSource;

import java.lang.Exception;
import java.lang.Integer;

public class UrlTileSourceTest {
	private UrlTileSource tileSource;

	@Before
	public void setUp() throws Exception {
		tileSource = new TestTileSource("http://example.org/tiles/vtm", "/{Z}/{X}/{Z}.vtm");
	}

	@Test
	public void shouldNotBeNull() throws Exception {
		assertThat(tileSource).isNotNull();
	}

	@Test
	public void shouldUseDefaultHttpEngine() throws Exception {
		TestTileDataSource dataSource = (TestTileDataSource) tileSource.getDataSource();
		assertThat(dataSource.getConnection()).isInstanceOf(LwHttp.class);
	}

	@Test
	public void shouldUseCustomHttpEngine() throws Exception {
		tileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory());
		TestTileDataSource dataSource = (TestTileDataSource) tileSource.getDataSource();
		assertThat(dataSource.getConnection()).isInstanceOf(OkHttpEngine.class);
	}

	@Test
	public void setApiKey_shouldAppendQueryString() throws Exception {
		tileSource.setApiKey("test123");
		assertThat(tileSource.getTileUrl(new Tile(0, 0, (byte) 0))).endsWith("?api_key=test123");
	}

	class TestTileSource extends UrlTileSource {
		public TestTileSource(String urlString, String tilePath) {
			super(urlString, tilePath);
		}

		@Override
		public ITileDataSource getDataSource() {
			return new TestTileDataSource(this, null, getHttpEngine());
		}
	}

	class TestTileDataSource extends UrlTileDataSource {
		public TestTileDataSource(UrlTileSource tileSource, ITileDecoder tileDecoder,
		        HttpEngine conn) {
			super(tileSource, tileDecoder, conn);
		}

		public HttpEngine getConnection() {
			return mConn;
		}
	}
}
