# VoiceBroadcast收款金额播报组件
收款语音播报通知可以使用很多的文字转语音的SDK，但本方案使用的是播放本地的mp3资源，实现了使用 SoundPool 和 MediaPlayer 两种方式来播放音频。

demo中的界面如下： 

如果加入队列，后面的语音会等待依次播报，否则当前正在播报的会停止，然后会立即播放最新的。
![demo界面](https://images.gitee.com/uploads/images/2020/0623/160451_7ca18234_1510987.jpeg)

# 使用说明
## 1、在根目录 build.gradle 添加:

```java
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
## 2、在module项目下的build.gradle中添加：

```java
implementation 'com.gitee.mtj_java:VoiceBroadcast:1.0.0'
```
## 3、初始化：

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 提前初始化装载音频文件
        // 使用 SoundPool 播放音频
        SoundPoolPlay.with(getBaseContext());
        // 使用 MediaPlayer 播放音频
//        VoicePlay.with(getBaseContext());
    }
```
## 4、开始播报
默认开头带有 ”收款成功“ ，末尾带有单位 ”元“
```java
// play 第一个参数：金额
// play 第二个参数：是否转成全数字。 默认人民币
// play 第三个参数：是否加入队列，依次播报
// 使用 SoundPool 播放音频
SoundPoolPlay.with(getBaseContext()).play("47.80", false, false);
// 使用 MediaPlayer 播放音频
//  VoicePlay.with(getBaseContext()).play("47.80", false, false);
```
自定义播报

```java
VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                // 播报开头语，不需要可以不设置
                .start(VoiceConstants.SUCCESS)
                // 播报金额
                .money("45.78")
                // 播报金额单位（元），不需要可以不设置
                .unit(VoiceConstants.YUAN)
                // 是否转成全数字
                .checkNum(false)
                .builder();
// play 第一个参数：自定义
// play 第二个参数：是否加入队列，依次播报
// 使用 SoundPool 播放音频
SoundPoolPlay.with(getBaseContext()).play(voiceBuilder, false);          
 // 使用 MediaPlayer 播放音频
//  VoicePlay.with(getBaseContext()).play(voiceBuilder, false);
```


## 5、onDestroy中释放资源

```java
    @Override
    protected void onDestroy() {
        SoundPoolPlay.with(getBaseContext()).stop();
//        VoicePlay.with(getBaseContext()).stop();
        super.onDestroy();
    }
```
# 完结

**本人公众号，关注一波，共同交流吧。**
![在这里插入图片描述](https://images.gitee.com/uploads/images/2020/0623/160234_026ba2fd_1510987.jpeg)
