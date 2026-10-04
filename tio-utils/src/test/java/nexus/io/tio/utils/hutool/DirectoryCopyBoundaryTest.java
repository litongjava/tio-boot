package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.testng.annotations.Test;

public class DirectoryCopyBoundaryTest {
  private interface Case { void run(Path root) throws Exception; }
  private void fixture(Case action) throws Exception {
    Path parent = Paths.get("target").toAbsolutePath().normalize().toRealPath();
    Path root = Files.createTempDirectory(parent, "copy-test-");
    try {
      action.run(root);
    } finally {
      // Restrict cleanup to the temporary fixture created under this module's target.
      if (!root.toRealPath().startsWith(parent) || root.equals(parent)) {
        throw new IOException("Fixture escaped the test directory");
      }
      Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }
        @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
          if (error != null) { throw error; }
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
  private void rejects(Path source, Path target, boolean overwrite) throws Exception {
    try {
      FileUtil.copyDirectory(source, target, overwrite);
      fail("Overlapping directories must be rejected");
    } catch (IOException expected) {
      assertTrue(Files.exists(source.resolve("keep.txt")));
    }
  }
  @Test public void equalSourceAndTargetPreserveSource() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectory(root.resolve("source"));
      Files.write(source.resolve("keep.txt"), new byte[] {1});
      rejects(source, source, true);
    });
  }
  @Test public void ancestorTargetPreservesSource() throws Exception {
    fixture(root -> {
      Path target = Files.createDirectory(root.resolve("parent"));
      Path source = Files.createDirectory(target.resolve("source"));
      Files.write(source.resolve("keep.txt"), new byte[] {1});
      rejects(source, target, true);
    });
  }
  @Test public void overwriteReplacesSeparateTarget() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectories(root.resolve("source/sub"));
      Files.write(source.resolve("new.txt"), new byte[] {1, 2});
      Path target = Files.createDirectory(root.resolve("target"));
      Files.write(target.resolve("old.txt"), new byte[] {3});
      FileUtil.copyDirectory(source.getParent(), target, true);
      assertFalse(Files.exists(target.resolve("old.txt")));
      assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(target.resolve("sub/new.txt")));
    });
  }
  @Test public void nonOverwritingCopyRetainsExistingFiles() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectory(root.resolve("source"));
      Path target = Files.createDirectory(root.resolve("target"));
      Files.write(source.resolve("same.txt"), new byte[] {1});
      Files.write(source.resolve("new.txt"), new byte[] {2});
      Files.write(target.resolve("same.txt"), new byte[] {3});
      FileUtil.copyDirectory(source, target, false);
      assertArrayEquals(new byte[] {3}, Files.readAllBytes(target.resolve("same.txt")));
      assertArrayEquals(new byte[] {2}, Files.readAllBytes(target.resolve("new.txt")));
    });
  }
  @Test public void descendantTargetIsRejectedBeforeCreation() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectory(root.resolve("source"));
      Files.write(source.resolve("keep.txt"), new byte[] {1});
      Path target = source.resolve("child/nested");
      rejects(source, target, true);
      rejects(source, target, false);
      assertFalse(Files.exists(source.resolve("child")));
      rejects(source, source.resolve("."), false);
    });
  }
  @Test public void missingSeparateTargetParentsAreCreated() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectory(root.resolve("source"));
      Files.write(source.resolve("keep.txt"), new byte[] {1});
      Path target = root.resolve("new/nested/target");
      FileUtil.copyDirectory(source, target, false);
      assertArrayEquals(new byte[] {1}, Files.readAllBytes(target.resolve("keep.txt")));
    });
  }
  @Test public void linkedTargetProtectsSourceAndPreservesSeparateReferent() throws Exception {
    fixture(root -> {
      Path source = Files.createDirectory(root.resolve("source"));
      Files.write(source.resolve("keep.txt"), new byte[] {1});
      Path link = root.resolve("link");
      try {
        Files.createSymbolicLink(link, source);
      } catch (IOException | UnsupportedOperationException e) {
        throw new org.testng.SkipException("Symbolic links are unavailable", e);
      }
      rejects(source, link, true);
      Files.delete(link);
      Path referent = Files.createDirectory(root.resolve("referent"));
      Files.write(referent.resolve("original.txt"), new byte[] {2});
      Files.createSymbolicLink(link, referent);
      FileUtil.copyDirectory(source, link, true);
      assertFalse(Files.isSymbolicLink(link));
      assertArrayEquals(new byte[] {2}, Files.readAllBytes(referent.resolve("original.txt")));
      assertArrayEquals(new byte[] {1}, Files.readAllBytes(link.resolve("keep.txt")));
    });
  }
}
