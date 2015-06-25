package org.chrisjr.susurrantutils;

import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;
 
import cc.mallet.pipe.*;
import cc.mallet.types.*;
 
public class MalletTfIdfPruner {
    public static void prune(String instanceFilename, String newInstanceFilename, 
                             int topWords, int minDocFreq) {
        InstanceList instances = InstanceList.load(new File(instanceFilename));
        Alphabet oldAlphabet = instances.getDataAlphabet();
 
        int numFeatures = oldAlphabet.size();
         
        Iterator<Instance> instanceIterator = instances.iterator();
        FeatureTfIdfPipe idfPipe = new FeatureTfIdfPipe(oldAlphabet, null);
        Iterator<Instance> iterator = idfPipe.newIteratorFrom(instanceIterator);
 
        // We aren't really interested in the instance itself,
        //  just the total feature counts.
        while (iterator.hasNext()) {
            iterator.next();
        }
         
        double[] tfScores = idfPipe.getTfScores();
        double[] idfScores = idfPipe.getIdfScores(minDocFreq);
        double[] tfIdfScores = new double[numFeatures];
 
        TreeSet<IDSorter> tfidf = new TreeSet<IDSorter>();
        for (int feature = 0; feature < numFeatures; feature++) {
            tfIdfScores[feature] = tfScores[feature] * idfScores[feature];
            tfidf.add(new IDSorter(feature, tfIdfScores[feature]));
        }
         
        Iterator<IDSorter> tfIdfIterator = tfidf.iterator();
 
        int minRank;
        int i = numFeatures;
        if (numFeatures - topWords > 1) {
            minRank = numFeatures - topWords - 2;
        } else {
            minRank = 0;
        }
         
        VocabPrinter vocabOut = null;
         
        while (tfIdfIterator.hasNext()) {
            IDSorter feature = (IDSorter) tfIdfIterator.next();
            i--;
            if (vocabOut != null) {
                vocabOut.printCsv(feature);
            }
            tfIdfScores[feature.getID()] = i;
        }
         
        if (vocabOut != null) {
            vocabOut.close();
        }
         
//        instances = InstanceList.load(new File(instanceFilename));
        Alphabet newAlphabet = new Alphabet();
        Noop newPipe = new Noop(newAlphabet, instances.getTargetAlphabet());
        InstanceList newInstanceList = new InstanceList(newPipe);
 
        Instance instance;
        while (instances.size() > 0) {
            instance = instances.get(0);
            FeatureSequence fs = (FeatureSequence) instance.getData();
             
            fs.prune(tfIdfScores, newAlphabet, minRank);
            newInstanceList.addThruPipe(instance);
             
//          newInstanceList.addThruPipe(new Instance(fs,
//                  instance.getTarget(),
//                    instance.getName(),
//                    instance.getSource()));               
            instances.remove(0);
        }
         
        instances = newInstanceList;
        instances.save (new File(newInstanceFilename));
    }
     
    public static void prune(String instanceFilename) {
        prune(instanceFilename, instanceFilename.replaceAll("\\.mallet$", "_pruned.mallet"));
    }
    public static void prune(String instanceFilename, String newInstanceFilename) {
        prune(instanceFilename, newInstanceFilename, 4096, 3);
    }
    public static void prune(String instanceFilename, int topWords, int minDocFreq) {
        prune(instanceFilename, instanceFilename.replaceAll("\\.mallet$", "_pruned.mallet"),
              topWords, minDocFreq);
    }
     
    private static class VocabPrinter extends PrintStream {
        Alphabet oldAlphabet;
        public VocabPrinter(String vocabFilename, Alphabet oldAlphabet) throws FileNotFoundException {
            super(new BufferedOutputStream(new FileOutputStream(vocabFilename)));
            this.oldAlphabet = oldAlphabet;
        }
         
        public void printCsv(IDSorter feature) {
            print(oldAlphabet.lookupObject(feature.getID()));
            print(',');
            println(feature.getWeight());
        }
    }
 
}