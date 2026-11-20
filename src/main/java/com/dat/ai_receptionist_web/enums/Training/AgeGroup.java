package com.dat.ai_receptionist_web.enums.Training;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AgeGroup {

    AGE_5_OR_LESS("<=5"),

    AGE_6("6"),
    AGE_7("7"),
    AGE_8("8"),
    AGE_9("9"),
    AGE_10("10"),
    AGE_11("11"),
    AGE_12("12"),
    AGE_13("13"),
    AGE_14("14"),
    AGE_15("15"),
    AGE_16("16"),
    AGE_17("17"),
    AGE_18("18"),
    AGE_19("19"),
    AGE_20("20"),
    AGE_21("21"),
    AGE_22("22"),
    AGE_23("23"),
    AGE_24("24"),
    AGE_25("25"),
    AGE_26("26"),
    AGE_27("27"),
    AGE_28("28"),
    AGE_29("29"),

    AGE_30_OR_MORE(">=30");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public static AgeGroup fromAge(int age) {
        if (age <= 5) {
            return AGE_5_OR_LESS;
        }

        if (age >= 30) {
            return AGE_30_OR_MORE;
        }

        return AgeGroup.valueOf("AGE_" + age);
    }
}