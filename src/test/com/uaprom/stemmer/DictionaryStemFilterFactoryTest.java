package com.uaprom.stemmer;

import java.io.File;
import java.io.Reader;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import org.apache.solr.core.SolrResourceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uaprom.stemmer.DictionaryStemFilterFactory;


public class DictionaryStemFilterFactoryTest {
    private final static Logger logger = LoggerFactory.getLogger(DictionaryStemFilterFactoryTest.class);
    
    public static void main(String[] args) throws IOException {
        DictionaryStemFilterFactoryTest test = new DictionaryStemFilterFactoryTest();
        
        // testSQLiteDictionary();
        // test.testSQLiteStemFilterFactory();
        test.test();
    }

    public void test() throws IOException {
        SolrResourceLoader loader = new SolrResourceLoader(null);
        Map<String, String> params = new HashMap<String,String>();

        params.put("luceneMatchVersion", "LUCENE_43");
        params.put("dictionaryClass", "com.uaprom.stemmer.pymorphy2.Pymorphy2Dictionary");
        params.put("pymorphy2DBPath", "dict");
        params.put("pymorphy2Replaces", "replaces.json");
        DictionaryStemFilterFactory filterFactory = new DictionaryStemFilterFactory(params);
        // filterFactory.setLuceneMatchVersion(Version.LUCENE_40);
        // filterFactory.init(params);
        filterFactory.inform(loader);
        
        Map<String, String> args = new HashMap<String,String>();
        args.put("luceneMatchVersion", "LUCENE_43");
        WhitespaceTokenizerFactory tokenizerFactory = new WhitespaceTokenizerFactory(args);

        args.put("luceneMatchVersion", "LUCENE_43");
        WordDelimiterFilterFactory wordDelimiterFactory = new WordDelimiterFilterFactory(args);

        args.put("luceneMatchVersion", "LUCENE_43");
        LowerCaseFilterFactory lowerCaseFactory = new LowerCaseFilterFactory(args);
        
        Tokenizer tokenizer = tokenizerFactory.create(getTextReader(loader, "text.txt"));
        TokenStream wordDelimiterFilter = wordDelimiterFactory.create(tokenizer);
        TokenStream lowerCaseFilter = lowerCaseFactory.create(wordDelimiterFilter);
        TokenStream stemFilter = filterFactory.create(lowerCaseFilter);

        long startTime, endTime;
        // 1
        int cycles = 1;
        for (int i = 0; i < cycles; i++) {
            startTime = System.currentTimeMillis();
            tokenizer.setReader(getTextReader(loader, "text.txt"));
            stemFilter.reset();
            List<String> tokens = testFilter(stemFilter);
            logger.debug(String.format("Tokens: %s", tokens.size()));
            endTime = System.currentTimeMillis();
            logger.debug("Time: {}", endTime - startTime);
            logger.debug("=================");
        }
   }

    public static List<String> testFilter(TokenStream stream) throws IOException {
        List<String> tokens = new ArrayList<String>();
        CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncAtt = stream.getAttribute(PositionIncrementAttribute.class);
        int pos = 0;
        stream.reset();
        while(stream.incrementToken()) {
            // logger.debug(posIncAttr.getPositionIncrement());
            if (posIncAtt.getPositionIncrement() == 1) pos++;
            tokens.add(termAtt.toString());
            System.out.println(String.format("%s: %s", pos, termAtt.toString()));
        }
        System.out.flush();

        return tokens;
    }

    private Reader getTextReader(ResourceLoader loader, String filename) throws IOException {
        return new BufferedReader(new InputStreamReader(loader.openResource(filename)));
    }
}
