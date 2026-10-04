package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.util.Locale;
import org.testng.annotations.Test;

public class FilenameBoundaryTest {
  @Test
  public void directoryDotsAreNotFileExtensions() {
    assertEquals("", FilenameUtils.getSuffix("folder.v1/readme"));
    assertEquals("", FilenameUtils.getSuffix("folder.v1\\readme"));
    assertEquals("gz", FilenameUtils.getSuffix("folder.v1/archive.tar.gz"));
  }

  @Test
  public void pathMethodsUseTheLastOfBothSeparatorKinds() {
    String path = "root/parent\\child\\photo.PNG";
    assertEquals("photo.PNG", FilenameUtils.getFilename(path));
    assertEquals("photo", FilenameUtils.getBaseName(path));
    assertEquals("root/parent\\child", FilenameUtils.getSubPath(path));
    assertEquals("child", FilenameUtils.getParentFolderName(path));
  }

  @Test
  public void imageDetectionHandlesNullAndIsIndependentOfLocale() {
    assertFalse(FilenameUtils.isImageFile(null));
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      assertTrue(FilenameUtils.isImageFile("image.GIF"));
    } finally {
      Locale.setDefault(previous);
    }
  }
}
