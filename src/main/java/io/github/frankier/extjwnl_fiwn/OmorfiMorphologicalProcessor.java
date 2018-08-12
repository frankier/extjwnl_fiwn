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

    public OmorfiMorphologicalProcessor(Dictionary dictionary, Map<String, Param> params) throws IOException
    {
	this.dictionary = dictionary;

	this.omorfi = new Omorfi();
	this.omorfi.loadAll();
    }

    static public List<String> analysToForms(List<Map<String, String>> analyses)
    {
	Comparator<Map<String, String>> cmp = Comparator.comparing(analy -> Float.parseFloat(analy.get("WEIGHT")));
	analyses.sort(cmp);
	return analyses.stream().map(analy ->
	    analy.get("WORD_ID")
	).collect(Collectors.toList());
    }

    public List<String> lookupAllBaseForms(POS pos, String derivation)
    {
	Collection<String> strAnalyses;
	try {
	    strAnalyses = this.omorfi.analyse(derivation);
	} catch (NoTokenizationException e) {
	    return Arrays.asList(derivation);
	}
	List<Map<String, String>> analyses = strAnalyses.stream().map(strAnaly ->
	    Splitter.on("][")
		.withKeyValueSeparator(
		    Splitter.on('=')
			.limit(2))
		.split(strAnaly.subSequence(1, strAnaly.length() - 1))
	).collect(Collectors.toList());
	// Filter by pos
	List<Map<String, String>> filteredAnalyses = analyses.stream()
	    .filter(analy -> this.posMap.get(posMap.get(analy.get("UPOS"))) == pos)
	    .collect(Collectors.toList());
	if (!filteredAnalyses.isEmpty()) {
	    return analysToForms(filteredAnalyses);
	} else {
	    // Make sure not to discard last match
	    return analysToForms(analyses);
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
