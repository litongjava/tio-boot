package nexus.io.tio.utils.page;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import nexus.io.model.page.Page;

public class PageBoundaryTest {
  @Test
  public void listAndSetExposeAccuratePageMetadata() {
    Page<Integer> page = PageUtils.fromList(Arrays.asList(1, 2, 3, 4, 5), 2, 2);
    assertEquals(Arrays.asList(3, 4), page.getList());
    assertEquals(3, page.getTotalPage());
    assertEquals(5, page.getTotalRow());
    assertFalse(page.isLastPage());
    Page<Integer> setPage = PageUtils.fromSet(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), 1, 10);
    assertEquals(10, setPage.getPageSize());
    assertEquals(1, setPage.getTotalPage());
  }

  @Test
  public void hugePageNumbersReturnEmptyInsteadOfOverflowing() {
    assertTrue(PageUtils.fromList(Arrays.asList(1, 2, 3), Integer.MAX_VALUE, 2).getList().isEmpty());
    assertTrue(PageUtils.fromSet(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Integer.MAX_VALUE, 2).getList().isEmpty());
  }

  @Test
  public void emptyAndDefaultArgumentsHaveUsableMetadata() {
    Page<Integer> empty = PageUtils.fromList(Collections.<Integer>emptyList(), 1, 20);
    assertEquals(20, empty.getPageSize());
    assertEquals(0, empty.getTotalPage());
    Page<Integer> all = PageUtils.fromList(Arrays.asList(1, 2, 3), 0, 0);
    assertEquals(1, all.getPageNumber());
    assertEquals(1, all.getTotalPage());
    assertEquals(Arrays.asList(1, 2, 3), all.getList());
    assertNull(PageUtils.fromList(null, 1, 10));
  }

  @Test
  public void converterOnlyVisitsTheRequestedPage() {
    AtomicInteger calls = new AtomicInteger();
    Page<String> page = PageUtils.fromList(Arrays.asList(1, 2, 3), 2, 2, value -> {
      calls.incrementAndGet();
      return value.toString();
    });
    assertEquals(Collections.singletonList("3"), page.getList());
    assertEquals(1, calls.get());
    assertTrue(page.isLastPage());
  }
}
