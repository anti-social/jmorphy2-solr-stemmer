package com.uaprom.stemmer;

import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;


abstract public class Dictionary {
    abstract public void init(Map<String,String> args, String baseDir) throws IOException;

    abstract public ArrayList<String> getNormalForms(char[] word, int offset, int count) throws IOException;

    public ArrayList<String> getNormalForms(String word) throws IOException {
        return getNormalForms(word.toCharArray(), 0, word.length());
    }
}
