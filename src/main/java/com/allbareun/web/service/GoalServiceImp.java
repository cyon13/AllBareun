package com.allbareun.web.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allbareun.web.dao.CalendarDao;
import com.allbareun.web.dao.CertificationDao;
import com.allbareun.web.dao.CycleDao;
import com.allbareun.web.dao.EvaluationDao;
import com.allbareun.web.dao.GoalCategoryDao;
import com.allbareun.web.dao.GoalDao;
import com.allbareun.web.dao.GroupDao;
import com.allbareun.web.dao.ParticipationDao;
import com.allbareun.web.dao.UserDao;
import com.allbareun.web.entity.Calendar;
import com.allbareun.web.entity.CertDetailView;
import com.allbareun.web.entity.Certification;
import com.allbareun.web.entity.CertificationView;
import com.allbareun.web.entity.Cycle;
import com.allbareun.web.entity.EvaluationView;
import com.allbareun.web.entity.Goal;
import com.allbareun.web.entity.GoalAllView;
import com.allbareun.web.entity.GoalCategory;
import com.allbareun.web.entity.GoalDetailView;
import com.allbareun.web.entity.GoalView;
import com.allbareun.web.entity.Group;
import com.allbareun.web.entity.Participation;
import com.allbareun.web.entity.User;

@Service
public class GoalServiceImp implements GoalService {

	@Autowired
	private GoalDao goalDao;
	@Autowired
	private GoalCategoryDao goalCategoryDao;
	@Autowired
	private CycleDao cycleDao;
	@Autowired
	private GroupDao groupDao;
	@Autowired
	private ParticipationDao participationDao;

	@Autowired
	private CertificationDao certificationDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private EvaluationDao evaluationDao;
	@Autowired
	private CalendarDao calendarDao;

	@Override
	@Transactional
	public int insert(Goal goal, List<GoalCategory> gcList, List<Cycle> cList, List<Group> gList) {

		int result = 0;
		int goalId = goal.getId();
		goalDao.update(goal);

		// 카테고리
		for (GoalCategory gc : gcList) {
			gc.setGoalId(goalId);
			goalCategoryDao.insert(gc);
		}

		// 인증 주기
		for (Cycle c : cList) {
			c.setGoalId(goalId);
			cycleDao.insert(c);
		}

		// 지인 그룹
		if (gList != null)
			for (Group g : gList) {
				g.setGoalId(goalId);
				groupDao.insert(g);
			}

		result++;

		return result;
	}

	@Override
	public int update(Goal goal) {

		int result = 0;
		goalDao.update(goal);
		result++;

		return result;

	}

	@Override
	@Transactional
	public int deleteGoalFromUser(Goal goal, List<GoalCategory> gcList, List<Cycle> cList, List<Group> gList) {
		int result = 0;

		int goalId = goal.getId();
		int userId = goal.getUserId();

		Goal origin = goalDao.get(goalId);
		boolean originPub = origin.getPublicStatus();
		int originTotalParticipants = origin.getTotalParticipants();

		// -------------------------- 전처리 --------------------------
		// 개인
		if (!originPub && originTotalParticipants == 1 || userId == origin.getUserId()) {
			goal.setUserId(0);
			goalDao.update(goal);
		}
		// 그룹 : 익명 & 지인
		else if (originTotalParticipants > 1) {
			groupDao.update(goalId, userId, false);
			participationDao.delete(goalId, userId);
		}

		// -------------------------- 후처리 --------------------------
		if (goal.getUserId() == 0)
			goal.setUserId(userId);

		result++;
		return result;
	}

	@Override
	@Transactional
	public int retryGoal(Goal goal, List<GoalCategory> gcList, List<Cycle> cList, List<Group> gList, int newGoalId) {
		int result = 0;

		this.deleteGoalFromUser(goal, gcList, cList, gList);
		goal.setId(newGoalId);
		this.insert(goal, gcList, cList, gList);

		result++;

		return result;
	}

	@Override
	public int delete(int goalId) {

		return goalDao.delete(goalId);
	}

	public GoalDetailView getDetailView(int id) {
		// TODO Auto-generated method stub
		return goalDao.getDetailView(id);
	}

	@Override
	public List<GoalView> getViewList(String categories, String startDate, String endDate, String days,
			int totalParticipants, String query) {
		// TODO Auto-generated method stub
		return goalDao.getViewList(categories, startDate, endDate, days, totalParticipants, query,0,0);
	}

	@Override
	public List<GoalView> getViewList(String categories, String startDate, String endDate, String days,
			int totalParticipants, String query, int count, int offset) {
		// TODO Auto-generated method stub
		return goalDao.getViewList(categories, startDate, endDate, days, totalParticipants, query, count, offset);

	}

	public List<User> getProfile(int id) {
		// TODO Auto-generated method stub
		return goalDao.getProfile(id);
	}

	@Override
	public List<CertificationView> getAuthImages(int id) {
		// TODO Auto-generated method stub
		return goalDao.getAuthImages(id);
	}
//	////////////////////////////////////////////////

	@Override
	public Goal get(int id) {

		return goalDao.get(id);
	}

	@Override
	public List<GoalAllView> getAllViewList(int userId, String status, String[] categories, int totalParticipants, int achievement, String query) {

		List<GoalAllView> list = goalDao.getAllViewList(userId, status, categories, totalParticipants, achievement, query);

		// Set Category Color & Set Participants Profile Image
		for (GoalAllView gav : list) {
			// Set Category Color
			String[] colors = gav.getCategoriesColor().split(",");
			String[] categoryArr = gav.getCategories().split(",");

			for (int j = 0; j < categoryArr.length; j++)
				categoryArr[j] = "<span style=\"color:" + colors[j] + "; font-weight:bold;\">" + categoryArr[j]
						+ "</span>";

			String categoryStr = String.join(",", categoryArr);
			gav.setCategories(categoryStr);

			// Set Participants Profile Image
			String[] profilesSrc = gav.getProfiles().split(",");
			String[] participants = gav.getParticipants().split(",");

			for (int j = 0; j < participants.length; j++) {
				participants[j] = "<div class=\"profile\"><img class=\"profile__image\" src=\"" + profilesSrc[j]
						+ "\" />" + "<span class=\"profile__info\">" + participants[j] + "</span></div>";
			}

			String profilesStr = String.join("", participants);
			gav.setParticipants(profilesStr);
		}

		return list;
	}

	@Override
	public GoalAllView getAllView(int id) {
		GoalAllView gav = goalDao.getAllView(id);

		// Set Participants Profile Image
		String[] profilesSrc = gav.getProfiles().split(",");
		String[] participants = gav.getParticipants().split(",");

		for (int j = 0; j < participants.length; j++) {
			participants[j] = "<div class=\"profile\"><img class=\"profile__image\" src=\"" + profilesSrc[j] + "\" />"
					+ "<span class=\"profile__info\">" + participants[j] + "</span></div>";
		}

		String profilesStr = String.join("", participants);
		gav.setParticipants(profilesStr);

		return gav;
	}

	@Override
	public List<CertificationView> getCertViewListById(int goalId) {
		// TODO Auto-generated method stub
		return certificationDao.getCertViewListById(goalId, null, null, null);
	}

	@Override
	public List<CertificationView> getCertViewListById(int goalId, String name, String startDate, String endDate) {
		// TODO Auto-generated method stub
		return certificationDao.getCertViewListById(goalId, name, startDate, endDate);
	}

	@Override
	public String getParticipantsId(int goalId) {
		// TODO Auto-generated method stub
		return goalDao.getParticipantsId(goalId);
	}

	@Override
	public List<User> getUserProfile(String ids) {
		List<User> list = new ArrayList<User>();

		String[] idStr = ids.split(",");
		int[] id = Arrays.stream(idStr).mapToInt(Integer::parseInt).toArray();
		for (int i = 0; i < id.length; i++) {
			User user = userDao.getById(id[i]);
			// String profile = user.getProfile();
			list.add(user);
		}
		return list;
	}

	@Override
	public List<String> getUserName(String ids) {
		List<String> list = new ArrayList<String>();

		String[] idStr = ids.split(",");
		int[] id = Arrays.stream(idStr).mapToInt(Integer::parseInt).toArray();
		for (int i = 0; i < id.length; i++) {
			User user = userDao.getById(id[i]);
			String name = user.getName();
			list.add(name);
		}
		return list;
	}

	@Override
	public CertDetailView getCertDetailView(int id) {
		// TODO Auto-generated method stub
		return certificationDao.getCertDetailView(id);
	}

	@Override
	public CertDetailView getPrev(int id, int goalId) {
		// TODO Auto-generated method stub
		return certificationDao.getPrev(id, goalId);
	}

	@Override
	public CertDetailView getNext(int id, int goalId) {
		// TODO Auto-generated method stub
		return certificationDao.getNext(id, goalId);
	}

	@Override
	public int authImageInsert(int id, int goalId, String authImage) {
		// TODO Auto-generated method stub
		return certificationDao.insert(id, goalId, authImage);
	}

	@Override
	public int deleteAuthImage(int id, int goalId, String fileName) {
		// TODO Auto-generated method stub
		return certificationDao.deleteAuthImage(id, goalId, fileName);
	}

	public int enter(Participation participation) {
		return goalDao.enter(participation);
		// TODO Auto-generated method stub

	}

	@Override
	@Transactional
	public int certAndEvalInsert(int goalId, int userId, String filePath, int answer1, int answer2, int answer3,
			String explanation) {
		// TODO Auto-generated method stub
		int cert = certificationDao.insert(userId, goalId, filePath);
		Certification c = certificationDao.getLast(userId, goalId);
		int certId = c.getId();
		System.out.println("certId" + certId);
		int eval = evaluationDao.insert(userId, goalId, answer1, answer2, answer3, explanation, certId);
		return 1;
	}

	public int getinfo(String email) {
		// TODO Auto-generated method stub
		return goalDao.getinfo(email);
	}

	public int getUserIdByEmail(String email) {

		return userDao.getUserId(email);
	}

	@Override
	public List<EvaluationView> getReport(int uid) {
		// TODO Auto-generated method stub
		return evaluationDao.getReport(uid);
	}

	@Override
	public List<EvaluationView> categoryChart(int uid) {
		// TODO Auto-generated method stub
		return evaluationDao.categoryChart(uid);
	}

	@Override
	public List<EvaluationView> getDoneLineChart(int id, int uid) {
		// TODO Auto-generated method stub
		return evaluationDao.getDoneLineChart(id, uid);
	}

//	@Override
//	public List<EvaluationView> getDoneBarChart(int id, int uid) {
//		// TODO Auto-generated method stub
//		return evaluationDao.getDoneBarChart(id,uid);
//	}

	@Override
	public List<String> getDays(int goalId) {
		// TODO Auto-generated method stub
		return cycleDao.getDays(goalId);
	}

	@Override
	@Transactional
	public int makeGoal(int userId) {
		goalDao.makeGoal(userId);
		Goal insertedGoal = goalDao.getLastInserted(userId);

		return insertedGoal.getId();
	}

	@Override
	public boolean isValidUserByEamil(String email) {
		boolean valid = false;
		User user = userDao.getUserByEamil(email);
		if(user != null)
			valid = true;
		return valid;
	}

	@Override
	public User getUser(int id) {
		
		return userDao.getuserById(id);
	}
	
	public List<CertificationView> getVideoImage(int id) {
		// TODO Auto-generated method stub
		return certificationDao.getVideoImage(id);
	}

	@Override
	public Date getStartDate(int id) {
		// TODO Auto-generated method stub
		return goalDao.getStartDate(id);
	}

	@Override
	public Date getEndDate(int id) {
		// TODO Auto-generated method stub
		return  goalDao.getEndDate(id);
	}

	@Override
	public List<EvaluationView> getDoneBarChart(String startDate) {
		// TODO Auto-generated method stub
		return evaluationDao.getDoneBarChart(startDate);
	}

	@Override
	public List<GoalAllView> getInvitedList(int userId, String[] categories, String query) {
		List<GoalAllView> list = goalDao.getInvitedList(userId, categories, query);

		// Set Category Color & Set Participants Profile Image
		for (GoalAllView gav : list) {
			// Set Category Color
			String[] colors = gav.getCategoriesColor().split(",");
			String[] categoryArr = gav.getCategories().split(",");

			for (int j = 0; j < categoryArr.length; j++)
				categoryArr[j] = "<span style=\"color:" + colors[j] + "; font-weight:bold;\">" + categoryArr[j]
						+ "</span>";

			String categoryStr = String.join(",", categoryArr);
			gav.setCategories(categoryStr);

			// Set Participants Profile Image
			String[] profilesSrc = gav.getProfiles().split(",");
			String[] participants = gav.getParticipants().split(",");

			for (int j = 0; j < participants.length; j++) {
				participants[j] = "<div class=\"profile\"><img class=\"profile__image\" src=\"" + profilesSrc[j]
						+ "\" />" + "<span class=\"profile__info\">" + participants[j] + "</span></div>";
			}

			String profilesStr = String.join("", participants);
			gav.setParticipants(profilesStr);
		}

		return list;
	}

	@Override
	public int rejectGoal(int goalId, int userId) {
		
		return groupDao.delete(goalId, userId);
	}

	@Override
	public int acceptGoal(int challengeGoalId, int userId) {
		
		return groupDao.update(challengeGoalId, userId, true);
	}

	@Override
	public List<Calendar> getByUserId(Calendar calendar) {
		
		return calendarDao.getByUserId(calendar);
	}
}