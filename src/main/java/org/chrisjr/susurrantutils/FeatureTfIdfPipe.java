package org.chrisjr.susurrantutils;
 
import java.lang.Math;
 
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureCounter;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
 
public class FeatureTfIdfPipe extends Pipe {
    /**
     * 
     */
    private static final long serialVersionUID = 6108361280753335174L;
    FeatureCounter dfCounter;
    TIntDoubleHashMap tfMaximum;
    int numInstances = 0;
 
    public FeatureTfIdfPipe(Alphabet dataAlphabet, Alphabet targetAlphabet) {
        super(dataAlphabet, targetAlphabet);
 
        dfCounter = new FeatureCounter(this.getDataAlphabet());
        tfMaximum = new TIntDoubleHashMap();
    }
     
    public FeatureTfIdfPipe() {
        this(new Alphabet(), null);
    }
 
    public Instance pipe(Instance instance) {
        TIntIntHashMap localCounter = new TIntIntHashMap();
        double total = 0.0;
             
        if (instance.getData() instanceof FeatureSequence) {
                 
            FeatureSequence features = (FeatureSequence) instance.getData();
 
            for (int position = 0; position < features.size(); position++) {
                int feature = features.getIndexAtPosition(position);
                total += feature;
                localCounter.adjustOrPutValue(feature, 1, 1);
            }
        }
        else {
            throw new IllegalArgumentException("Looking for a FeatureSequence, found a " + 
                                               instance.getData().getClass());
        }
 
        for (int feature: localCounter.keys()) {
            dfCounter.increment(feature);
            double tf = (double) localCounter.get(feature) / total;
            if (tf > tfMaximum.get(feature)) {
                tfMaximum.put(feature, tf);
            }
        }
 
        numInstances++;         
 
        return instance;
    }
     
    public double[] getTfScores() {
        Alphabet currentAlphabet = getDataAlphabet();
        double[] tfScores = new double[currentAlphabet.size()];
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
            tfScores[feature] = tfMaximum.get(feature);
        }
        return tfScores;
    }
    public double[] getIdfScores(int minDocFreq) {
        Alphabet currentAlphabet = getDataAlphabet();
        double[] idfScores = new double[currentAlphabet.size()];
 
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
            int df = dfCounter.get(feature);
            double idf = Math.log10(((double) numInstances) / (double) df);
            if (df < minDocFreq) idf = -1.0;
            idfScores[feature] = idf;
        }
        return idfScores;
    }
 
}