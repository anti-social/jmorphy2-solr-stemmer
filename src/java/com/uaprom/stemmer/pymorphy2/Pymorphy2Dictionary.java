package com.uaprom.stemmer.pymorphy2;

import java.io.File;
import java.io.DataInput;
import java.io.FileReader;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.input.SwappedDataInputStream;
import org.apache.commons.codec.binary.Base64;

import org.apache.noggit.JSONParser;

import com.uaprom.stemmer.Dictionary;


public class Pymorphy2Dictionary extends Dictionary {
    private static final Logger logger = LoggerFactory.getLogger(Pymorphy2Dictionary.class);
    
    private DAWG dawg;
    private ArrayList<Paradigm> paradigms;
    private String[] suffixes;
    private String[] lemmaPrefixes;
    
    private static final String WORDS_FILENAME = "words.dawg";
    private static final String PARADIGMS_FILENAME = "paradigms.array";
    private static final String SUFFIXES_FILENAME = "suffixes.json";
    private static final String LEMMA_PREFIXES_FILENAME = "lemma-prefixes.json";

    public Pymorphy2Dictionary(Map<String,String> args, String baseDir) throws IOException {
        // logger.info("Pymorphy2Dictionary::Pymorphy2Dictionary");
        
        String dbPath = args.get("pymorphy2DBPath");
        if (dbPath == null) {
            dbPath = "dict";
        }
        dbPath = baseDir + dbPath;
        
        dawg = new DAWG(new File(dbPath, WORDS_FILENAME));

        loadParadigms(new File(dbPath, PARADIGMS_FILENAME));
        loadSuffixes(new File(dbPath, SUFFIXES_FILENAME));
        loadLemmaPrefixes(new File(dbPath, LEMMA_PREFIXES_FILENAME));
    }

    private void loadParadigms(File file) throws IOException {
        DataInput paradigmsStream = new SwappedDataInputStream(new FileInputStream(file));
        short paradigmsCount = paradigmsStream.readShort();
        paradigms = new ArrayList<Paradigm>(paradigmsCount);
        // logger.info(paradigmsCount);
        for (int paraId = 0; paraId < paradigmsCount; paraId++) {
            paradigms.add(new Paradigm(paradigmsStream));
        }
        // paradigms = new short[paradigmsCount];
    }

    private String[] readJsonStrings(File file) throws IOException {
        ArrayList<String> stringList = new ArrayList<String>();
        String[] stringArray;

        JSONParser parser = new JSONParser(new FileReader(file));
        int event;
        while ((event = parser.nextEvent()) != JSONParser.EOF) {
            if (event == JSONParser.STRING) {
                stringList.add(parser.getString());
            }
        }
        
        stringArray = new String[stringList.size()];
        return stringList.toArray(stringArray);
    }

    private void loadSuffixes(File file) throws IOException {
        suffixes = readJsonStrings(file);
    }

    private void loadLemmaPrefixes(File file) throws IOException {
        lemmaPrefixes = readJsonStrings(file);
    }

    @Override
    public ArrayList<String> getNormalForms(char[] word, int offset, int count) throws IOException {
        ArrayList<String> normalForms = new ArrayList<String>();
        String w = new String(word, offset, count);

        ArrayList<FoundParadigm> paradigmValues;
        paradigmValues = dawg.similarItems(w.getBytes("UTF-8"));
            
        HashSet<String> uniqueNormalForms = new HashSet<String>();
        for (FoundParadigm paradigmValue : paradigmValues) {
            String nf = buildNormalForm(paradigmValue.paradigmId, paradigmValue.idx, w);
            if (!uniqueNormalForms.contains(nf)) {
                normalForms.add(nf.toLowerCase());
                uniqueNormalForms.add(nf);
            }
        }
        
        return normalForms;
    }

    protected String buildNormalForm(short paradigmId, short idx, String word) {
        // logger.info("Pymorphy2Dictionary::buildNormalForm");
        // logger.info(idx);

        Paradigm paradigm = paradigms.get(paradigmId);
        int paradigmLength = paradigm.paradigm.length / 3;
        String stem = buildStem(paradigm.paradigm, idx, word);
        // logger.info(stem);

        int prefixId = paradigm.paradigm[paradigmLength * 2 + 0] & 0xFFFF;
        int suffixId = paradigm.paradigm[0] & 0xFFFF;

        String prefix = lemmaPrefixes[prefixId];
        String suffix = suffixes[suffixId];
        
        return prefix + stem + suffix;
    }

    protected String buildStem(short[] paradigm, short idx, String word) {
        // logger.info("Pymorphy2Dictionary::buildStem");

        int paradigmLength = paradigm.length / 3;
        // logger.info(paradigmLength);

        int prefixId = paradigm[paradigmLength * 2 + idx] & 0xFFFF;
        // logger.info(prefixId);
        String prefix = lemmaPrefixes[prefixId];
        // logger.info(prefix);

        int suffixId = paradigm[idx] & 0xFFFF;
        // logger.info(suffixId);
        String suffix = suffixes[suffixId];
        // logger.info(suffix);

        if (!suffix.equals("")) {
            return word.substring(prefix.length(), word.length() - suffix.length());
        }
        else {
            return word.substring(prefix.length());
        }
    }

    private class DAWG {
        private static final byte PAYLOAD_SEPARATOR = 0x01;

        private DAWGDict dict;
        private Guide guide;

        public DAWG(File file) throws IOException {
            DataInput input = new SwappedDataInputStream(new BufferedInputStream(new FileInputStream(file)));
            dict = new DAWGDict(input);
            guide = new Guide(input);
        }

        public ArrayList<FoundParadigm> similarItems(byte[] key) throws IOException {
            // logger.info("DAWG::similarItems");
            
            ArrayList<FoundParadigm> res = new ArrayList<FoundParadigm>();
            int index = 0;
            
            for (int i = 0; i < key.length; i++) {
                index = dict.followByte(key[i], index);
                if (index == -1) {
                    return res;
                }
                // logger.info(index);
            }
            
            // logger.info(">>>");
            // logger.info(index);
            if (index == -1) {
                return res;
            }

            index = dict.followByte(PAYLOAD_SEPARATOR, index);
            // logger.info(index);
            if (index == -1) {
                return res;
            }

            return valueForIndex(index);
        }

        public ArrayList<FoundParadigm> valueForIndex(int index) throws IOException {
            // logger.info("DAWG::valueForIndex");
            ArrayList<FoundParadigm> values = new ArrayList<FoundParadigm>();

            Completer completer = new Completer(dict, guide);
            
            completer.start(index);
            while (completer.next()) {
                // ArrayList<Byte> data = completer.getKey();
                byte[] encodedData = completer.getKey();
                // logger.info(encodedData);
                
                // byte[] encodedBytes = new byte[data.size()];
                // for (int i = 0; i < data.size(); i++) {
                //     encodedBytes[i] = data.get(i);
                // }
                Base64 base64 = new Base64();
                byte[] decodedData = base64.decodeBase64(encodedData);
                // logger.info(decodedData);
                // logger.info(decodedData.length);

                DataInput stream = new DataInputStream(new ByteArrayInputStream(decodedData));
                short paradigmId = stream.readShort();
                short idx = stream.readShort();
                values.add(new FoundParadigm(paradigmId, idx));
            }

            return values;
        }
    };

    private class DAWGDict {
        private static final int PRECISION_MASK = 0xFFFFFFFF;

        private static final int HAS_LEAF_BIT = 1 << 8;
        private static final int EXTENSION_BIT = 1 << 9;
        private static final int OFFSET_MAX = 1 << 21;
        private static final int IS_LEAF_BIT = 1 << 31;
        
        // private Units units;
        private int[] units;

        public DAWGDict(DataInput input) throws IOException {
            // logger.info("DAWGDict::DAWGDict");
            
            int size = input.readInt();
            // logger.info(size);
            units = new int[size];
            for (int i = 0; i < size; i++) {
                units[i] = input.readInt();
            }
        }

        public boolean contains(byte[] key) {
            int index = followBytes(key, 0);
            if (index == -1) {
                return false;
            }
            
            return true;
        }

        public int followBytes(byte[] key, int index) {
            for (int i = 0; i < key.length; i++) {
                index = followByte(key[i], index);
                if (index == -1) {
                    return -1;
                }
            }

            return index;
        }
        
        public int followByte(byte c, int index) {
            int o = offset(units[index]);
            int nextIndex = (index ^ o ^ (c & 0xFF)) & PRECISION_MASK;

            if (label(units[nextIndex]) != (c & 0xFF)) {
                return -1;
            }

            return nextIndex;
        }

        public boolean hasValue(int index) {
            return hasLeaf(units[index]);
        }

        protected int offset(int base) {
            return ((base >> 10) << ((base & EXTENSION_BIT) >> 6)) & PRECISION_MASK;
        }

        protected int label(int base) {
            return base & (IS_LEAF_BIT | 0xFF) & PRECISION_MASK;
        }

        protected int value(int base) {
            return base & ~IS_LEAF_BIT & PRECISION_MASK;
        }

        protected boolean hasLeaf(int base) {
            return (base & HAS_LEAF_BIT & PRECISION_MASK) != 0;
        }
    };

    private class Guide {
        private byte[] units;
        
        public Guide(DataInput input) throws IOException {
            // logger.info("Guide::Guide");
            
            int baseSize = input.readInt();
            // logger.info(baseSize);

            int size = baseSize * 2;
            units = new byte[size];
            for (int i = 0; i < size; i++) {
                units[i] = input.readByte();
            }
        }

        public byte child(int index) {
            return units[index * 2];
        }

        public byte sibling(int index) {
            return units[index * 2 + 1];
        }

        public int size() {
            return units.length;
        }
    };

    private class Completer {
        private DAWGDict dict;
        private Guide guide;

        private byte[] key;
        private int keyLength;
        private ArrayList<Integer> indexStack;
        private int lastIndex;

        private static final int INITIAL_KEY_LENGTH = 9;

        public Completer(DAWGDict dict, Guide guide) {
            this.dict = dict;
            this.guide = guide;
        }

        public void start(int index) {
            key = new byte[INITIAL_KEY_LENGTH];
            keyLength = 0;
            indexStack = new ArrayList<Integer>();
            indexStack.add(index);
            lastIndex = 0;
        }

        public boolean next() {
            int index = indexStack.get(indexStack.size() - 1);

            if (lastIndex != 0) {
                byte childLabel = guide.child(index);

                if (childLabel != 0) {
                    index = follow(childLabel, index);
                    if (index == -1) {
                        return false;
                    }
                }
                else {
                    while (true) {
                        byte siblingLabel = guide.sibling(index);

                        if (keyLength > 0) {
                            keyLength--;
                        }

                        indexStack.remove(indexStack.size() - 1);
                        if (indexStack.isEmpty()) {
                            return false;
                        }

                        index = indexStack.get(indexStack.size() - 1);
                        if (siblingLabel != 0) {
                            index = follow(siblingLabel, index);
                            if (index == -1) {
                                return false;
                            }

                            break;
                        }
                    }
                }
            }

            return findTerminal(index);
        }

        public byte[] getKey() {
            // logger.info("Completer::getKey - {}", keyLength);
            
            byte[] newKey = new byte[keyLength];
            System.arraycopy(key, 0, newKey, 0, keyLength);
            return newKey;
        }
        
        private int follow(byte label, int index) {
            int nextIndex = dict.followByte(label, index);
            if (nextIndex == -1) {
                return -1;
            }

            addLabel(label);
            indexStack.add(nextIndex);
            return nextIndex;
        }

        private boolean findTerminal(int index) {
            while (!dict.hasValue(index)) {
                byte label = guide.child(index);

                index = dict.followByte(label, index);
                if (index == -1) {
                    return false;
                }

                addLabel(label);
                indexStack.add(index);
            }

            lastIndex = index;
            return true;
        }

        private void addLabel(byte label) {
            if (keyLength == key.length) {
                byte[] newKey = new byte[key.length * 2];
                System.arraycopy(key, 0, newKey, 0, key.length);
                key = newKey;
            }
            
            key[keyLength] = label;
            keyLength++;
        }
    };

    private class Paradigm {
        private short[] paradigm;

        public Paradigm(DataInput input) throws IOException {
            short length = input.readShort();
            paradigm = new short[length];
            for (int i = 0; i < length; i++) {
                paradigm[i] = input.readShort();
            }
        }

        public short[] getParadigm() {
            return paradigm;
        }
    };

    private class FoundParadigm {
        private short paradigmId;
        private short idx;

        public FoundParadigm(short paradigmId, short idx) {
            this.paradigmId = paradigmId;
            this.idx = idx;
        }
    };
    
    // private class Units {
    //     private int size;
    //     private int[] units;

    //     public Units(DataInput input) {
    //     }
    // }
}
