---
title: 'Panako: a scalable audio search system'
tags:
- Acoustic fingerprinting
- Music Information Retreival
- Java software
authors:
- name: Joren Six
  orcid: 0000-0001-7671-1907
  affiliation: 1
affiliations:
- name: IPEM, Ghent University, Belgium
  index: 1
date: "13 June 2021"
bibliography: paper.bib
---


# Summary

`Panako` solves the problem of finding short audio fragments in large digital audio archives. The content based audio search algorithm implemented in Panako is able to identify a short audio query in large database of thousands of hours of audio using an acoustic fingerprinting technique. 

![A general acoustic fingerprinting system. Features are extracted from audio and combined into fingerprints. The fingerprints are matched with fingerprints in a reference database. Finally a match is reported.\label{fig:general}](resources/media/general_acoustic_fingerprinting_schema.png){width=90%}

The dataflow in Panako closely resembles the flow depicted in \autoref{fig:general}. Audio is transformed to features which are grouped in recognizable fingerprints. The fingerprints are compared with a database of reference fingerprints. If a match is found it is reported or, in case of a true negative, the system returns that the audio is not present in the database. The properties of the acoustic fingerprinting system mainly depend on the selection of features, the information captured by the fingerprints and the performance of the matching step.

There are three algorithms implemented in Panako. The first is based on pitch class histograms @six2012ethnic_fingerprinter. The other two are based on peaks in a spectral representation: a baseline algorithm [@Wang2003a;@six2020olaf] and the main Panako algorithm [@wang2003patent;@six2014panako;@six2021panakovtwo].

The main algorithm in Panako finds matches between a short query fragment and the audio in the database even if the query is time-stretched, pitch-shifted, slowed down or sped up while maintaining other desirable features such as scalability, robustness and reliability. This is important since changes in replay speed do occur commonly, they are either introduced by accident during an analog to digital conversion or are introduced deliberately. 

Accidental audio speed changes are often the result of varying or uncalibrated recording or playback speeds of analogue physical media such as wax cylinders, wire recordings, magnetic tapes and gramophone records. To identify duplicates in a digitized archive, a music search algorithm should be robust against replay speed. Deliberate speed manipulations are sometimes introduced during radio broadcasts: occasionally songs are played a bit faster to fit into a time-slot. During a DJ-set speed changes are almost always present. To correctly identify audio in these cases as well, a music search algorithm must be robust against pitch shifting, time stretching and speed changes.


# Statement of need

Audio search algorithms have been described for decades [@Wang2003a;@sonnleitner2014quad_based_fingerprinter;@haitsma2002fingerprinter;@herre2002scalable;@fenet2011pitch_shift_fingerprinting;@cano2005fingerprinting_overview] but have not been accessible for researchers due to the lack of proper, scalable, freely available implementations. Panako solves this problem by providing an acoustic fingerprinting system which can be used by researchers for DJ-set analysis, digital music archive management and audio-to-audio alignment.

In DJ-set analysis the aim is to automatically identify music in sets and how that music was modifided and mixed [@black2018unmixdb;@kim2020djset;@sonnleitner2016landmark]. With its robustness for time-stretch/pitch-shift and speed-up Panako is ideally suited to gather large scale insights in DJ performance practice.

@six2018dupapps describes the applications of acoustic fingerprints for digital music archive management. These range from meta-data quality verification - through the identification of duplicates - to merging archives with potential duplicate material.

A less straightforward application of Panako is audio-to-audio alignment and synchronization [@six2015synchronizing;@six2017framework]. In that case the matching fingerprints are used to align e.g. multiple video recordings of the same event by aligning the audio attached to each video.

Alternative systems with available implementations are by @neuralfp and @ellis20142014. Both systems however lack robustness against speed changes. The details on the ideas implemented in Panako and  [@six2014panako;@six2021panakovtwo]


# Acknowledgements

Ghent University BOF Project Papillom 

# References



