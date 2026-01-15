package com.practice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;

public class FileEventSource implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(FileEventSource.class);

    boolean keepRunning = true;
    long updateInterval;
    private final File file;
    long filePointer = 0;
    EventHandler eventHandler;

    public FileEventSource(boolean keepRunning, long updateInterval, File file, EventHandler eventHandler) {
        this.keepRunning = keepRunning;
        this.updateInterval = updateInterval;
        this.file = file;
        this.eventHandler = eventHandler;
    }

    @Override
    public void run() {
        try {
            while (this.keepRunning) {
                Thread.sleep(this.updateInterval);
                // 파일 크기 계산 및 변경 사항 감지
                long len = this.file.length();
                if (len < this.filePointer) {
                    logger.info("file was reset");
                    filePointer = len;
                } else if (len > this.filePointer) {
                    readAppendAndSend();
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void readAppendAndSend() throws IOException, ExecutionException, InterruptedException {
        RandomAccessFile raf = new RandomAccessFile(this.file, "r");
    }
}
