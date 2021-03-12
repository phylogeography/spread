package com.spread.progress;

public interface IProgressObserver {
    void init (IProgressReporter reporter);
    void handleProgress (double progress);
}
