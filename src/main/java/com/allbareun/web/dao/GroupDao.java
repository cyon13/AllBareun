package com.allbareun.web.dao;

import com.allbareun.web.entity.Group;

public interface GroupDao {

	int insert(Group group);
	int update(int goalId, int userId, boolean answer);
	int delete(int goalId, int userId);

}
