package com.subliminalsearch.simpleprojectresourcemanager.service;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.util.*;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.User;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceTypeConstants;

/**
 * Service for Active Directory/LDAP integration
 * Handles authentication, authorization, and user/group management
 */
public class LDAPService {
    
    // Configuration - these would typically come from a config file
    private static final String LDAP_URL = "ldap://your-domain-controller.company.com:389";
    private static final String DOMAIN = "company.com";
    private static final String BASE_DN = "DC=company,DC=com";
    
    // Your AD Groups
    private static final String PROJECT_MANAGERS_GROUP = "CN=Project Managers,OU=Groups,DC=company,DC=com";
    private static final String CYBERMETAL_GROUP = "CN=CyberMetal,OU=Groups,DC=company,DC=com";
    
    private LdapContext context;
    
    public enum UserRole {
        ADMIN,           // Full system access
        PROJECT_MANAGER, // Can manage all projects
        SUPERVISOR,      // Can view all, edit some
        TECHNICIAN,      // Can only view their assignments
        VIEWER           // Read-only access
    }
    
    /**
     * Authenticate user with Windows credentials
     */
    public User authenticate(String username, String password) throws NamingException {
        String userPrincipal = username + "@" + DOMAIN;
        
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userPrincipal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        
        try {
            context = new InitialLdapContext(env, null);
            
            // Authentication successful, now get user details
            User user = getUserDetails(username);
            
            // Set user role based on group membership
            user.setRole(determineUserRole(username));
            
            return user;
        } catch (AuthenticationException e) {
            throw new SecurityException("Invalid username or password");
        }
    }
    
    /**
     * Get user details from AD
     */
    public User getUserDetails(String username) throws NamingException {
        User user = new User();
        user.setUsername(username);
        
        String searchFilter = "(&(objectClass=user)(sAMAccountName=" + username + "))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{
            "cn", "mail", "department", "title", "manager", "memberOf", "displayName", "telephoneNumber"
        });
        
        NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);
        
        if (results.hasMore()) {
            SearchResult result = results.next();
            Attributes attrs = result.getAttributes();
            
            user.setFullName(getAttributeValue(attrs, "displayName"));
            user.setEmail(getAttributeValue(attrs, "mail"));
            user.setDepartment(getAttributeValue(attrs, "department"));
            user.setTitle(getAttributeValue(attrs, "title"));
            user.setPhone(getAttributeValue(attrs, "telephoneNumber"));
            
            // Get group memberships
            List<String> groups = new ArrayList<>();
            Attribute memberOf = attrs.get("memberOf");
            if (memberOf != null) {
                for (int i = 0; i < memberOf.size(); i++) {
                    String groupDN = (String) memberOf.get(i);
                    groups.add(extractGroupName(groupDN));
                }
            }
            user.setGroups(groups);
        }
        
        return user;
    }
    
    /**
     * Determine user role based on AD group membership
     */
    public UserRole determineUserRole(String username) throws NamingException {
        List<String> userGroups = getUserGroups(username);
        
        // Check group membership in order of precedence
        if (userGroups.contains("Domain Admins") || userGroups.contains("IT Admins")) {
            return UserRole.ADMIN;
        }
        if (userGroups.contains("Project Managers")) {
            return UserRole.PROJECT_MANAGER;
        }
        if (userGroups.contains("Supervisors") || userGroups.contains("Team Leads")) {
            return UserRole.SUPERVISOR;
        }
        if (userGroups.contains("CyberMetal")) {
            return UserRole.TECHNICIAN;
        }
        
        return UserRole.VIEWER; // Default role
    }
    
    /**
     * Check if user is member of specific AD group
     */
    public boolean isUserInGroup(String username, String groupName) throws NamingException {
        List<String> groups = getUserGroups(username);
        return groups.contains(groupName);
    }
    
    /**
     * Get all users in a specific AD group
     */
    public List<User> getGroupMembers(String groupName) throws NamingException {
        List<User> members = new ArrayList<>();
        
        String searchFilter = "(&(objectClass=group)(cn=" + groupName + "))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"member"});
        
        NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);
        
        if (results.hasMore()) {
            SearchResult result = results.next();
            Attributes attrs = result.getAttributes();
            Attribute memberAttr = attrs.get("member");
            
            if (memberAttr != null) {
                for (int i = 0; i < memberAttr.size(); i++) {
                    String memberDN = (String) memberAttr.get(i);
                    User member = getUserFromDN(memberDN);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }
        }
        
        return members;
    }
    
    /**
     * Sync AD users to application resources
     * Creates Resource entries for CyberMetal group members
     */
    public List<Resource> syncFieldTechnicians() throws NamingException {
        List<Resource> resources = new ArrayList<>();
        List<User> cyberMetalMembers = getGroupMembers("CyberMetal");
        
        for (User user : cyberMetalMembers) {
            Resource resource = new Resource();
            resource.setResourceName(user.getFullName());
            resource.setResourceType(ResourceTypeConstants.TECHNICIAN);
            resource.setEmail(user.getEmail());
            resource.setPhone(user.getPhone());
            resource.setDepartment(user.getDepartment());
            resource.setActive(true);
            resource.setLdapUsername(user.getUsername());
            
            // Set skill level based on title or other attributes
            if (user.getTitle() != null) {
                if (user.getTitle().contains("Senior")) {
                    resource.setSkillLevel("Senior");
                } else if (user.getTitle().contains("Lead")) {
                    resource.setSkillLevel("Lead");
                } else {
                    resource.setSkillLevel("Standard");
                }
            }
            
            resources.add(resource);
        }
        
        return resources;
    }
    
    /**
     * Get Project Managers from AD
     */
    public List<User> getProjectManagers() throws NamingException {
        return getGroupMembers("Project Managers");
    }
    
    /**
     * Create SharePoint-compatible permissions based on AD groups
     */
    public Map<String, String> getSharePointPermissions(String username) throws NamingException {
        Map<String, String> permissions = new HashMap<>();
        
        UserRole role = determineUserRole(username);
        
        switch (role) {
            case ADMIN:
                permissions.put("SharePoint", "Full Control");
                permissions.put("List", "Owner");
                permissions.put("Calendar", "Owner");
                break;
            case PROJECT_MANAGER:
                permissions.put("SharePoint", "Edit");
                permissions.put("List", "Contributor");
                permissions.put("Calendar", "Editor");
                break;
            case SUPERVISOR:
                permissions.put("SharePoint", "Edit");
                permissions.put("List", "Contributor");
                permissions.put("Calendar", "Reviewer");
                break;
            case TECHNICIAN:
                permissions.put("SharePoint", "Read");
                permissions.put("List", "Reader");
                permissions.put("Calendar", "Free/Busy");
                break;
            default:
                permissions.put("SharePoint", "Read");
                permissions.put("List", "Visitor");
                permissions.put("Calendar", "Free/Busy");
        }
        
        return permissions;
    }
    
    /**
     * Validate current session
     */
    public boolean validateSession() {
        try {
            if (context != null) {
                context.reconnect(null);
                return true;
            }
        } catch (NamingException e) {
            // Session expired
        }
        return false;
    }
    
    /**
     * Get users for auto-complete/search
     */
    public List<User> searchUsers(String searchTerm) throws NamingException {
        List<User> users = new ArrayList<>();
        
        String searchFilter = "(&(objectClass=user)(|(cn=*" + searchTerm + "*)(sAMAccountName=*" + searchTerm + "*)))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setCountLimit(20); // Limit results
        
        NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);
        
        while (results.hasMore()) {
            SearchResult result = results.next();
            User user = getUserFromSearchResult(result);
            users.add(user);
        }
        
        return users;
    }
    
    // Helper methods
    
    private List<String> getUserGroups(String username) throws NamingException {
        List<String> groups = new ArrayList<>();
        
        String searchFilter = "(&(objectClass=user)(sAMAccountName=" + username + "))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"memberOf"});
        
        NamingEnumeration<SearchResult> results = context.search(BASE_DN, searchFilter, searchControls);
        
        if (results.hasMore()) {
            SearchResult result = results.next();
            Attributes attrs = result.getAttributes();
            Attribute memberOf = attrs.get("memberOf");
            
            if (memberOf != null) {
                for (int i = 0; i < memberOf.size(); i++) {
                    String groupDN = (String) memberOf.get(i);
                    groups.add(extractGroupName(groupDN));
                }
            }
        }
        
        return groups;
    }
    
    private String extractGroupName(String groupDN) {
        // Extract CN from DN (e.g., "CN=Group Name,OU=Groups,DC=company,DC=com")
        if (groupDN.startsWith("CN=")) {
            int endIndex = groupDN.indexOf(",");
            return groupDN.substring(3, endIndex);
        }
        return groupDN;
    }
    
    private String getAttributeValue(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr != null) {
            return (String) attr.get();
        }
        return null;
    }
    
    private User getUserFromDN(String userDN) throws NamingException {
        // Extract username from DN and get full user details
        String username = extractUsernameFromDN(userDN);
        if (username != null) {
            return getUserDetails(username);
        }
        return null;
    }
    
    private String extractUsernameFromDN(String userDN) {
        // Extract CN or sAMAccountName from DN
        if (userDN.startsWith("CN=")) {
            int endIndex = userDN.indexOf(",");
            return userDN.substring(3, endIndex);
        }
        return null;
    }
    
    private User getUserFromSearchResult(SearchResult result) throws NamingException {
        User user = new User();
        Attributes attrs = result.getAttributes();
        
        user.setUsername(getAttributeValue(attrs, "sAMAccountName"));
        user.setFullName(getAttributeValue(attrs, "displayName"));
        user.setEmail(getAttributeValue(attrs, "mail"));
        user.setDepartment(getAttributeValue(attrs, "department"));
        
        return user;
    }
    
    public void close() {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException e) {
                // Log error
            }
        }
    }
}