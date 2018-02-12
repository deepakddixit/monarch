package io.ampool.presto.connector;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.Range;
import com.facebook.presto.spi.predicate.TupleDomain;
import io.ampool.monarch.table.filter.Filter;
import io.ampool.monarch.table.filter.FilterList;
import io.ampool.monarch.table.filter.SingleColumnValueFilter;
import io.ampool.monarch.types.CompareOp;

/**
 * Created by deepak on 9/2/18.
 */
public class FilterUtils {

  public static Filter convertToAmpoolFilters(TupleDomain<ColumnHandle> queryConstraints) {
    FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ALL);
    if (!queryConstraints.isNone()) {
      Map<ColumnHandle, Domain> columnHandleDomainMap = queryConstraints.getDomains().get();
      columnHandleDomainMap.forEach((columnHandle, domain) -> {
        if (domain != null) {
          filters
              .addFilter(toPredicate(((AmpoolColumnHandle) columnHandle).getColumnName(), domain));
        }
      });
    }
    Filter filter = optimizeFilters(filters);
    return filter;
  }

  private static Filter optimizeFilters(Filter filters) {
    if (filters instanceof FilterList) {
      if (((FilterList) filters).getFilters().size() == 0) {
        return null;
      }
      if (((FilterList) filters).getFilters().size() == 1) {
        return optimizeFilters(((FilterList) filters).getFilters().get(0));
      }
    }
    return filters;
  }

  private static Filter toPredicate(String columnName, Domain domain) {
//    checkArgument(domain.getType().isOrderable(), "Domain type must be orderable");

//    if (domain.getValues().isNone()) {
//      return domain.isNullAllowed() ? columnName + " IS NULL" : "FALSE";
//    }

//    if (domain.getValues().isAll()) {
//      return domain.isNullAllowed() ? "TRUE" : columnName + " IS NOT NULL";
//    }

    List<Filter> disjuncts = new ArrayList<>();
    List<Object> singleValues = new ArrayList<>();
    for (Range range : domain.getValues().getRanges().getOrderedRanges()) {
//      checkState(!range.isAll()); // Already checked
      if (range.isSingleValue()) {
        singleValues.add(range.getLow().getValue());
      } else {
        List<Filter> rangeConjuncts = new ArrayList<>();
        if (!range.getLow().isLowerUnbounded()) {
          switch (range.getLow().getBound()) {
            case ABOVE:
              rangeConjuncts.add(toPredicate(columnName, ">", range.getLow().getValue()));
              break;
            case EXACTLY:
              rangeConjuncts.add(toPredicate(columnName, ">=", range.getLow().getValue()));
              break;
            case BELOW:
              throw new IllegalArgumentException("Low marker should never use BELOW bound");
            default:
              throw new AssertionError("Unhandled bound: " + range.getLow().getBound());
          }
        }
        if (!range.getHigh().isUpperUnbounded()) {
          switch (range.getHigh().getBound()) {
            case ABOVE:
              throw new IllegalArgumentException("High marker should never use ABOVE bound");
            case EXACTLY:
              rangeConjuncts.add(toPredicate(columnName, "<=", range.getHigh().getValue()));
              break;
            case BELOW:
              rangeConjuncts.add(toPredicate(columnName, "<", range.getHigh().getValue()));
              break;
            default:
              throw new AssertionError("Unhandled bound: " + range.getHigh().getBound());
          }
        }
        if (!rangeConjuncts.isEmpty()) {
          FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
          rangeConjuncts.forEach(filter -> {
            filterList.addFilter(filter);
          });
          disjuncts.add(filterList);
        }
      }
    }

    // Add back all of the possible single values either as an equality or an IN predicate
    if (singleValues.size() == 1) {
      disjuncts.add(toPredicate(columnName, "=", getOnlyElement(singleValues)/*, type*/));
    } else if (singleValues.size() > 1) {
      FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
      for (Object value : singleValues) {
        filterList.addFilter(toPredicate(columnName, "=", value));
      }
      disjuncts.add(filterList);
    }

    // Add nullability disjuncts
//    checkState(!disjuncts.isEmpty());
//    if (domain.isNullAllowed()) {
//      disjuncts.add(columnName + " IS NULL");
//    }
    FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
    disjuncts.forEach(filter -> {
      filterList.addFilter(filter);
    });
    disjuncts.add(filterList);

    return filterList;
//    return "(" + Joiner.on(" OR ").join(disjuncts) + ")";
  }

  private static SingleColumnValueFilter toPredicate(String columnName, String operator,
                                                     Object value/*, Type type*/) {
    CompareOp scanOperator = getScanOperator(operator);
    SingleColumnValueFilter filter = new SingleColumnValueFilter(columnName, scanOperator, value);
    return filter;
  }

  private static CompareOp getScanOperator(String operator) {
    switch (operator) {
      case "=":
        return CompareOp.EQUAL;
      case ">":
        return CompareOp.GREATER;
      case ">=":
        return CompareOp.GREATER_OR_EQUAL;
      case "<":
        return CompareOp.LESS;
      case "<=":
        return CompareOp.LESS_OR_EQUAL;
      case "!=":
        return CompareOp.NOT_EQUAL;
    }
    return null;
  }
}
