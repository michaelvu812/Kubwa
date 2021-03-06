package com.wesleyelliott.kubwa.rule;

import android.text.TextUtils;

/**
 * Created by wesley on 2016/07/28.
 */

public class RegexRule extends Rule<String> {

    private String regex;

    public RegexRule(String regex) {
        super(String.class);
        this.regex = regex;
    }

    protected String getRegex() {
        return regex;
    }

    @Override
    public boolean isValid(String value) {
        return !TextUtils.isEmpty(value) && value.matches(getRegex());
    }
}
