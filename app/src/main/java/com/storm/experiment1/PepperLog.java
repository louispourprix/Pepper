package com.storm.experiment1;

import com.storm.posh.plan.planelements.PlanElement;
import com.storm.posh.plan.planelements.Sense;
import com.storm.posh.plan.planelements.drives.DriveCollection;

public interface PepperLog {
    void appendLog(String tag, String text, boolean server);
    void appendLog(String tag, String text);
    void appendLog(String text);
    void clearLog();

    void setCurrentDrive(DriveCollection drive);
    void setCurrentElement(PlanElement element);

    void checkedBooleanSense(String tag, Sense sense, boolean value);
    void checkedDoubleSense(String tag, Sense sense, double value);
    void clearCheckedSenses();

    void  notifyABOD3(String name, String type);
}
