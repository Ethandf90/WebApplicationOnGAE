package com.project.travel.form;

import static com.project.travel.service.OfyService.ofy;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.cmd.Query;
import com.project.travel.mode.Travel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class TravelQueryForm {

    private static final Logger LOG = Logger.getLogger(TravelQueryForm.class.getName());

    public static enum FieldType {
        STRING, INTEGER
    }

    public static enum Field {
        CITY("city", FieldType.STRING),
        TOPIC("topics", FieldType.STRING),
        MONTH("month", FieldType.INTEGER),
        MAX_ATTENDEES("maxAttendees", FieldType.INTEGER);

        private String fieldName;

        private FieldType fieldType;

        private Field(String fieldName, FieldType fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        private String getFieldName() {
            return this.fieldName;
        }
    }

    public static enum Operator {
        EQ("=="),
        LT("<"),
        GT(">"),
        LTEQ("<="),
        GTEQ(">="),
        NE("!=");

        private String queryOperator;

        private Operator(String queryOperator) {
            this.queryOperator = queryOperator;
        }

        private String getQueryOperator() {
            return this.queryOperator;
        }

        private boolean isInequalityFilter() {
            return this.queryOperator.contains("<") || this.queryOperator.contains(">") ||
                    this.queryOperator.contains("!");
        }
    }


    public static class Filter {
        private Field field;
        private Operator operator;
        private String value;

        public Filter () {}

        public Filter(Field field, Operator operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        public Field getField() {
            return field;
        }

        public Operator getOperator() {
            return operator;
        }

        public String getValue() {
            return value;
        }
    }


    private List<Filter> filters = new ArrayList<>(0);

    //Holds the first inequalityFilter for checking the feasibility of the whole query.
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Filter inequalityFilter;

    public TravelQueryForm() {}

    // Checks the feasibility of the whole query.
    private void checkFilters() {
        for (Filter filter : this.filters) {
            if (filter.operator.isInequalityFilter()) {
                // Only one inequality filter is allowed.
                if (inequalityFilter != null && !inequalityFilter.field.equals(filter.field)) {
                    throw new IllegalArgumentException(
                            "Inequality filter is allowed on only one field.");
                }
                inequalityFilter = filter;
            }
        }
    }


    public List<Filter> getFilters() {
        return ImmutableList.copyOf(filters);
    }

    public TravelQueryForm filter(Filter filter) {
        if (filter.operator.isInequalityFilter()) {
            // Only allows inequality filters on a single field.
            if (inequalityFilter != null && !inequalityFilter.field.equals(filter.field)) {
                throw new IllegalArgumentException(
                        "Inequality filter is allowed on only one field.");
            }
            inequalityFilter = filter;
        }
        filters.add(filter);
        return this;
    }

    //Returns an Objectify Query object for the specified filters.
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Query<Travel> getQuery() {
        // First check the feasibility of inequality filters.
        checkFilters();
        Query<Travel> query = ofy().load().type(Travel.class);
        if (inequalityFilter == null) {
            // Order by name.
            query = query.order("name");
        } else {
            // If we have any inequality filters, order by the field first.
            query = query.order(inequalityFilter.field.getFieldName());
            query = query.order("name");
        }
        for (Filter filter : this.filters) {
            // Applies filters in order.
            if (filter.field.fieldType == FieldType.STRING) {
                query = query.filter(String.format("%s %s", filter.field.getFieldName(),
                        filter.operator.getQueryOperator()), filter.value);
            } else if (filter.field.fieldType == FieldType.INTEGER) {
                query = query.filter(String.format("%s %s", filter.field.getFieldName(),
                        filter.operator.getQueryOperator()), Integer.parseInt(filter.value));
            }
        }
        LOG.info(query.toString());
        return query;
    }
}
