package com.spread.parsers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.spread.exceptions.SpreadException;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import com.spread.math.MultivariateNormalDistribution;
import com.spread.utils.Trait;
import com.spread.utils.ParsersUtils;

public class TimeSliceTree implements Callable<Void> {

    private ConcurrentHashMap<Double, List<double[]>> sliceMap;
    private RootedTree currentTree;
    private Double[] sliceHeights;
    private String traitName;
    private String rrwRateName;

    public TimeSliceTree(ConcurrentHashMap<Double, List<double[]>> sliceMap, //
                         RootedTree currentTree, //
                         Double[] sliceHeights, //
                         String traitName, //
                         String rrwRateName //
                         ) {

        this.sliceMap = sliceMap;
        this.currentTree = currentTree;
        this.sliceHeights = sliceHeights;
        this.traitName = traitName;
        this.rrwRateName = rrwRateName;

    }

    @Override
    public Void call() throws SpreadException {

        // parse once per tree

        Double[] precisionArray = ParsersUtils.getDoubleArrayTreeAttribute(currentTree, ParsersUtils.PRECISION);

        int dim = (int) Math.sqrt(1 + 8 * precisionArray.length) / 2;

        double treeNormalization = getTreeLength(currentTree,
                                                 currentTree.getRootNode());

        for (Node node : currentTree.getNodes()) {
            if (!currentTree.isRoot(node)) {

                // parse once per node

                Node parentNode = currentTree.getParent(node);
                Double parentHeight = ParsersUtils.getNodeHeight(currentTree, parentNode);
                Double nodeHeight = ParsersUtils.getNodeHeight(currentTree, node);

                Double rate = 1.0;
                if (rrwRateName != null) {
                    rate = (Double) ParsersUtils.getObjectNodeAttribute(node, rrwRateName);
                }// END: rate set check

                Trait trait = getNodeTrait(node, traitName);
                Trait parentTrait = getNodeTrait(parentNode, traitName);

                if (!trait.isNumber() || !parentTrait.isNumber()) {

                    throw new SpreadException("Trait " + traitName
                                              + " is not numeric!");

                } else {

                    if (trait.getDim() != dim
                        || parentTrait.getDim() != dim) {

                        throw new SpreadException("Trait " + traitName + " is not " + dim + " dimensional!");
                    }
                }

                for (int i = 0; i < sliceHeights.length; i++) {

                    double sliceHeight = sliceHeights[i];

                    if (nodeHeight < sliceHeight
                        && sliceHeight <= parentHeight) {

                        double[] imputedLocation = imputeValue(
                                                               trait.getValue(), //
                                                               parentTrait.getValue(), //
                                                               sliceHeight, //
                                                               nodeHeight, //
                                                               parentHeight, //
                                                               rate, //
                                                               treeNormalization, //
                                                               precisionArray //
                                                               );

                        double latitude = imputedLocation[ParsersUtils.LATITUDE_INDEX];
                        double longitude = imputedLocation[ParsersUtils.LONGITUDE_INDEX];
                        double[] coordinate = new double[2];
                        coordinate[ParsersUtils.LATITUDE_INDEX] = latitude;
                        coordinate[ParsersUtils.LONGITUDE_INDEX] = longitude;

                        if (sliceMap.containsKey(sliceHeight)) {

                            sliceMap.get(sliceHeight).add(coordinate);

                        } else {

                            LinkedList<double[]> coords = new LinkedList<double[]>();
                            coords.add(coordinate);

                            sliceMap.put(sliceHeight, coords);

                        } // END: key check

                    } // END: sliceTime check

                } // END: i loop

            } // END: root node check
        } // END: node loop

        return null;
    }

    private double[] imputeValue(double[] trait, //
                                 double[] parentTrait, //
                                 double sliceHeight, //
                                 double nodeHeight, //
                                 double parentHeight, //
                                 double rate, //
                                 double treeNormalization, //
                                 Double[] precisionArray //
                                 ) {

        int dim = (int) Math.sqrt(1 + 8 * precisionArray.length) / 2;

        double[][] precision = new double[dim][dim];
        int c = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                precision[j][i] = precision[i][j] = precisionArray[c++]
                    * treeNormalization;
            }
        }

        double[] nodeValue = new double[dim];
        double[] parentValue = new double[dim];

        for (int i = 0; i < dim; i++) {

            nodeValue[i] = trait[i];
            parentValue[i] = parentTrait[i];

        }

        final double scaledTimeChild = (sliceHeight - nodeHeight) * rate;
        final double scaledTimeParent = (parentHeight - sliceHeight) * rate;
        final double scaledWeightTotal = (1.0 / scaledTimeChild)
            + (1.0 / scaledTimeParent);

        if (scaledTimeChild == 0) {
            return trait;
        }

        if (scaledTimeParent == 0) {
            return parentTrait;
        }
        // Find mean value, weighted average
        double[] mean = new double[dim];
        double[][] scaledPrecision = new double[dim][dim];

        for (int i = 0; i < dim; i++) {
            mean[i] = (nodeValue[i] / scaledTimeChild + parentValue[i]
                       / scaledTimeParent)
                / scaledWeightTotal;

            for (int j = i; j < dim; j++)
                scaledPrecision[j][i] = scaledPrecision[i][j] = precision[i][j]
                    * scaledWeightTotal;
        }

        mean = MultivariateNormalDistribution.nextMultivariateNormalPrecision(mean,
                                                                              scaledPrecision);

        double[] result = new double[dim];
        for (int i = 0; i < dim; i++) {
            result[i] = mean[i];
        }

        return result;
    }// END: imputeValue

    private Trait getNodeTrait(Node node, String traitName) throws SpreadException {

        Object nodeAttribute = node.getAttribute(traitName);

        if (nodeAttribute == null) {
            throw new SpreadException("Attribute " + traitName + " missing from the node. \n");
        }

        return new Trait(nodeAttribute);
    }// END: getNodeTrait

    private double getTreeLength(RootedTree tree, Node node) {

        int childCount = tree.getChildren(node).size();
        if (childCount == 0)
            return tree.getLength(node);

        double length = 0;
        for (int i = 0; i < childCount; i++) {
            length += getTreeLength(tree, tree.getChildren(node).get(i));
        }
        if (node != tree.getRootNode())
            length += tree.getLength(node);

        return length;
    }// END: getTreeLength

}
