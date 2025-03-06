package com.example.an_droids;

import java.io.Serializable;

/**
 * SocialSituation represents the social context in which a mood event occurs.
 *It provides fixed options—
 * ALONE, ONE_OTHER, MULTIPLE_PEOPLE, and CROWD—as separate instances.
 * Use the values() method to get all available options and the main method to display them.
 */

public class SocialSituation implements Serializable {
    private String situation;

    private SocialSituation(String situation) {

        this.situation = situation;

    }

    public String getSituation() {

        return situation;

    }

    public static final SocialSituation ALONE = new SocialSituation("alone");
    public static final SocialSituation ONE_OTHER = new SocialSituation("with one other person");
    public static final SocialSituation MULTIPLE_PEOPLE = new SocialSituation("with two to several people");
    public static final SocialSituation CROWD = new SocialSituation("with a crowd");

    public static SocialSituation[] values() {

        return new SocialSituation[] { ALONE, ONE_OTHER, MULTIPLE_PEOPLE, CROWD };

    }

    @Override

    public String toString() {

        return situation;

    }

    public static void main(String[] args) {

        System.out.println("Available Social Situations:");

        System.out.println("ALONE: " + ALONE.getSituation());

        System.out.println("ONE_OTHER: " + ONE_OTHER.getSituation());

        System.out.println("MULTIPLE_PEOPLE: " + MULTIPLE_PEOPLE.getSituation());


        System.out.println("CROWD: " + CROWD.getSituation());
    }
}
