package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRepositoryTest {
    private DatabaseConfig databaseConfig;
    private ProjectRepository projectRepository;
    private Path testDbPath;

    @BeforeEach
    void setUp() throws IOException {
        testDbPath = Files.createTempDirectory("test-scheduler-db");
        databaseConfig = new DatabaseConfig(testDbPath.toString() + "/");
        projectRepository = new ProjectRepository(databaseConfig.getDataSource());
    }

    @AfterEach
    void tearDown() throws IOException {
        databaseConfig.shutdown();
        Files.deleteIfExists(Paths.get(testDbPath.toString(), "scheduler.db"));
        Files.deleteIfExists(testDbPath);
    }

    @Test
    void shouldSaveAndRetrieveProject() {
        Project project = new Project("PROJ-001", "Test Project", 
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        Project saved = projectRepository.save(project);
        
        assertNotNull(saved.getId());
        assertEquals("PROJ-001", saved.getProjectId());
        assertEquals("Test Project", saved.getDescription());
        assertEquals(LocalDate.of(2025, 8, 1), saved.getStartDate());
        assertEquals(LocalDate.of(2025, 8, 15), saved.getEndDate());
        assertEquals(ProjectStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void shouldFindProjectById() {
        Project project = new Project("PROJ-002", "Another Test Project",
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 20));
        Project saved = projectRepository.save(project);

        Optional<Project> found = projectRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("PROJ-002", found.get().getProjectId());
        assertEquals("Another Test Project", found.get().getDescription());
    }

    @Test
    void shouldFindProjectByProjectId() {
        Project project = new Project("PROJ-003", "Project ID Test",
                LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 25));
        projectRepository.save(project);

        Optional<Project> found = projectRepository.findByProjectId("PROJ-003");
        
        assertTrue(found.isPresent());
        assertEquals("Project ID Test", found.get().getDescription());
    }

    @Test
    void shouldUpdateProject() {
        Project project = new Project("PROJ-004", "Original Description",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10));
        Project saved = projectRepository.save(project);

        saved.setDescription("Updated Description");
        saved.setStatus(ProjectStatus.COMPLETED);
        projectRepository.update(saved);

        Optional<Project> updated = projectRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals("Updated Description", updated.get().getDescription());
        assertEquals(ProjectStatus.COMPLETED, updated.get().getStatus());
    }

    @Test
    void shouldDeleteProject() {
        Project project = new Project("PROJ-005", "To Be Deleted",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5));
        Project saved = projectRepository.save(project);

        projectRepository.delete(saved.getId());

        Optional<Project> deleted = projectRepository.findById(saved.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    void shouldFindProjectsByStatus() {
        Project activeProject = new Project("PROJ-006", "Active Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10));
        projectRepository.save(activeProject);

        Project completedProject = new Project("PROJ-007", "Completed Project",
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 15));
        completedProject.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(completedProject);

        List<Project> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        List<Project> completedProjects = projectRepository.findByStatus(ProjectStatus.COMPLETED);
        
        assertEquals(1, activeProjects.size());
        assertEquals("PROJ-006", activeProjects.get(0).getProjectId());
        
        assertEquals(1, completedProjects.size());
        assertEquals("PROJ-007", completedProjects.get(0).getProjectId());
    }

    @Test
    void shouldFindProjectsByDateRange() {
        Project earlyProject = new Project("PROJ-008", "Early Project",
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 15));
        projectRepository.save(earlyProject);

        Project overlappingProject = new Project("PROJ-009", "Overlapping Project",
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 20));
        projectRepository.save(overlappingProject);

        Project lateProject = new Project("PROJ-010", "Late Project",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 15));
        projectRepository.save(lateProject);

        List<Project> projectsInAugust = projectRepository.findByDateRange(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));
        
        assertEquals(1, projectsInAugust.size());
        assertEquals("PROJ-009", projectsInAugust.get(0).getProjectId());
    }

    @Test
    void shouldCheckProjectExistence() {
        Project project = new Project("PROJ-011", "Existence Test",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10));
        Project saved = projectRepository.save(project);

        assertTrue(projectRepository.existsById(saved.getId()));
        assertTrue(projectRepository.existsByProjectId("PROJ-011"));
        assertFalse(projectRepository.existsById(999L));
        assertFalse(projectRepository.existsByProjectId("NONEXISTENT"));
    }

    @Test
    void shouldCountProjects() {
        assertEquals(0, projectRepository.count());

        projectRepository.save(new Project("PROJ-012", "Count Test 1",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10)));
        assertEquals(1, projectRepository.count());

        projectRepository.save(new Project("PROJ-013", "Count Test 2",
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 15)));
        assertEquals(2, projectRepository.count());
    }
}