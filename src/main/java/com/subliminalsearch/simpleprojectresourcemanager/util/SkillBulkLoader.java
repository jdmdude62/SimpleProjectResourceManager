package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Skill;
import com.subliminalsearch.simpleprojectresourcemanager.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SkillBulkLoader {
    private static final Logger logger = LoggerFactory.getLogger(SkillBulkLoader.class);
    
    public static void main(String[] args) {
        SkillBulkLoader loader = new SkillBulkLoader();
        loader.loadSkills();
    }
    
    public void loadSkills() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        SkillRepository skillRepo = new SkillRepository(dbConfig.getDataSource());
        
        Map<String, String[]> skillsByCategory = new HashMap<>();
        
        // Project Management Skills
        skillsByCategory.put("Planning & Coordination", new String[] {
            "Project Scoping and Estimation",
            "Resource Allocation and Scheduling",
            "Multi-Site Project Coordination",
            "Gantt Chart/MS Project Management",
            "Critical Path Analysis",
            "Milestone Tracking",
            "Equipment and Material Logistics",
            "Subcontractor Management",
            "Installation Sequencing",
            "Cutover Planning and Execution"
        });
        
        skillsByCategory.put("Technical Project Leadership", new String[] {
            "Technical Specification Review",
            "Solution Architecture Approval",
            "Integration Point Identification",
            "Risk Mitigation Planning",
            "Technical Escalation Management",
            "Quality Assurance Oversight",
            "System Testing Coordination",
            "Commissioning Management",
            "Performance Metrics Definition"
        });
        
        // Team Management Skills
        skillsByCategory.put("Personnel Management", new String[] {
            "Team Building and Motivation",
            "Performance Evaluation",
            "Skills Gap Analysis",
            "Training Program Development",
            "Conflict Resolution",
            "Workload Balancing",
            "On-Call Rotation Management",
            "Career Development Planning",
            "Mentoring and Coaching",
            "Remote Team Coordination"
        });
        
        skillsByCategory.put("Field Operations", new String[] {
            "Field Service Dispatch",
            "Emergency Response Coordination",
            "Travel and Logistics Management",
            "Tool and Equipment Inventory Control",
            "Vehicle Fleet Management",
            "Safety Program Implementation",
            "Incident Investigation",
            "Time and Attendance Tracking"
        });
        
        // Business Management Skills
        skillsByCategory.put("Financial Management", new String[] {
            "Project Budget Development",
            "Cost Control and Tracking",
            "Change Order Management",
            "Invoice Review and Approval",
            "Profitability Analysis",
            "Capital Equipment Planning",
            "Expense Report Management",
            "Contract Negotiation Support"
        });
        
        skillsByCategory.put("Client Relations", new String[] {
            "Stakeholder Management",
            "Executive-Level Communication",
            "Expectation Setting",
            "Escalation Handling",
            "Customer Satisfaction Monitoring",
            "Account Growth Strategies",
            "Service Level Agreement (SLA) Management",
            "Proposal Development"
        });
        
        // Compliance & Documentation
        skillsByCategory.put("Quality & Standards", new String[] {
            "ISO 9001 Implementation",
            "Standard Operating Procedure (SOP) Development",
            "Audit Preparation and Response",
            "Compliance Tracking",
            "Certification Management",
            "Best Practice Documentation",
            "Knowledge Base Management"
        });
        
        skillsByCategory.put("Safety & Regulatory", new String[] {
            "OSHA Compliance Management",
            "Job Safety Analysis (JSA) Creation",
            "Permit Management",
            "Environmental Compliance",
            "Insurance and Liability Management",
            "Incident Reporting Systems",
            "Safety Training Coordination"
        });
        
        // Strategic Skills
        skillsByCategory.put("Business Development", new String[] {
            "Technical Sales Support",
            "Solution Design Review",
            "Competitive Analysis",
            "Partnership Development",
            "Service Offering Expansion",
            "Market Trend Analysis",
            "Customer Needs Assessment"
        });
        
        skillsByCategory.put("Continuous Improvement", new String[] {
            "Process Optimization",
            "Lessons Learned Implementation",
            "KPI Development and Tracking",
            "Root Cause Analysis",
            "Efficiency Metrics Management",
            "Technology Adoption Planning",
            "Automation Opportunity Identification"
        });
        
        // Communication Skills
        skillsByCategory.put("Reporting & Analytics", new String[] {
            "Executive Dashboard Creation",
            "Project Status Reporting",
            "Technical Writing",
            "Presentation Skills",
            "Data Visualization",
            "Trend Analysis",
            "Forecasting"
        });
        
        skillsByCategory.put("Cross-Functional Coordination", new String[] {
            "IT Department Liaison",
            "Operations Team Collaboration",
            "Vendor Management",
            "Engineering Coordination",
            "Warehouse Management Liaison",
            "C-Suite Communication",
            "Union Relations (if applicable)"
        });
        
        // Process the skills
        int totalAdded = 0;
        int totalSkipped = 0;
        
        for (Map.Entry<String, String[]> entry : skillsByCategory.entrySet()) {
            String category = entry.getKey();
            String[] skillNames = entry.getValue();
            
            logger.info("Processing category: {}", category);
            
            for (String skillName : skillNames) {
                try {
                    // Check if skill already exists
                    boolean exists = skillRepo.findAll().stream()
                        .anyMatch(s -> s.getName().equalsIgnoreCase(skillName));
                    
                    if (!exists) {
                        Skill skill = new Skill();
                        skill.setName(skillName);
                        skill.setCategory(category);
                        skill.setActive(true);
                        
                        // Add a description based on the skill name
                        skill.setDescription(generateDescription(skillName, category));
                        
                        skillRepo.save(skill);
                        totalAdded++;
                        logger.info("Added skill: {} in category: {}", skillName, category);
                    } else {
                        totalSkipped++;
                        logger.debug("Skill already exists: {}", skillName);
                    }
                } catch (Exception e) {
                    logger.error("Failed to add skill: {} - {}", skillName, e.getMessage());
                }
            }
        }
        
        logger.info("Bulk load complete. Added: {}, Skipped: {}", totalAdded, totalSkipped);
        System.out.println("\n=== Skill Bulk Load Complete ===");
        System.out.println("Total skills added: " + totalAdded);
        System.out.println("Total skills skipped (already exist): " + totalSkipped);
    }
    
    private String generateDescription(String skillName, String category) {
        // Generate a basic description based on the skill name and category
        if (skillName.contains("Management")) {
            return "Expertise in " + skillName.toLowerCase() + " within the " + category + " domain";
        } else if (skillName.contains("Analysis") || skillName.contains("Review")) {
            return "Ability to perform " + skillName.toLowerCase() + " as part of " + category;
        } else if (skillName.contains("Development") || skillName.contains("Creation")) {
            return "Skills in " + skillName.toLowerCase() + " for effective " + category;
        } else {
            return "Proficiency in " + skillName.toLowerCase() + " related to " + category;
        }
    }
}