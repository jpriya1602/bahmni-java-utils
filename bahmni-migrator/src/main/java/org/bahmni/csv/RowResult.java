package org.bahmni.csv;

import org.apache.commons.lang.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class RowResult<T extends CSVEntity> {
    private Messages messages;
    private T csvEntity;

    public RowResult(T csvEntity) {
        this(csvEntity, (Messages)null);
    }

    public RowResult(T csvEntity, Throwable exception) {
        this(csvEntity, exception.getMessage());
    }

    public RowResult(T csvEntity, String errorMessage) {
        this(csvEntity, new Messages(errorMessage));
    }

    public RowResult(T csvEntity, Messages errorMessage) {
        this.csvEntity = csvEntity;
        this.messages = errorMessage;
    }

    public boolean isSuccessful() {
        return getMessages().isEmpty();
    }

    public String[] getRowWithErrorColumn() {
        if (csvEntity == null)
            return new String[] {};

        return csvEntity.getRowWithErrorColumn(messages.asString());
    }

    public String getRowWithErrorColumnAsString() {
        return StringUtils.join(getRowWithErrorColumn(), ",");
    }

    private Messages getMessages(){
        if (this.messages == null) this.messages = new Messages();
        return this.messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RowResult rowResult = (RowResult) o;

        if (csvEntity != null ? !csvEntity.equals(rowResult.csvEntity) : rowResult.csvEntity != null) return false;
        if (messages != null ? !messages.equals(rowResult.messages) : rowResult.messages != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messages != null ? messages.hashCode() : 0;
        result = 31 * result + (csvEntity != null ? csvEntity.hashCode() : 0);
        return result;
    }

    public T getCsvEntity() {
        return csvEntity;
    }
}
