package com.uaprom.stemmer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DictionaryStemmer {
    private final static Logger logger = LoggerFactory.getLogger(DictionaryStemmer.class);
    
    protected Dictionary dict;
    
    protected int MAX_PREFIX_LENGTH = 6;
    protected int MAX_WORD_WITHOUT_PREFIX_LENGTH = 3;
        
    public DictionaryStemmer(Dictionary dict) {
        this.dict = dict;
    }
        
    public ArrayList<String> getNormalForms(String word) throws IOException {
        return getNormalForms(word.toCharArray(), 0, word.length());
    }
    
    public ArrayList<String> getNormalForms(char[] word, int offset, int count) throws IOException {
        // logger.info("DictionaryStemmer::getNormalForms");
        // logger.info(new String(word, offset, count));

        ArrayList<String> normalForms = dict.getNormalForms(word, offset, count);

        if (normalForms.isEmpty()) {
            for (int i = 1; i <= MAX_PREFIX_LENGTH && count - i >= MAX_WORD_WITHOUT_PREFIX_LENGTH; i++) {
                normalForms = dict.getNormalForms(word, offset + i, count - i);
                
                if (!normalForms.isEmpty()) {
                    normalForms = addPrefix(normalForms, new String(word, 0, i));
                    break;
                }
            }
        }

        if (normalForms.isEmpty()) {
            normalForms = new ArrayList<String>();
            normalForms.add(new String(word, offset, count));
        }

        return normalForms;
    }

    protected ArrayList<String> addPrefix(ArrayList<String> words, String prefix) {
        ArrayList<String> prefixedWords = new ArrayList<String>();
        
        for (String word : words) {
            prefixedWords.add(prefix + word);
        }
        
        return prefixedWords;
    }
}
