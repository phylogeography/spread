package com.spread.utils;

public class ProgressBar extends Thread {

    private static final String anim = "|/-\\";

    private boolean showProgress;
    private double progressPercentage;
    private final int barLength;

    public ProgressBar(int barLength) {
        this.barLength = barLength;
        this.showProgress = true;
        this.progressPercentage = 0;
    }// END: Constructor

    public void run() {

        int i = 0;

        while (showProgress) {

            int column = (int) (progressPercentage * (barLength));
            int j = 0;

            String progress = "\r[";
            for (; j < column - 1; j++) {
                progress += ("*");
            }

            String whitespace = "";
            for (; j < barLength - 2; j++) {
                whitespace += (" ");
            }
            whitespace += ("]");

            System.out.print(progress + anim.charAt(i++ % anim.length())
                             + whitespace);

            try {

                Thread.sleep(100);

            } catch (Exception e) {
                // do nothing
            }// END: try-catch

        }// END: while

    }// END: run

    public void showCompleted() {
        String progress = "\r[";
        for (int i = 0; i < barLength - 1; i++) {
            progress += ("*");
        }
        progress += ("]");
        System.out.print(progress);
    }// END: showCompleted

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }// END: setShowProgress

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }// END: setProgressPercentage

}// END: class
