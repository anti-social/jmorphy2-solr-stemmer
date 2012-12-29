jmorphy2-solr-stemmer
=====================

Solr stemmer that uses pymorphy2 dictionaries

How to build
------------

```sh
# first compile solr
git clone https://github.com/apache/lucene-solr.git
cd lucene-solr
git checkout lucene_solr_4_0_0
ant ivy-bootstrap
cd solr
ant example
cd ../..

# compile stemmer
git clone https://github.com/anti-social/jmorphy2-solr-stemmer.git
cd jmorphy2-solr-stemmer
ant compile
```

Then you can find compiled jar file in the solr/lib directory.
Copy jar file into lib directory of your solr home.

Compile dictionary
------------------

```sh
pip install https://github.com/kmike/pymorphy2/archive/master.zip
pymorphy dict download_xml db.opencorpora.xml
# next command takes time and almost 4Gb memory
pymorphy dict compile db.opencorpora.xml
```

Copy dict directory into conf directory of your solr home.

Schema example
-----------

```xml
<fieldType name="text" class="solr.TextField" omitNorms="true">
  <analyzer>
    <charFilter class="solr.MappingCharFilterFactory" mapping="mapping_ru.txt"/>
    <charFilter class="solr.HTMLStripCharFilterFactory"/>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.StopFilterFactory" words="stopwords.txt" ignoreCase="true"/>
    <filter class="solr.StopFilterFactory" words="stopwords_ru.txt" ignoreCase="true"/>
    <filter class="solr.WordDelimiterFilterFactory" generateWordParts="1" generateNumberParts="1"
            catenateWords="1" catenateNumbers="1" catenateAll="0"/>
    <filter class="solr.LowerCaseFilterFactory"/>

    <filter class="com.uaprom.stemmer.DictionaryStemFilterFactory"
            dictionaryClass="com.uaprom.stemmer.pymorphy2.Pymorphy2Dictionary"
            pymorphy2DBPath="dict"/>
  </analyzer>
</fieldType>
```
