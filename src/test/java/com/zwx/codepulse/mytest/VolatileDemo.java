package com.zwx.codepulse.mytest;

import java.util.concurrent.TimeUnit;

/**
 * @author: 张伟旭
 * @Create: 2026-10-11 18:41
 * @description:
 **/

public class VolatileDemo {
    static volatile int flag = 0;

    public synchronized static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            int localFlag = flag;
            while (true){
                if (localFlag != flag){
                    System.out.println("读取到了修改后的标志位: " + flag);
                    localFlag = flag;
                }
            }
        }).start();

        new Thread(() -> {
            int localFlag = flag;
            while (true){
                System.out.println("标志位被修改为了: " + ++localFlag);
                flag = localFlag;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
