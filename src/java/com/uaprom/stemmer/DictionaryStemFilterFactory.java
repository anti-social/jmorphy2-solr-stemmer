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
    private boolean omitNumbers = true;

    private static final String DEFAULT_DICTIONARY_CLASSPATH =
        "com.uaprom.stemmer.pymorphy2.Pymorphy2Dictionary";
    
    public void inform(ResourceLoader loader) throws IOException {
        assureMatchVersion();
            
        String dictionaryClassPath = args.get("dictionaryClass");
        if (dictionaryClassPath == null) {
            // throw new IllegalArgumentException("You must specify type of a dictionary");
            dictionaryClassPath = DEFAULT_DICTIONARY_CLASSPATH;
        }

        Class<? extends Dictionary> dictionaryClass = ((SolrResourceLoader)loader).findClass(dictionaryClassPath, Dictionary.class);
        String configDir = ((SolrResourceLoader)loader).getConfigDir();        
        try {
            dictionary = (Dictionary)dictionaryClass.getDeclaredConstructor(Map.class, String.class).newInstance(args, configDir);
        }
        catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                                    String.format("Error instantiating class: '%s'", dictionaryClassPath),
                                    e);
        }
        
        if (dictionary == null) {
            throw new IllegalArgumentException(String.format("Cannot find '%s' dictionary class", dictionaryClassPath));
        }

        String omitNumbers = args.get("omitNumbers");
        if (omitNumbers != null && omitNumbers.equalsIgnoreCase("false")) {
            this.omitNumbers = false;
        }
    }

    public TokenStream create(TokenStream tokenStream) {
        return new DictionaryStemFilter(tokenStream, new DictionaryStemmer(dictionary), omitNumbers);
    }
}
