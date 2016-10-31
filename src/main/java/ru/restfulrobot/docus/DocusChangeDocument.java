package ru.restfulrobot.docus;

import java.util.Date;

class DocusChangeDocument {
    private Boolean changeFile;
    private String name;
    private String department;
    private Date date;

    DocusChangeDocument(String name, String department, Boolean changeFile,
                        Date date) {
        this.changeFile = changeFile;
        this.department = department;
        this.name = name;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public Boolean getChangeFile() {
        return changeFile;
    }

    public Date getDate() {
        return date;
    }

}