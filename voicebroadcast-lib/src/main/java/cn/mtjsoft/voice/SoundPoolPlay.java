package cn.mtjsoft.voice;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import cn.mtjsoft.voice.constant.VoiceConstants;
import cn.mtjsoft.voice.util.FileUtils;

/**
 * @describe 音频播放
 * @ideas
 */
public class SoundPoolPlay {
    private SoundPool soundpool;
    private ExecutorService mExecutorService;
    private WeakReference<Context> weakReference;
    // 存储音频
    private volatile Map<String, Integer> soundmap = new HashMap<String, Integer>();
    // 存储播放时长
    private volatile Map<String, Integer> soundLengthMap = new HashMap<String, Integer>();
    // 正在播放的id
    private volatile int soundID = 0;
    // 是否正在播放
    private volatile boolean isPaly = false;
    // 播放队列
    private volatile BlockingQueue<String> soundQueue = new LinkedBlockingQueue<>();

    private SoundPoolPlay(Context context) {
        weakReference = new WeakReference<>(context);
        this.mExecutorService = Executors.newCachedThreadPool();
        if (soundpool == null) {
            //当前系统的SDK版本大于等于21(Android 5.0)时
            if (Build.VERSION.SDK_INT > 21) {
                SoundPool.Builder builder = new SoundPool.Builder();
                //传入音频数量
                builder.setMaxStreams(1);
                //AudioAttributes是一个封装音频各种属性的方法
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                //设置音频流的合适的属性
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);//STREAM_MUSIC
                //加载一个AudioAttributes
                builder.setAudioAttributes(attrBuilder.build());
                soundpool = builder.build();
            } else {
                soundpool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            }
            if (soundmap.isEmpty()) {
                mExecutorService.execute(() -> {
                    String[] videoStr = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dot", "hundred", "success", "ten",
                            "ten_million", "ten_thousand", "thousand", "yuan"};
                    // 装载音频文件
                    for (String str : videoStr) {
                        MediaPlayer mMediaPlayer = null;
                        AssetFileDescriptor fileDescription = null;
                        try {
                            mMediaPlayer = new MediaPlayer();
                            fileDescription = FileUtils.getAssetFileDescription(weakReference.get(),
                                    String.format(VoiceConstants.FILE_PATH, str));
                            soundmap.put(str, soundpool.load(fileDescription.getFileDescriptor(),
                                    fileDescription.getStartOffset(),
                                    fileDescription.getLength(), 1));
                            mMediaPlayer.setDataSource(fileDescription.getFileDescriptor(),
                                    fileDescription.getStartOffset(),
                                    fileDescription.getLength());
                            // 同步装载，获取音频时长
                            mMediaPlayer.prepare();
                            soundLengthMap.put(str, mMediaPlayer.getDuration());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (fileDescription != null) {
                                try {
                                    fileDescription.close();
                                    fileDescription = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (mMediaPlayer != null) {
                                if (mMediaPlayer.isPlaying()) {
                                    mMediaPlayer.stop();
                                }
                                mMediaPlayer.release();
                                mMediaPlayer = null;
                            }
                        }
                    }
                });
            }
        }
    }

    private volatile static SoundPoolPlay soundPoolPlay = null;

    /**
     * 单例
     *
     * @return
     */
    public static SoundPoolPlay with(Context context) {
        if (soundPoolPlay == null) {
            synchronized (SoundPoolPlay.class) {
                if (soundPoolPlay == null) {
                    soundPoolPlay = new SoundPoolPlay(context);
                }
            }
        }
        return soundPoolPlay;
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
     * 开启线程
     *
     * @param builder
     */
    private void executeStart(VoiceBuilder builder) {
        List<String> voicePlay = VoiceTextTemplate.genVoiceList(builder);
        if (voicePlay.isEmpty()) {
            return;
        }
        if (soundQueue.size() > 0) {
            soundQueue.clear();
            isPaly = false;
            soundpool.stop(soundID);
        }
        soundQueue.addAll(voicePlay);
        mExecutorService.execute(this::start);
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
     */
    private void start() {
        if (weakReference.get() != null) {
            while (soundQueue.size() > 0) {
                try {
                    isPaly = true;
                    String take = soundQueue.take();
                    soundID = soundmap.get(take);
                    long lastPlayStartTime = System.currentTimeMillis();
                    soundpool.play(soundID, 1, 1, 1, 0, 1);
                    while (isPaly) {
                        //代表播放完成
                        if (System.currentTimeMillis() > lastPlayStartTime + soundLengthMap.get(take)) {
                            isPaly = false;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isPaly = false;
                }
            }
            isPaly = false;
        }
    }

    /**
     * 开始播报，后面的入队按顺序播放
     *
     * @param voicePlay
     */
    private void startMQ(final List<String> voicePlay) {
        synchronized (SoundPoolPlay.this) {
            if (weakReference.get() != null) {
                CountDownLatch mCountDownLatch = new CountDownLatch(1);
                int count = 0;
                while (count < voicePlay.size() && soundpool != null) {
                    isPaly = true;
                    String take = voicePlay.get(count);
                    soundID = soundmap.get(take);
                    long lastPlayStartTime = System.currentTimeMillis();
                    soundpool.play(soundID, 1, 1, 1, 0, 1);
                    while (isPaly) {
                        //代表播放完成
                        if (System.currentTimeMillis() > lastPlayStartTime + soundLengthMap.get(take)) {
                            isPaly = false;
                            break;
                        }
                    }
                    count++;
                }
                isPaly = false;
                mCountDownLatch.countDown();
                try {
                    mCountDownLatch.await();
                    notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 停止播报，界面销毁时调用释放资源
     */
    public void stop() {
        soundQueue.clear();
        isPaly = false;
        if (soundpool != null) {
            soundpool.release();
            soundpool = null;
        }
        if (!mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
        soundPoolPlay = null;
    }
}
