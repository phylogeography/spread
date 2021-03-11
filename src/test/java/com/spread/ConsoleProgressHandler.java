package com.spread;

import com.spread.progress.IHandler;

public class ConsoleProgressHandler implements IHandler {

    public ConsoleProgressHandler() {
    }

    @Override
    public void handleProgress(int progress) {
        System.out.print(progress);
    }

}
