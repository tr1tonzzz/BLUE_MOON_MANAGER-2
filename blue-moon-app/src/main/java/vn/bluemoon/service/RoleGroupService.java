package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Group;
import vn.bluemoon.repository.GroupRepository;
import vn.bluemoon.repository.RoleRepository;

import java.util.List;

/**
 * Role and Group service
 */
public class RoleGroupService {
    private final GroupRepository groupRepository = new GroupRepository();
    private final RoleRepository roleRepository = new RoleRepository();

    /**
     * Get all groups
     * @return List of groups
     * @throws DbException if database error occurs
     */
    public List<Group> getAllGroups() throws DbException {
        return groupRepository.findAll();
    }

    /**
     * Assign role to user
     * @param userId User ID
     * @param groupId Group ID
     * @throws DbException if database error occurs
     */
    public void assignRoleToUser(Integer userId, Integer groupId) throws DbException {
        if (!roleRepository.userHasRole(userId, groupId)) {
            roleRepository.assignRoleToUser(userId, groupId);
        }
    }

    /**
     * Remove role from user
     * @param userId User ID
     * @param groupId Group ID
     * @throws DbException if database error occurs
     */
    public void removeRoleFromUser(Integer userId, Integer groupId) throws DbException {
        roleRepository.removeRoleFromUser(userId, groupId);
    }

    /**
     * Get groups for user
     * @param userId User ID
     * @return List of groups
     * @throws DbException if database error occurs
     */
    public List<Group> getGroupsForUser(Integer userId) throws DbException {
        return groupRepository.findByUserId(userId);
    }
}

















