package com.uaprom.stemmer.pymorphy2;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.common.SolrException;

import com.uaprom.stemmer.DictionaryStemmer;
import com.uaprom.stemmer.DictionaryStemFilter;


public class Pymorphy2DictionaryStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    // TODO: load metadata
    private static final String META_FILENAME = "meta.json";
    private static final String WORDS_FILENAME = "words.dawg";
    private static final String PARADIGMS_FILENAME = "paradigms.array";
    private static final String SUFFIXES_FILENAME = "suffixes.json";
    private static final String LEMMA_PREFIXES_FILENAME = "lemma-prefixes.json";

    private Pymorphy2Dictionary dictionary = null;
    private String dbPath;
    private String replacesFilename;
    private boolean omitNumbers;

    public Pymorphy2DictionaryStemFilterFactory(Map<String,String> args) {
        super(args);
        assureMatchVersion();
        dbPath = args.get("db");
        if (dbPath == null) {
            dbPath = "dict";
        }

        replacesFilename = args.get("replaces");
        omitNumbers = getBoolean(args, "omitNumbers", true);
    }

    public void inform(ResourceLoader loader) throws IOException {
        InputStream dawg = loader.openResource((new File(dbPath, WORDS_FILENAME)).getPath());
        InputStream paradigms = loader.openResource((new File(dbPath, PARADIGMS_FILENAME)).getPath());
        InputStream suffixes = loader.openResource((new File(dbPath, SUFFIXES_FILENAME)).getPath());
        InputStream lemmaPrefixes = loader.openResource((new File(dbPath, LEMMA_PREFIXES_FILENAME)).getPath());

        InputStream replaces = null;
        if (replacesFilename != null) {
            replaces = loader.openResource(replacesFilename);
        }

        dictionary = new Pymorphy2Dictionary(dawg, paradigms, suffixes, lemmaPrefixes, replaces);
    }

    public TokenStream create(TokenStream tokenStream) {
        return new DictionaryStemFilter(tokenStream, new DictionaryStemmer(dictionary), omitNumbers);
    }
}
