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
