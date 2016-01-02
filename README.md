# BioDataCloud-IGV

IGV (Integrative Genomics Viewer) [1] is a very efficient genome browser written in Java, allowing users to visualize and explore a large variety of genomic data types, but limited to a single genome. However, to perform comparative genomic studies, it is very useful to be able to observe different types of data simultaneously on several genotypes.

As part of the BioDataCloud project (Investments for the Future Initiative), a collaboration between the INRA Migale platform and the Biogemma company was established to tackle this issue. According to the technical specifications set by Biogemma, a new feature has been added to IGV that allows users to jump to a new genotype from different types of data (genes, regions in genomic sequence, genetic markers) selected by the user on the reference genome. 
This jump results in the opening of a new IGV window on these data, if they are available for the new genotype. This window retains all IGV features and synchronizes simultaneously with the main window. 
All jumps can be saved in an IGV session file allowing users to quickly restore already used genotypes and data or to share them with other.

The number of jumps achievable and therefore the number of simultaneously observable genomes depends only upon the available hardware (mainly RAM) capabilities and the availability of the corresponding data.
This  IGV version was deployed in a virtual machine of the IFB cloud (http://france-bioinformatique.fr) to benefit from the high RAM resources required by the application. 

With this new feature, users can now compare different genotypes with the reference genome and navigate between them synchronously while keeping the IGV performance.

References:
[1] Helga Thorvaldsdóttir, James T. Robinson, Jill P. Mesirov. Integrative Genomics Viewer (IGV): high-performance genomics data visualization and exploration. Briefings in Bioinformatics 14, 178-192 (2013).

# Poster
<A HREF="http://migale.jouy.inra.fr/sites/migale.jouy.inra.fr.drupal7.migale.jouy.inra.fr/files/poster_jobim_v5.pdf"><IMG SRC="http://migale.jouy.inra.fr/sites/migale.jouy.inra.fr.drupal7.migale.jouy.inra.fr/files/poster_jobim_v5.pdf"></A>
