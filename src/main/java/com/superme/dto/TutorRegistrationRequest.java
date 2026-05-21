package com.superme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TutorRegistrationRequest {

    private String name;
    private String time;

    @JsonProperty("entity_name")
    private String entityName;

    @JsonProperty("entity_type")
    private String entityType;

    private String achievement;
    private String gender;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("calculated_age")
    private Integer calculatedAge;

    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String qualification;
    private String title;

    @JsonProperty("selected_category")
    private String selectedCategory;

    // School
    @JsonProperty("selected_boards")
    private String selectedBoards;

    @JsonProperty("selected_classes")
    private String selectedClasses;

    @JsonProperty("selected_school_subjects")
    private String selectedSchoolSubjects;

    // College
    @JsonProperty("selected_degree")
    private String selectedDegree;

    @JsonProperty("selected_year")
    private String selectedYear;

    // Languages
    @JsonProperty("selected_lang_list")
    private String selectedLangList;

    @JsonProperty("selected_lang_level")
    private String selectedLangLevel;

    // Hobbies
    @JsonProperty("selected_hobbies_skills")
    private String selectedHobbiesSkills;

    @JsonProperty("selected_hobbies_level")
    private String selectedHobbiesLevel;

    @JsonProperty("selected_hobbies_age")
    private String selectedHobbiesAge;

    // Exams
    @JsonProperty("selected_exams")
    private String selectedExams;

    // Sports
    @JsonProperty("selected_sports_acts")
    private String selectedSportsActs;

    @JsonProperty("selected_sports_age")
    private String selectedSportsAge;

    // Others
    @JsonProperty("selected_others_skills")
    private String selectedOthersSkills;

    @JsonProperty("selected_others_level")
    private String selectedOthersLevel;

    // Common teaching details
    @JsonProperty("selected_teach_mode")
    private String selectedTeachMode;

    @JsonProperty("selected_spoken_langs")
    private String selectedSpokenLangs;

    @JsonProperty("selected_experience")
    private String selectedExperience;

    @JsonProperty("selected_fee_type")
    private String selectedFeeType;

    @JsonProperty("selected_days")
    private String selectedDays;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;
}
