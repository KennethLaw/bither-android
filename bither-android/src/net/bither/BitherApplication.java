/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;

import net.bither.activity.cold.ColdActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjApplication;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.utils.Threading;
import net.bither.exception.UEHandler;
import net.bither.service.BlockchainService;

import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class BitherApplication extends BitherjApplication {

    private ActivityManager activityManager;

    private static org.slf4j.Logger log = LoggerFactory.getLogger(BitherApplication.class);
    private static BitherApplication mBitherApplication;
    public static HotActivity hotActivity;
    public static ColdActivity coldActivity;
    public static UEHandler ueHandler;
    public static Activity initialActivity;
    public static boolean isFirstIn = false;
    public static long reloadTxTime = -1;


    @Override
    public void onCreate() {
        AbstractAppAndroidImpl appAndroid = new AbstractAppAndroidImpl();
        appAndroid.construct();
        super.onCreate();
        AbstractApp.notificationService.removeAddressLoadCompleteState();
        initApp();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
                .permitDiskReads().permitDiskWrites().penaltyLog().build());
        Threading.throwOnLockCycles();
        mBitherApplication = this;
        ueHandler = new UEHandler();
        Thread.setDefaultUncaughtExceptionHandler(ueHandler);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    }


    public static BitherApplication getBitherApplication() {
        return mBitherApplication;
    }

    public static void startBlockchainService() {
        mContext.startService(new Intent(mContext, BlockchainService.class));

    }


    public static boolean canReloadTx() {
        if (reloadTxTime == -1) {
            return true;
        } else {
            return reloadTxTime + 60 * 60 * 1000 < System.currentTimeMillis();
        }
    }

    public static File getLogDir() {
        final File logDir = BitherjApplication.mContext.getDir("log", Context.MODE_WORLD_READABLE);
        return logDir;
    }

    private void initLogging() {
        final File logDir = getLogDir();
        final File logFile = new File(logDir, "bitherj.log");
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
        filePattern.setContext(context);
        filePattern.setPattern("%d{HH:mm:ss.SSS} [%thread] %logger{0} - %msg%n");
        filePattern.start();

        final RollingFileAppender<ILoggingEvent> fileAppender = new
                RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(context);
        fileAppender.setFile(logFile.getAbsolutePath());

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new
                TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir.getAbsolutePath() + "/bitherj.%d.log.gz");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setEncoder(filePattern);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setTagEncoder(logcatTagPattern);
        logcatAppender.setEncoder(logcatPattern);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);
        log.addAppender(fileAppender);
        log.addAppender(logcatAppender);
        log.setLevel(Level.INFO);
    }

    private void initApp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AddressManager.getInstance();
                initLogging();
            }
        }).start();
    }


}
