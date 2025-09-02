import com.subliminalsearch.simpleprojectresourcemanager.util.SkillBulkLoader;

public class RunSkillBulkLoader {
    public static void main(String[] args) {
        SkillBulkLoader loader = new SkillBulkLoader();
        loader.loadSkills();
        System.exit(0);
    }
}