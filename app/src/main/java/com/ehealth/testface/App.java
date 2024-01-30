package com.ehealth.testface;

import android.app.Application;

public class App extends Application {

    public  static  Application ApplicationINSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationINSTANCE = this;

    }
}
