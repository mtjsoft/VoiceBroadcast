package cn.mtjsoft.voice;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.mtjsoft.voice.constant.VoiceConstants;
import cn.mtjsoft.voice.util.FileUtils;

/**
 * @describe 音频播放
 * @ideas
 */

public class VoicePlay {

    private MediaPlayer mMediaPlayer;
    private ExecutorService mExecutorService;
    private WeakReference<Context> weakReference;

    private VoicePlay(Context context) {
        weakReference = new WeakReference<>(context);
        this.mExecutorService = Executors.newSingleThreadExecutor();
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }
    }

    private volatile static VoicePlay mVoicePlay = null;

    /**
     * 单例
     *
     * @return
     */
    public static VoicePlay with(Context context) {
        if (mVoicePlay == null) {
            synchronized (VoicePlay.class) {
                if (mVoicePlay == null) {
                    mVoicePlay = new VoicePlay(context);
                }
            }
        }
        return mVoicePlay;
    }

    /**
     * 默认收款成功样式
     *
     * @param money
     */
    public void play(String money) {
        play(money, false);
    }

    /**
     * @param money    金额
     * @param checkNum 是否中文
     * @param mq       放入播放队列，按顺序播放
     */
    public void play(String money, boolean checkNum, boolean mq) {
        VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                .start(VoiceConstants.SUCCESS)
                .money(money)
                .unit(VoiceConstants.YUAN)
                .checkNum(checkNum)
                .builder();
        if (mq) {
            executeStartMQ(voiceBuilder);
        } else {
            executeStart(voiceBuilder);
        }
    }

    /**
     * 设置播报数字
     *
     * @param money
     * @param checkNum
     */
    public void play(String money, boolean checkNum) {
        VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                .start(VoiceConstants.SUCCESS)
                .money(money)
                .unit(VoiceConstants.YUAN)
                .checkNum(checkNum)
                .builder();
        executeStart(voiceBuilder);
    }

    /**
     * 接收自定义
     *
     * @param voiceBuilder
     */
    public void play(VoiceBuilder voiceBuilder, boolean mq) {
        if (mq) {
            executeStartMQ(voiceBuilder);
        } else {
            executeStart(voiceBuilder);
        }
    }

    /**
     * 停止播报，界面销毁时调用释放资源
     */
    public void stop() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (!mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
    }

    /**
     * 开启线程
     *
     * @param builder
     */
    private void executeStart(VoiceBuilder builder) {
        List<String> voicePlay = VoiceTextTemplate.genVoiceList(builder);
        if (voicePlay.isEmpty()) {
            return;
        }
        start(voicePlay);
    }

    /**
     * 开启线程
     *
     * @param builder
     */
    private void executeStartMQ(VoiceBuilder builder) {
        List<String> voicePlay = VoiceTextTemplate.genVoiceList(builder);
        if (voicePlay.isEmpty()) {
            return;
        }
        mExecutorService.execute(() -> startMQ(voicePlay));
    }

    /**
     * 开始播报，后面的覆盖前面的播报
     *
     * @param voicePlay
     */
    private void start(final List<String> voicePlay) {
        if (weakReference.get() != null) {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
            } else {
                mMediaPlayer = new MediaPlayer();
            }
            AssetFileDescriptor assetFileDescription = null;
            try {
                final int[] counter = {0};
                assetFileDescription = FileUtils.getAssetFileDescription(weakReference.get(),
                        String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                mMediaPlayer.setDataSource(
                        assetFileDescription.getFileDescriptor(),
                        assetFileDescription.getStartOffset(),
                        assetFileDescription.getLength());
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    if (weakReference.get() == null) {
                        stop();
                    } else {
                        mediaPlayer.reset();
                        counter[0]++;
                        if (counter[0] < voicePlay.size()) {
                            try {
                                AssetFileDescriptor fileDescription2 = FileUtils.getAssetFileDescription(weakReference.get(),
                                        String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                                mediaPlayer.setDataSource(
                                        fileDescription2.getFileDescriptor(),
                                        fileDescription2.getStartOffset(),
                                        fileDescription2.getLength());
                                mediaPlayer.prepare();
                                mediaPlayer.start();
                                fileDescription2.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                mMediaPlayer.setOnErrorListener((mediaPlayer, i, i1) -> {
                    stop();
                    return false;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (assetFileDescription != null) {
                    try {
                        assetFileDescription.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 开始播报，后面的入队按顺序播放
     *
     * @param voicePlay
     */
    private void startMQ(final List<String> voicePlay) {
        synchronized (VoicePlay.this) {
            if (weakReference.get() != null) {
                mMediaPlayer = new MediaPlayer();
                final CountDownLatch mCountDownLatch = new CountDownLatch(1);
                AssetFileDescriptor assetFileDescription = null;
                try {
                    final int[] counter = {0};
                    assetFileDescription = FileUtils.getAssetFileDescription(weakReference.get(),
                            String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                    mMediaPlayer.setDataSource(
                            assetFileDescription.getFileDescriptor(),
                            assetFileDescription.getStartOffset(),
                            assetFileDescription.getLength());
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                        if (weakReference.get() == null) {
                            stop();
                            mCountDownLatch.countDown();
                        } else {
                            mediaPlayer.reset();
                            counter[0]++;
                            if (counter[0] < voicePlay.size()) {
                                try {
                                    AssetFileDescriptor fileDescription2 = FileUtils.getAssetFileDescription(weakReference.get(),
                                            String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                                    mediaPlayer.setDataSource(
                                            fileDescription2.getFileDescriptor(),
                                            fileDescription2.getStartOffset(),
                                            fileDescription2.getLength());
                                    mediaPlayer.prepare();
                                    mediaPlayer.start();
                                    fileDescription2.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    mCountDownLatch.countDown();
                                }
                            } else {
                                mCountDownLatch.countDown();
                            }
                        }
                    });
                    mMediaPlayer.setOnErrorListener((mediaPlayer, i, i1) -> {
                        stop();
                        mCountDownLatch.countDown();
                        return false;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mCountDownLatch.countDown();
                } finally {
                    if (assetFileDescription != null) {
                        try {
                            assetFileDescription.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    mCountDownLatch.await();
                    notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
