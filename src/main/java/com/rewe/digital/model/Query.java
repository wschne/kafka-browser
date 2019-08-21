package com.rewe.digital.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.EMPTY;

public class Query {
    private String query;

    public Query(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query.replaceAll("-", "_");
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTopic() {
        final Pattern p = Pattern.compile("(?i)(from)\\s*(?=\\S*['-]?)([a-zA-Z\\d_'-]+)*");
        final Matcher m = p.matcher(query);
        if (m.find()) {
            return m.group(2);
        }
        return EMPTY;
    }
}
