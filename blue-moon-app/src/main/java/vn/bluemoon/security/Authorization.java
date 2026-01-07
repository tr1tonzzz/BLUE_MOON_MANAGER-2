package vn.bluemoon.security;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Group;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.FunctionRepository;
import vn.bluemoon.repository.GroupRepository;

import java.util.List;

/**
 * Authorization utility class
 */
public class Authorization {
    private static final GroupRepository groupRepository = new GroupRepository();
    private static final FunctionRepository functionRepository = new FunctionRepository();

    /**
     * Check if user has access to a function
     * @param user User to check
     * @param boundaryClass Boundary class name of the function
     * @return true if user has access
     */
    public static boolean hasAccess(User user, String boundaryClass) throws DbException {
        if (user == null) {
            return false;
        }

        // Quản trị viên có toàn quyền
        if (hasRole(user, "Quản trị viên")) {
            return true;
        }

        // Get all groups for the user
        List<Group> groups = groupRepository.findByUserId(user.getId());
        
        // Check if any group has access to the function
        for (Group group : groups) {
            List<vn.bluemoon.model.entity.Function> functions = functionRepository.findByGroupId(group.getId());
            for (vn.bluemoon.model.entity.Function function : functions) {
                if (boundaryClass.equals(function.getBoundaryClass())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if user has a specific role
     * @param user User to check
     * @param groupName Group name
     * @return true if user has the role
     */
    public static boolean hasRole(User user, String groupName) throws DbException {
        if (user == null) {
            return false;
        }

        List<Group> groups = groupRepository.findByUserId(user.getId());
        for (Group group : groups) {
            if (groupName.equals(group.getName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if user is admin (Quản trị viên)
     * Admin has all permissions
     */
    public static boolean isAdmin(User user) throws DbException {
        return hasRole(user, "Quản trị viên");
    }
    
    /**
     * Check if user can manage residents (Admin or Tổ trưởng)
     */
    public static boolean canManageResidents(User user) throws DbException {
        return isAdmin(user) || hasRole(user, "Tổ trưởng");
    }
    
    /**
     * Check if user can manage fee types (Admin or Tổ trưởng)
     */
    public static boolean canManageFeeTypes(User user) throws DbException {
        return isAdmin(user) || hasRole(user, "Tổ trưởng");
    }
    
    /**
     * Check if user can collect fees (Admin or Kế toán)
     */
    public static boolean canCollectFees(User user) throws DbException {
        return isAdmin(user) || hasRole(user, "Kế toán");
    }
    
    /**
     * Check if user can manage users (Admin or Tổ trưởng)
     */
    public static boolean canManageUsers(User user) throws DbException {
        return isAdmin(user) || hasRole(user, "Tổ trưởng");
    }
}


