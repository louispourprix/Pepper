package com.storm.posh.planner.planelements;

public class PlanElement {
    public String name;

    private static final String TAG = PlanElement.class.getSimpleName();

    @Override
    public String toString() {
        return "PlanElement{" +
                "name='" + name + '\'' +
                '}';
    }

    public PlanElement(String name) {
        this.name = name;
    }
}

/*
using System.Collections.Generic;

public class PlanElement
{
    private string name = "";
    internal string Name
    {
        get
        {
            return name;
        }
    }

    public PlanElement(string name)
    {
        this.name = name;
    }
}
 */