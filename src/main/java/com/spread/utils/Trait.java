package com.spread.utils;

public class Trait {

    private Object id;
    private Object[] array;
    private boolean isMultivariate = false;

    public Trait(Object obj) {
        this.id = obj;
        if (obj instanceof Object[]) {
            isMultivariate = true;
            array = (Object[]) obj;
        }
    }

    public boolean isMultivariate() {
        return isMultivariate;
    }

    public boolean isNumber() {
        if (!isMultivariate)
            return (id instanceof Double);
        return (array[0] instanceof Double);
    }

    public int getDim() {
        if (isMultivariate) {
            return array.length;
        }
        return 1;
    }

    public double[] getValue() {
        int dim = getDim();
        double[] result = new double[dim];
        if (!isMultivariate) {
            result[0] = (Double) id;
        } else {
            for (int i = 0; i < dim; i++)
                result[i] = (Double) array[i];
        }
        return result;
    }

    public void multiplyBy(double factor) {
        if (!isMultivariate) {
            id = ((Double) id * factor);
        } else {
            for (int i = 0; i < array.length; i++) {
                array[i] = ((Double) array[i] * factor);
            }
        }
    }

    public String toString() {
        if (!isMultivariate)
            return id.toString();
        StringBuffer sb = new StringBuffer(array[0].toString());
        for (int i = 1; i < array.length; i++)
            sb.append("\t").append(array[i]);
        return sb.toString();
    }

    public Object getId() {
        return id;
    }

}// END: class
