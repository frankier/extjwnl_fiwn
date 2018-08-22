# extjwnl_fiwn

This repository contains Java code to make
[extJWNL](https://github.com/extjwnl/extjwnl) interoperate with
[FinnWordNet](http://www.ling.helsinki.fi/en/lt/research/finnwordnet/) and the
Finnish language.

## Installation

You can add this Java library to your project by following [the instructions on
jitpack.io](https://jitpack.io/#frankier/extjwnl_fiwn).

## Usage

This library provides two classes, `FiWNFileDictionaryElementFactory` which is
a tiny shim to make extJWNL able to use FinnWordNet's dictionary files and
`OmorfiMorphologicalProcessor`, which is a `extJWNL` `MorphologicalProcessor`
which will lemmatize Finnish word forms. The latter should only be used keeping
in mind it may produce poor results due to morphological ambiguity, and thus in
cases where words to be looked up occur in some context, finding a way to
integrate a disambiguating morphological analyser such as
[FinnPOS](https://github.com/mpsilfve/FinnPos) or
[Finnish-dep-parser](https://github.com/TurkuNLP/Finnish-dep-parser) should be
preferred.

An example of a working XML configuration (you'll need to change the path to
FinnWordNet -- which occurs twice):

```
<?xml version="1.0" encoding="UTF-8"?>  
<jwnl_properties language="en">  
        <version publisher="University of Helsinki" number="2.0"
                 path="file:/path/to/fiwn/data/dict/" language="fi"/>  
        <dictionary class="net.sf.extjwnl.dictionary.FileBackedDictionary">  
                <param name="morphological_processor" value="io.github.frankier.extjwnl_fiwn.OmorfiMorphologicalProcessor"/>
                <param name="dictionary_element_factory" value="io.github.frankier.extjwnl_fiwn.FiWNFileDictionaryElementFactory"/>
                <param name="file_manager" value="net.sf.extjwnl.dictionary.file_manager.FileManagerImpl">  
                        <param name="file_type" value="net.sf.extjwnl.princeton.file.PrincetonRandomAccessDictionaryFile">
                                <param name="encoding" value="UTF-8"/>
                        </param>
                        <param name="dictionary_path" value="file:/path/to/fiwn/data/dict/"/>  
                </param>  
        </dictionary>  
        <resource class="net.sf.extjwnl.princeton.PrincetonResource"/>
</jwnl_properties>  
```
