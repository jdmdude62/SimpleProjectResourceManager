package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Year;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectNumberGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProjectNumberGenerator.class);
    
    private final ProjectRepository projectRepository;
    private static final String PROJECT_PREFIX = "PRJ";
    private static final Pattern PROJECT_NUMBER_PATTERN = Pattern.compile(PROJECT_PREFIX + "-(\\d{4})-(\\d{4})");
    
    public ProjectNumberGenerator(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }
    
    /**
     * Generates the next available project number in format: PRJ-YYYY-NNNN
     * where YYYY is the current year and NNNN is a sequential number
     */
    public String generateNextProjectNumber() {
        int currentYear = Year.now().getValue();
        int nextSequence = getNextSequenceForYear(currentYear);
        
        String projectNumber = String.format("%s-%04d-%04d", PROJECT_PREFIX, currentYear, nextSequence);
        logger.info("Generated project number: {}", projectNumber);
        
        return projectNumber;
    }
    
    /**
     * Validates if a project number follows the standard format
     */
    public boolean isValidProjectNumber(String projectNumber) {
        if (projectNumber == null || projectNumber.trim().isEmpty()) {
            return false;
        }
        return PROJECT_NUMBER_PATTERN.matcher(projectNumber).matches();
    }
    
    /**
     * Checks if a project number already exists in the database
     */
    public boolean isProjectNumberAvailable(String projectNumber) {
        List<String> existingNumbers = projectRepository.getAllProjectIds();
        return !existingNumbers.contains(projectNumber);
    }
    
    /**
     * Gets the next available sequence number for the given year
     */
    private int getNextSequenceForYear(int year) {
        List<String> allProjectIds = projectRepository.getAllProjectIds();
        
        int maxSequence = 0;
        for (String projectId : allProjectIds) {
            Matcher matcher = PROJECT_NUMBER_PATTERN.matcher(projectId);
            if (matcher.matches()) {
                int projectYear = Integer.parseInt(matcher.group(1));
                if (projectYear == year) {
                    int sequence = Integer.parseInt(matcher.group(2));
                    maxSequence = Math.max(maxSequence, sequence);
                }
            }
        }
        
        return maxSequence + 1;
    }
    
    /**
     * Suggests alternative project numbers if the desired one is taken
     */
    public String suggestAlternative(String desiredNumber) {
        if (isProjectNumberAvailable(desiredNumber)) {
            return desiredNumber;
        }
        
        // If the desired number is taken, generate the next available one
        return generateNextProjectNumber();
    }
}