
package com.muzima;

import android.app.Application;

import com.muzima.db.Html5FormDBHelper;
import com.muzima.db.Html5FormDataSource;
import com.muzima.service.FormsService;
import com.muzima.service.HttpService;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "YOUR_FORM_KEY")
public class MuzimaApplication extends Application{
    private Html5FormDataSource html5FormDataSource;
    private FormsService formsService;

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }

    public Html5FormDataSource getHtml5FormDataSource(){
        if(html5FormDataSource == null){
            html5FormDataSource = new Html5FormDataSource(new Html5FormDBHelper(this));
        }
        return html5FormDataSource;
    }

    public FormsService getFormsService() {
        if(formsService == null){
            formsService = new FormsService(this, getHtml5FormDataSource(), new HttpService());
        }
        return formsService;
    }
}