package com.omarea.shared;

/**
 * Created by helloklf on 2017/6/3.
 */

public class xposed_check {
    //判断Xposed插件是否已经激活（将在Xposed部分中hook返回值为true）
    public static boolean xposedIsRunning(){
        return false;
    }
}
