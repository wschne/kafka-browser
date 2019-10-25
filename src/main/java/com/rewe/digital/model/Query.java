package com.rewe.digital.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.EMPTY;

@Data
@AllArgsConstructor
public class Query {
    private String query;

    public String getTopic() {
        final Pattern p = Pattern.compile("(?i)(from)\\s*(?=\\S*['-]?)([a-zA-Z\\d_'-]+)*");
        final Matcher m = p.matcher(query);
        if (m.find()) {
            return m.group(2);
        }
        return EMPTY;
    }

    public String getFlattenedQuery() {
        return StringUtils.replace(query, "\n", " ");
    }

    public String getNormalizedTopicName() {
        val topicName = this.getTopic();
        return topicName.replace("-", "_");
    }

    public String getNormalizedQuery() {
        return this.query.replace(getTopic(), getNormalizedTopicName());
    }

}
