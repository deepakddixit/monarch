package io.ampool.presto.connector;

import static com.facebook.presto.spi.type.IntegerType.INTEGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.Range;
import com.facebook.presto.spi.predicate.SortedRangeSet;
import com.facebook.presto.spi.predicate.TupleDomain;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ampool.monarch.table.filter.Filter;
import io.ampool.monarch.table.filter.FilterList;
import io.ampool.monarch.table.filter.SingleColumnValueFilter;
import io.ampool.monarch.types.CompareOp;
import io.ampool.monarch.types.TypeHelper;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by deepak on 9/2/18.
 */
public class PredicateToFilterConversionTest {

  private List<AmpoolColumnHandle> columns = null;

  @Before
  public void setUp() {
    columns = ImmutableList.of(
        new AmpoolColumnHandle("test_id", "col_0", INTEGER, 0),
        new AmpoolColumnHandle("test_id", "col_1", INTEGER, 1)
    );
  }


  @Test
  public void testNoFilters() {
    TupleDomain<ColumnHandle> empty = TupleDomain.none();
    Filter filter = FilterUtils.convertToAmpoolFilters(empty);
    assertNull(filter);
  }

  @Test
  public void testSingleEqualFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, "="))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.EQUAL));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }

  @Test
  public void testSingleGreaterFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, ">"))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.GREATER));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }

  @Test
  public void testSingleGreaterOREqualFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, ">="))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.GREATER_OR_EQUAL));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }

  @Test
  public void testSingleLessFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, "<"))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.LESS));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }

  @Test
  public void testSingleLessOREqualFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, "<="))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.LESS_OR_EQUAL));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }

  @Test
  public void testOREqualFilter() {
    TupleDomain<ColumnHandle> tupleDomain = TupleDomain.withColumnDomains(
        ImmutableMap.<ColumnHandle, Domain>builder()
            .put(columns.get(0), createDomain(INTEGER, 100l, "="))
            .put(columns.get(1), createDomain(INTEGER, 100l, "="))
            .build());

    Filter filter = FilterUtils.convertToAmpoolFilters(tupleDomain);
    System.out.println("Filters " + TypeHelper.deepToString(filter));

    assertTrue("Filter must be SingleColumnValueFilter",
        (filter instanceof SingleColumnValueFilter));
    assertTrue("Operator must be EQUAL",
        ((SingleColumnValueFilter) filter).getOperator().equals(
            CompareOp.EQUAL));
    assertTrue("Column Name must be " + columns.get(0).getColumnName(),
        ((SingleColumnValueFilter) filter).getColumnNameString().equals(
            columns.get(0).getColumnName()));
    System.out
        .println("Value: " + ((SingleColumnValueFilter) filter).getValue().getClass());
    assertEquals(100l, ((SingleColumnValueFilter) filter).getValue());
  }


  public Domain createDomain(Type type, Object value, String operator) {
    switch (operator) {
      case "=":
        return Domain.create(SortedRangeSet.copyOf(type,
            ImmutableList.of(
                Range.equal(type, value))),
            false);
      case ">":
        return Domain.create(SortedRangeSet.copyOf(type,
            ImmutableList.of(
                Range.greaterThan(type, value))),
            false);
      case ">=":
        return Domain.create(SortedRangeSet.copyOf(type,
            ImmutableList.of(
                Range.greaterThanOrEqual(type, value))),
            false);
      case "<":
        return Domain.create(SortedRangeSet.copyOf(type,
            ImmutableList.of(
                Range.lessThan(type, value))),
            false);
      case "<=":
        return Domain.create(SortedRangeSet.copyOf(type,
            ImmutableList.of(
                Range.lessThanOrEqual(type, value))),
            false);
    }

    return Domain.create(SortedRangeSet.copyOf(type,
        ImmutableList.of(
            Range.equal(type, value))),
        false);
  }
}
