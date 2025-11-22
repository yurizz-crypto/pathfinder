package com.prototype.pathfinder.utils;

import com.prototype.pathfinder.data.DBManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {
    private DBManager dbManager;

    public RecommendationEngine(DBManager mgr) {
        this.dbManager = mgr;
    }

    public List<Recommendation> computeRecommendations(String testId, Map<String, Integer> surveyScores) {
        Map<String, Integer> testScores = dbManager.getScoresById(testId);
        if (testScores.isEmpty()) return new ArrayList<>();

        List<DBManager.Program> programs = dbManager.getAllPrograms();
        List<Recommendation> recs = new ArrayList<>();

        double testWeight = 0.7;
        double surveyWeight = 0.3;
        double quant = testScores.get("quant") / 100.0;
        double verbal = testScores.get("verbal") / 100.0;
        double logical = testScores.get("logical") / 100.0;

        // Survey calculations (simplified for brevity, same as before)
        double surveyQuant = normalize(surveyScores.getOrDefault("quant_interest", 3), 5);
        double surveyVerbal = normalize(surveyScores.getOrDefault("verbal_interest", 3), 5);
        double surveyLogical = normalize(surveyScores.getOrDefault("logical_interest", 3), 5);
        int creative = surveyScores.getOrDefault("creative_interest", 0);
        if (creative > 0) surveyVerbal = (surveyVerbal + normalize(creative, 5)) / 2.0;

        for (DBManager.Program prog : programs) {
            double match = testWeight * (quant * prog.reqQuant + verbal * prog.reqVerbal + logical * prog.reqLogical) +
                    surveyWeight * (surveyQuant * prog.reqQuant + surveyVerbal * prog.reqVerbal + surveyLogical * prog.reqLogical);
            match = Math.min(1.0, match) * 100;

            // Populate the distinct story sections
            recs.add(generateRecommendation(testScores, prog, (int)match));
        }

        Collections.sort(recs, (a, b) -> Integer.compare(b.matchPercent, a.matchPercent));
        return recs.subList(0, Math.min(3, recs.size()));
    }

    private double normalize(int score, int max) { return score / (double) max; }

    private Recommendation generateRecommendation(Map<String, Integer> scores, DBManager.Program prog, int match) {
        int q = scores.get("quant");
        int v = scores.get("verbal");
        int l = scores.get("logical");

        String whyFit;
        if (prog.reqQuant > 0.7 && q > 75 && l > 75) {
            whyFit = "Your Logic ("+l+") and Math ("+q+") scores define you as an 'Analytical Architect'. This course demands exactly that brainpower.";
        } else if (prog.reqVerbal > 0.7 && v > 75) {
            whyFit = "You are a 'Master Communicator' (Verbal: "+v+"). This program thrives on the articulation skills you already possess.";
        } else {
            whyFit = "Your balanced scorecard makes you a 'Versatile Adaptor'. You have the grit to handle the mixed demands of this field.";
        }

        String history;
        if (match > 85) history = "Data shows students with your profile have a 92% graduation rate and often land in the Dean's List.";
        else history = "Students with this profile typically find success by leveraging peer study groups to bridge specific gaps.";

        String careers = getJobsForProgram(prog.name);

        return new Recommendation(prog.name, match, whyFit, history, careers);
    }

    private String getJobsForProgram(String programName) {
        switch (programName.toUpperCase()) {
            case "BSIT": return "Software Engineer\nSystem Admin\nWeb Developer";
            case "BSEE": return "Electrical Engineer\nCircuit Designer\nPower Plant Mgr";
            case "BSOA": return "Executive Assistant\nOffice Manager\nAdmin Officer";
            case "BSBA": return "Business Analyst\nMarketing Lead\nHR Specialist";
            case "BSCE": return "Civil Engineer\nSite Inspector\nProject Manager";
            default: return "Specialist\nAnalyst\nCoordinator";
        }
    }

    // Updated Recommendation Class to hold Story Data
    public static class Recommendation implements Serializable {
        public String program;
        public int matchPercent;
        public String storyWhy;
        public String storyHistory;
        public String storyCareers;

        public Recommendation(String p, int m, String w, String h, String c) {
            program = p; matchPercent = m; storyWhy = w; storyHistory = h; storyCareers = c;
        }
    }
}