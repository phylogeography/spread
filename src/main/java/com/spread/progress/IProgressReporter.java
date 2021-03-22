package com.spread.progress;

public interface IProgressReporter {
   void registerProgressObserver(IProgressObserver observer);
   void updateProgress(double progress);
}
