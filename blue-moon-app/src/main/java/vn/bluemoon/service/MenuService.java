package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Menu;
import vn.bluemoon.repository.MenuRepository;

import java.util.List;

/**
 * Menu service
 */
public class MenuService {
    private final MenuRepository menuRepository = new MenuRepository();

    /**
     * Get all menus
     * @return List of menus
     * @throws DbException if database error occurs
     */
    public List<Menu> getAllMenus() throws DbException {
        return menuRepository.findAll();
    }

    /**
     * Get root menus
     * @return List of root menus
     * @throws DbException if database error occurs
     */
    public List<Menu> getRootMenus() throws DbException {
        return menuRepository.findRootMenus();
    }

    /**
     * Create menu
     * @param menu Menu to create
     * @return Created menu
     * @throws DbException if database error occurs
     */
    public Menu createMenu(Menu menu) throws DbException {
        return menuRepository.create(menu);
    }

    /**
     * Get menu by ID
     * @param id Menu ID
     * @return Menu
     * @throws DbException if database error occurs
     */
    public Menu getMenuById(Integer id) throws DbException {
        return menuRepository.findById(id);
    }
}

















