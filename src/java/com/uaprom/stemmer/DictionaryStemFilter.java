package com.uaprom.stemmer;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import com.uaprom.stemmer.DictionaryStemmer;


public class DictionaryStemFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
    private DictionaryStemmer stemmer;
    private boolean omitNumbers;
        
    private List<String> buffer;
    private State savedState;
     
    public DictionaryStemFilter(TokenStream input, DictionaryStemmer stemmer) {
        this(input, stemmer, true);
    }
    
    public DictionaryStemFilter(TokenStream input, DictionaryStemmer stemmer, boolean omitNumbers) {
        super(input);
        this.stemmer = stemmer;
        this.omitNumbers = omitNumbers;
    }
        
    @Override
    public boolean incrementToken() throws IOException {
        if (buffer != null && !buffer.isEmpty()) {
            String nextStem = buffer.remove(0);
            restoreState(savedState);
            posIncAtt.setPositionIncrement(0);
            termAtt.copyBuffer(nextStem.toCharArray(), 0, nextStem.length());
            termAtt.setLength(nextStem.length());
            return true;
        }
    
        if (!input.incrementToken()) {
            return false;
        }
    
        if (keywordAtt.isKeyword()) {
            return true;
        }

        char[] termBuffer = termAtt.buffer();
        int termLength = termAtt.length();
        
        if (omitNumbers) {
            for (int i = 0; i < termLength; i++) {
                if (Character.isDigit(termBuffer[i])) {
                    return true;
                }
            }
        }
        
        buffer = stemmer.getNormalForms(termBuffer, 0, termLength);

        if (buffer.isEmpty()) { // we do not know this word, return it unchanged
            return true;
        }     

        String stem = buffer.remove(0);
        termAtt.copyBuffer(stem.toCharArray(), 0, stem.length());
        termAtt.setLength(stem.length());

        if (!buffer.isEmpty()) {
            savedState = captureState();
        }

        return true;
    }
}
