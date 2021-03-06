package com.allbareun.web.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.allbareun.web.entity.Cycle;
import com.allbareun.web.entity.Goal;
import com.allbareun.web.entity.GoalAllParticipantsView;
import com.allbareun.web.entity.GoalCategory;
import com.allbareun.web.entity.GoalDetailView;
import com.allbareun.web.entity.GoalView;
import com.allbareun.web.entity.Group;
import com.allbareun.web.entity.Participation;
import com.allbareun.web.entity.User;
import com.allbareun.web.service.GoalService;

@Controller
@RequestMapping("/goal/")
public class GoalController {

	@Autowired
	private GoalService service;

	@GetMapping("reg")
	public String reg(Principal principal, Model model) {
		int userId = service.getUserIdByEmail(principal.getName());
		int goalId = service.makeGoal(userId);
		model.addAttribute("gId", goalId);
		
		return "user.goal.reg";
	}
	
	@GetMapping("reg/cancel")
	public String regCancel(@RequestParam(name = "id") int id) {
		service.delete(id);
		
		return "redirect:/mygoal/list";
	}

	@PostMapping("reg")
	public String reg(@RequestParam(name = "g-id") int id,
						@RequestParam(name = "g-mImg", defaultValue = "/images/default-image2.png") String mainImage,
						@RequestParam(name = "g-t") String title, @RequestParam(name = "g-ex") String explanation,
						@RequestParam(name = "g-sd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
						@RequestParam(name = "g-ed") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
						@RequestParam(name = "g-ps") boolean publicStatus,
						@RequestParam(name = "g-c") int count,
						@RequestParam(name = "g-tp") int totalParticipants,
						@RequestParam(name = "g-gEx", required = false) String goodEx,
						@RequestParam(name = "g-bEx", required = false) String badEx,
						@RequestParam(name = "g-exEx", required = false) String exExplanation,
						@RequestParam(name = "gct-id") int[] goalCategoryTypeIds,
						@RequestParam(name = "d-id") int[] dayIds,
						@RequestParam(name = "g-m", required = false) int[] members,
						Principal principal) {
		
		// 현재 user 정보 찾기
		int userId = service.getUserIdByEmail(principal.getName());
		
		// 목표 등록 전처리
		mainImage = "/upload/goal/" + id + "/" + mainImage;
		goodEx = "/upload/goal/" + id + "/" + goodEx;
		badEx = "/upload/goal/" + id + "/" + badEx;

		Goal goal = new Goal(id, title, explanation, mainImage, goodEx, badEx, endDate, startDate, publicStatus, null, count, userId, totalParticipants, exExplanation);
		
		List<GoalCategory> gcList = new ArrayList<>();
		List<Cycle> cList = new ArrayList<>();
		List<Group> gList = new ArrayList<>();

		// 카테고리 : 최대 2개
		for (int i = 0; i < goalCategoryTypeIds.length; i++)
			gcList.add(new GoalCategory(0, 0, goalCategoryTypeIds[i], i));

		// 인증 주기
		for (int i = 0; i < dayIds.length; i++)
			cList.add(new Cycle(0, 0, dayIds[i]));

		// 지인 그룹
		if (publicStatus == false && totalParticipants > 1)
			for (int i = 0; i < members.length; i++) {
				Group member = new Group();
				member.setRequestDispatchUserId(userId);
				member.setRequestReceiveUserId(members[i]);
				gList.add(member);
			}
		else
			gList = null;

		service.insert(goal, gcList, cList, gList);

		return "redirect:/mygoal/list";
	}

	@PostMapping("reg/upload")
	@ResponseBody
	public String upload(@RequestParam(name = "id") int id, MultipartFile file, HttpServletRequest request) throws IllegalStateException, IOException {
//		System.out.println(file.getOriginalFilename());
		
		String url = "/upload/goal/" + id;
	    String realPath = request.getServletContext().getRealPath(url);
//	    System.out.println("Real Path : " + realPath);

	    File realPathFile = new File(realPath);
	    if(!realPathFile.exists())
	    	realPathFile.mkdirs();
	    
	    String uploadedFilePath = realPath + File.separator + file.getOriginalFilename();
	    File uploadedFile = new File(uploadedFilePath);
	    
	    file.transferTo(uploadedFile);

		return "Server : upload file!";
	}
	
	@GetMapping("{id}")
	public String participate(@PathVariable("id") int id, Model model) {

		
		List<GoalAllParticipantsView> profile = service.getProfile(id);
		GoalDetailView detail = service.getDetailView(id);
		

		model.addAttribute("detail", detail);
		model.addAttribute("profile", profile);

		return "user.goal.participate";
	}

	@PostMapping("{id}")
	public String enter(Principal principal, Participation participation,User user) {

		
		  //int uid = Integer.parseInt(principal.getName()); // 로그인 인증 정보가 갖고와짐
		 
		 String email = principal.getName(); // 로그인 인증 정보가 갖고와짐
		 int uid = service.getinfo(email);
		 System.out.println(uid);
		 participation.setUserId(uid);

		int result = service.enter(participation);
		System.out.println("참가완료");

		return "redirect:../goal/" + participation.getId();

	}

	@GetMapping("list")

	public String list(@RequestParam(name = "categories", defaultValue = "") String categories,
			@RequestParam(name = "startDate", defaultValue = "") String startDate,
			@RequestParam(name = "endDate", defaultValue = "") String endDate,
			@RequestParam(name = "days", defaultValue = "") String days,
			@RequestParam(name = "count", defaultValue = "0") int totalParticipants,
			@RequestParam(name = "query", defaultValue = "") String query,
			@RequestParam(name = "count", defaultValue = "6") int count,
			@RequestParam(name = "offset", defaultValue = "0") int offset
			, Model model) {
		
		List<GoalView> list = service.getViewList(categories, startDate, endDate, days, totalParticipants, query,count,offset);
		
//		for (GoalView g : list) {
//			System.out.println(g);
//		}
		
		// color 변경
		for (int i = 0; i < list.size(); i++) {
			GoalView gv = list.get(i);
			String[] colors = list.get(i).getCategoriesColor().split(",");
			String[] categoryArr = list.get(i).getCategories().split(",");
			for (int j = 0; j < categoryArr.length; j++)
				categoryArr[j] = "<span style=\"color:" + colors[j] + "; font-weight:bold;\">" + categoryArr[j]
						+ "</span>";

			String categoryStr = String.join(",", categoryArr);
			list.get(i).setCategories(categoryStr);
		}

		model.addAttribute("list", list);

		return "common.goal.list";
	}
}
