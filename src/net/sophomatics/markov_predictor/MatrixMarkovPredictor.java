package net.sophomatics.markov_predictor;

import net.sophomatics.matrix.NestedMapMatrix;
import net.sophomatics.matrix.Matrix;
import net.sophomatics.util.Identifiable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by mark on 12.07.15.
 */
public class MatrixMarkovPredictor<Condition, Consequence> extends Identifiable implements MarkovPredictor<Condition, Consequence> {
    private final Matrix<Condition, Consequence, Integer> matrix;
    private final Logger logger;

    public MatrixMarkovPredictor(int id) {
        super(id);
        this.matrix = new NestedMapMatrix<>();
        this.logger = Logger.getLogger(this.getClass().getSimpleName());
    }

    public MatrixMarkovPredictor(int id, MatrixMarkovPredictor<Condition, Consequence> other) {
        this(id);
        this.matrix.integrate(other.matrix);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        } else if (!(other instanceof MarkovPredictor)){
            return false;
        }
        MatrixMarkovPredictor cast = (MatrixMarkovPredictor) other;
        return cast.matrix.equals(this.matrix);
    }

    @Override
    public int hashCode() {
        return 37 * 3 + this.matrix.hashCode();
    }

    @Override
    public void clear() {
        this.matrix.clear();
    }

    @Override
    public int getMass(Condition cause) {
        int sum = 0;
        for (int eachFrequency : this.matrix.getValues(cause)) {
            sum += eachFrequency;
        }
        return sum;
    }

    @Override
    public Set<Consequence> getEffects(Condition cause) {
        return this.matrix.getKeys(cause);
    }

    @Override
    public boolean isKnown(Condition cause) {
        return this.matrix.containsKey(cause);
    }

    public String print() {
        return this.toString() + "\n" + this.matrix.print();
    }

    @Override
    public String toString() {
        return String.format("%s%s", this.getClass().getSimpleName(), this.getId());
    }

    @Override
    public boolean containsCondition(Condition cause) {
        return this.matrix.get(cause) != null;
    }

    @Override
    public int getFrequency(Condition cause, Consequence effect) {
        Integer f = this.matrix.get(cause, effect);
        if (f == null) {
            return 0;
        }
        return f;
    }

    @Override
    public void store(Condition cause, Consequence effect) {
        this.matrix.put(cause, effect, this.getFrequency(cause, effect) + 1);
    }

    @Override
    public Consequence getConsequence(Condition cause) {
        Map<Consequence, Integer> row = this.matrix.get(cause);
        if (row == null) {
            String message = String.format("<%s> has no consequence yet.", cause);
            this.logger.log(Level.SEVERE, message);
            throw new NullPointerException();
        }

        List<Consequence> nextList = new ArrayList<>();
        int thisValue, maxValue = -1;
        for (Map.Entry<Consequence, Integer> entry : row.entrySet()) {
            thisValue = entry.getValue();
            if (thisValue == maxValue) {
                nextList.add(entry.getKey());

            } else if (maxValue < thisValue) {
                nextList.clear();
                nextList.add(entry.getKey());
                maxValue = thisValue;
            }
        }

        return nextList.get(0);
    }

    @Override
    public float getSimilarity(MarkovPredictor<Condition, Consequence> other) {
        MatrixMarkovPredictor<Condition, Consequence> cast = (MatrixMarkovPredictor<Condition, Consequence>) other;
        float similarity = 1f;

        Condition otherCause;
        Consequence otherEffect;
        Map<Consequence, Integer> otherRow;
        Map<Consequence, Integer> thisRow;
        float sum;
        Integer thisFrequency;

        for (Map.Entry<Condition, Map<Consequence, Integer>> otherEntry : cast.matrix.entrySet()) {
            otherCause = otherEntry.getKey();
            thisRow = this.matrix.get(otherCause);
            if (thisRow == null) {
                continue;
            }
            sum = 0;
            for (Integer eachFreq : thisRow.values()) {
                sum += eachFreq;
            }
            otherRow = otherEntry.getValue();
            for (Map.Entry<Consequence, Integer> otherSubEntry : otherRow.entrySet()) {
                otherEffect = otherSubEntry.getKey();
                thisFrequency = thisRow.get(otherEffect);
                if (thisFrequency == null || thisFrequency < 1) {
                    return 0f;
                }
                similarity *= Math.pow(thisFrequency / sum, otherSubEntry.getValue());
            }
        }

        return similarity;
    }

    @Override
    public void add(MarkovPredictor<Condition, Consequence> other) {
        MatrixMarkovPredictor<Condition, Consequence> cast = (MatrixMarkovPredictor<Condition, Consequence>) other;
        NestedMapMatrix<Condition, Consequence, Integer> otherMatrix = (NestedMapMatrix<Condition, Consequence, Integer>) cast.matrix;

        Condition otherCause;
        Consequence otherEffect;
        Map<Consequence, Integer> thisRow, otherRow;
        Integer thisValue;

        for (Map.Entry<Condition, Map<Consequence, Integer>> entry : otherMatrix.entrySet()) {
            otherCause = entry.getKey();
            otherRow = entry.getValue();
            thisRow = this.matrix.getRow(otherCause);

            for (Map.Entry<Consequence, Integer> subEntry : otherRow.entrySet()) {
                otherEffect = subEntry.getKey();
                thisValue = thisRow.get(otherEffect);
                if (thisValue == null) {
                    thisValue = 0;
                }
                thisRow.put(otherEffect, thisValue + subEntry.getValue());
            }
        }
    }

    @Override
    public float getProbability(Condition cause, Consequence effect) {
        Map<Consequence, Integer> row = this.matrix.get(cause);
        if (row == null) {
            return 1f;
        }

        Integer f = row.get(effect);
        if (f == null) {
            return 0f;
        }

        int sum = 0;
        for (int eachF : row.values()) {
            sum += eachF;
        }

        return (float) f / sum;
    }

}
