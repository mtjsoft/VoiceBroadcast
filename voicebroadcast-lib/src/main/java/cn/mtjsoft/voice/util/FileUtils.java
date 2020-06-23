package cn.mtjsoft.voice.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import java.io.IOException;

/**
 * @describe 文件相关的工具类
 * @ideas
 */
public class FileUtils {


    /**
     * Assets获取资源
     *
     * @param context
     * @param filename
     * @return
     * @throws IOException
     */
    public static AssetFileDescriptor getAssetFileDescription(Context context, String filename) throws IOException {
        AssetManager manager = context.getApplicationContext().getAssets();
        return manager.openFd(filename);
    }
}
