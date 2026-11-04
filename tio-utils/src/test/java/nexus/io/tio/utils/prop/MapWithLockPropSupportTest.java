package nexus.io.tio.utils.prop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class MapWithLockPropSupportTest {

  @Test
  public void settingNullRemovesTheAttribute() {
    MapWithLockPropSupport props = new MapWithLockPropSupport();
    props.setAttribute("k", "v");
    assertEquals("v", props.getAttribute("k"));
    props.setAttribute("k", null);
    assertNull(props.getAttribute("k"));
  }
}
