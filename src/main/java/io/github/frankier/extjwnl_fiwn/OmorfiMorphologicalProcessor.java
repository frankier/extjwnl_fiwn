package io.github.frankier.extjwnl_fiwn;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.dictionary.MorphologicalProcessor;
import net.sf.extjwnl.util.factory.Param;
import net.sf.hfst.NoTokenizationException;
import com.github.flammie.omorfi.Omorfi;

public class OmorfiMorphologicalProcessor implements MorphologicalProcessor
{
    private Dictionary dictionary;
    private Omorfi omorfi;
    private static final Map<String, POS> posMap = new HashMap<String, POS>();
    static {
        posMap.put("ADJ", POS.ADJECTIVE);
        posMap.put("VERB", POS.VERB);
        //posMap.put("AUX", POS.VERB);
        posMap.put("ADV", POS.ADVERB);
        //posMap.put("PRON", POS.NOUN);
        //posMap.put("NUM", POS.ADJECTIVE);
        posMap.put("ADP", POS.ADVERB);
        posMap.put("SCONJ", POS.ADVERB);
        posMap.put("SYM", POS.NOUN);
    }
    private static final Pattern lemmaExtract = Pattern.compile("(.*)(_\\d+)?");

    class LemmaPos implements Comparable<LemmaPos>
    {
	String lemma;
	POS pos;
	int cmpLen;
	float weight;

	@Override
	public int compareTo(LemmaPos other) {
	    int cl = Integer.compare(this.cmpLen, other.cmpLen);
	    return cl == 0 ? Float.compare(this.weight, other.weight) : cl;
	}
    }

    public OmorfiMorphologicalProcessor(Dictionary dictionary, Map<String, Param> params) throws IOException
    {
	this.dictionary = dictionary;

	this.omorfi = new Omorfi();
	this.omorfi.loadAll();
    }

    public List<String> lookupAllBaseForms(POS pos, String derivation)
    {
	Collection<String> strAnalyses;
	try {
	    strAnalyses = this.omorfi.analyse(derivation);
	} catch (NoTokenizationException e) {
	    return Arrays.asList(derivation);
	}
	List<LemmaPos> analyses = strAnalyses.stream().flatMap(strAnaly -> {
	    List<List<String>> bits = StreamSupport.stream(
		Splitter
		.on("][")
		.split(strAnaly.subSequence(1, strAnaly.length() - 1))
		.spliterator(),
		false
	    )
	    .map(kv -> Splitter.on('=').limit(2).splitToList(kv))
	    .collect(Collectors.toList());
	    List<LemmaPos> lemmaPoses = new ArrayList();
	    int cmpLen = 0;
	    float weight = Float.POSITIVE_INFINITY;
	    for (List<String> bit: bits) {
		if (bit.get(0) == "WORD_ID") {
		    LemmaPos lp = new LemmaPos();
		    lp.lemma = this.lemmaExtract.matcher(bit.get(1)).group(1);
		    lemmaPoses.add(lp);
		    cmpLen++;
		} else if (bit.get(0) == "UPOS") {
		    lemmaPoses.get(lemmaPoses.size() - 1).pos = this.posMap.get(bit.get(1));
		} else if (bit.get(0) == "WEIGHT") {
		    weight = Float.parseFloat(bit.get(1));
		}
	    }
	    for (LemmaPos lp: lemmaPoses) {
		lp.cmpLen = cmpLen;
		lp.weight = weight;
	    }
	    return lemmaPoses.stream();
	}).collect(Collectors.toList());
	// Filter by pos
	List<String> filteredAnalyses = analyses.stream()
	    .filter(analy -> analy.pos == pos)
	    .map(analy -> analy.lemma)
	    .collect(Collectors.toList());
	if (!filteredAnalyses.isEmpty()) {
	    return filteredAnalyses;
	} else {
	    // Make sure not to discard last match
	    return analyses.stream()
		.map(analy -> analy.lemma)
		.collect(Collectors.toList());
	}
    }

    public IndexWord lookupBaseForm(POS pos, String derivation) throws JWNLException
    {
	List<String> forms = lookupAllBaseForms(pos, derivation);
	if (forms.isEmpty()) {
	    return null;
	} else {
	    return this.dictionary.getIndexWord(pos, forms.get(0));
	}
    }
}
