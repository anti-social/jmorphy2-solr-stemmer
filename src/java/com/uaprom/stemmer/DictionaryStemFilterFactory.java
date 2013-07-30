package com.uaprom.stemmer;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.common.SolrException;


public class DictionaryStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    private Dictionary dictionary = null;
    private boolean omitNumbers;
    private String dictionaryClassPath;

    private static final String DEFAULT_DICTIONARY_CLASSPATH =
        "com.uaprom.stemmer.pymorphy2.Pymorphy2Dictionary";

    public DictionaryStemFilterFactory(Map<String,String> args) {
        super(args);
        assureMatchVersion();
        dictionaryClassPath = get(args, "dictionaryClass", DEFAULT_DICTIONARY_CLASSPATH);
        omitNumbers = getBoolean(args, "omitNumbers", true);
    }

    public void inform(ResourceLoader loader) throws IOException {
        String configDir = ((SolrResourceLoader)loader).getConfigDir();
        dictionary = ((SolrResourceLoader)loader).newInstance(dictionaryClassPath, Dictionary.class);
        dictionary.init(getOriginalArgs(), configDir);

        if (dictionary == null) {
            throw new IllegalArgumentException(String.format("Cannot find '%s' dictionary class", dictionaryClassPath));
        }
    }

    public TokenStream create(TokenStream tokenStream) {
        return new DictionaryStemFilter(tokenStream, new DictionaryStemmer(dictionary), omitNumbers);
    }
}
