package com.rewe.digital.gui.controls.helper.autocomplete;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SparkSchemaTraverseUtil {
    public List<StructField> getChildrenOfField(final List<String> selectedColumns,
                                                final StructType schema) {
        final StructField[] rootLevelFields = schema.fields();
        if (selectedColumns.isEmpty()) {
            return getFieldNames(rootLevelFields);
        } else {
            Optional<StructField> fieldToLookFor = Optional.empty();
            Optional<StructField> previousFoundField = Optional.empty();
            for (String fieldName : selectedColumns) {
                if (fieldToLookFor.isPresent()) {
                    previousFoundField = fieldToLookFor;
                    fieldToLookFor = getNestedField(fieldName, getFields(fieldToLookFor));
                } else {
                    previousFoundField = fieldToLookFor;
                    fieldToLookFor = getNestedField(fieldName, rootLevelFields);
                }
            }
            if (fieldToLookFor.isPresent()) {
                return fieldToLookFor.map(
                        this::getStructFields)
                        .orElseGet(() -> getFieldNames(rootLevelFields));
            } else {
                return previousFoundField.map(
                        this::getStructFields)
                        .orElseGet(() -> getFieldNames(rootLevelFields));
            }
        }
    }

    private List<StructField> getStructFields(StructField structField) {
        if (structField.dataType() instanceof StructType) {
            return getChildrenOfStructTypedField(structField);
        } else {
            return new ArrayList<StructField>();
        }
    }

    private List<StructField> getChildrenOfStructTypedField(StructField structField) {
        return Stream.of(structField)
                .map(StructField::dataType)
                .flatMap(dataType -> Arrays.stream(((StructType) dataType).fields()))
                .collect(Collectors.toList());
    }

    private List<StructField> getFieldNames(StructField[] rootLevelFields) {
        return Arrays.stream(rootLevelFields)
                .collect(Collectors.toList());
    }

    private StructField[] getFields(Optional<StructField> parent) {
        return parent.map(structField -> Stream.of(structField)
                .map(s -> ((StructType) s.dataType()))
                .flatMap(s -> Arrays.stream(s.fields()))
                .toArray(StructField[]::new)
        )
                .orElse(new StructField[0]);
    }

    private Optional<StructField> getNestedField(String fieldName, StructField[] fields) {
        Optional<StructField> fieldToLookFor;
        fieldToLookFor = Arrays.stream(fields)
                .filter(structField -> StringUtils.equalsIgnoreCase(fieldName, structField.name()))
                .findFirst();
        return fieldToLookFor;
    }
}
