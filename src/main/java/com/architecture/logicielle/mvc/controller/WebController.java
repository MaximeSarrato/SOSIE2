package com.architecture.logicielle.mvc.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.architecture.logicielle.mvc.data.StageView;
import com.architecture.logicielle.mvc.data.UserView;
import com.architecture.logicielle.repository.StageRepository;
import com.architecture.logicielle.repository.UserRepository;
import com.architecture.logicielle.repository.entities.Stage;
import com.architecture.logicielle.repository.entities.UserEntity;
import com.architecture.logicielle.service.StageService;
import com.architecture.logicielle.service.StageServiceImpl;
import com.architecture.logicielle.service.UserService;
import com.architecture.logicielle.service.UserServiceImpl;

@Controller
public class WebController extends WebMvcConfigurerAdapter {
	@Autowired // This means to get the bean called userRepository
	// Which is auto-generated by Spring, we will use it to handle the data
	private UserRepository userRepository;
	private UserService userService = new UserServiceImpl();
	@Autowired
	private StageRepository stageRepository;
	private StageService stageService = new StageServiceImpl();

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
	}

	@GetMapping("/")
	public String showHomePage(Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long id = Long.parseLong(auth.getName());
		UserEntity userEnt = userService.GetUserById(id, userRepository);
		UserView userView = userService.parseUserEntityToUserView(userEnt);
		model.addAttribute("userView", userView);
		return "consultUser";
	}

	@GetMapping("/inscription")
	public String showFromInscription(Model model) {
		model.addAttribute("user", new UserView());
		return "inscription";
	}

	@PostMapping("/inscription")
	public String InscriptionSubmit(Model model, @ModelAttribute @Valid UserView user, BindingResult bindingResult) {
		model.addAttribute("user", user);

		if (bindingResult.hasErrors()) {
			model.addAttribute("ErrorMessage", "Inalid from !");
			return "inscription";
		} else {
			UserEntity userEnt = userService.parseUserViewToUserEntity(user);
			UserEntity userEntCheck = userService.checkUser(userEnt, userRepository);
			if (userEntCheck == null) {
				userService.saveUser(userEnt, userRepository);
			} else {
				model.addAttribute("ErrorMessage", "User Exist !");
				return "inscription";
			}

			return "redirect:/";
		}
	}

	@GetMapping("/edit")
	public String showEditPage(Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long id = Long.parseLong(auth.getName());
		UserEntity userEnt = userService.GetUserById(id, userRepository);
		UserView userView = userService.parseUserEntityToUserView(userEnt);
		model.addAttribute("user", userView);
		return "editUser";
	}

	@PostMapping("/edit")
	public String EditProfileSubmit(Model model, @ModelAttribute @Valid UserView user, BindingResult bindingResult) {

		model.addAttribute("user", user);
		if (bindingResult.hasErrors()) {
			model.addAttribute("ErrorMessage", "Inalid from !");
			return "editUser";
		} else {
			UserEntity userEnt = userService.parseUserViewToUserEntity(user);
			userService.saveUser(userEnt, userRepository);
			return "redirect:/";
		}
	}

	@GetMapping("/DeleteProfile/{userId}")
	public String DeleteUsert(@PathVariable Long userId, Model model, @ModelAttribute UserView user) {
		// fermer la session avant de supprimer le user
		SecurityContextHolder.clearContext();
		UserEntity userEnt = userService.GetUserById(userId, userRepository);
		List<Stage> stages = stageRepository.findByUsername(userEnt.getUsername());
		stageRepository.delete(stages);
		userService.deleteUser(userEnt, userRepository);

		return "redirect:/login";
	}

	@PostMapping("/editStage")
	public String Ajouter_Stage(Model model, @ModelAttribute @Valid StageView stage, BindingResult bindingResult) {
		model.addAttribute("stage", stage);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long id = Long.parseLong(auth.getName());
		UserEntity user = userRepository.findOne(id);
		UserView userView = userService.parseUserEntityToUserView(user);
		if (bindingResult.hasErrors()) {
			model.addAttribute("ErrorMessage", "Invalid form !");
			model.addAttribute("userView", userView);
			return "stage/editStage";
		} else {
			Stage stageSaisi = stageService.parseStageViewToStage(stage);
			Stage stageCheck = stageService.checkStage(stageSaisi, stageRepository);
			stageSaisi.setEleve(user);
			if(userRepository.findOne(stage.getUsernameEnseignantReferant()) != null) {
				stageSaisi.setEnseignantReferant(userRepository.findOne(stage.getUsernameEnseignantReferant()));
			
			if (stageCheck == null) {
				stageService.saveStage(stageSaisi, stageRepository);
			} else {
				model.addAttribute("ErrorMessage", "Stage Exist !");
				model.addAttribute("userView", userView);
				return "stage/editStage";
			}
			}
			else {
				model.addAttribute("ErrorMessage", "L'enseignant ajouté n'existe pas");
				model.addAttribute("userView", userView);
				return "stage/editStage";
			}

			return "redirect:/";
		}
	}

	@GetMapping("/editStage")
	public String showFromCreateStage(Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long id = Long.parseLong(auth.getName());
		UserEntity userEnt = userService.GetUserById(id, userRepository);
		UserView userView = userService.parseUserEntityToUserView(userEnt);
		if (!userEnt.getStatut().equals("Student")) {
			return "redirect:/consultStage";
		}
		else {
		if (stageService.checkStageByUser(id, stageRepository)) {
			Stage stage = stageService.GetStageByIdUser(id, stageRepository);
			StageView stageView = stageService.parseStageToStageView(stage);
			model.addAttribute("userView", userView);
			model.addAttribute("stage", stageView);
			return "stage/editStage";
		} else {
			model.addAttribute("userView", userView);
			model.addAttribute("stage", new StageView());
			return "stage/editStage";
		}
		}

	}

	@GetMapping("/consultStage")
	public String showConsultStage(Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long id = Long.parseLong(auth.getName());
		UserEntity userEnt = userService.GetUserById(id, userRepository);
		UserView userView = userService.parseUserEntityToUserView(userEnt);
		if (userEnt.getStatut().equals("administrator")) {
			model.addAttribute("ErrorMessage", "Vous n'avez pas accès aux stages");
			return "redirect:/";
		} else if (userEnt.getStatut().equals("teacher")) {

			if (stageService.checkStageByEns(id, stageRepository)) {
				List <Stage> stages = stageService.GetListStageByIdEns(id, stageRepository);
				List<StageView> stageView = new ArrayList<StageView>();
				for(int i=0;i<stages.size();i++)
				{
					stageView.add(stageService.parseStageToStageView(stages.get(i)));
				}
				model.addAttribute("userView", userView);
				model.addAttribute("stage", stageView);
				return "stage/consultStageEns";
			} else {
				model.addAttribute("ErrorMessage", "Vous n'êtes affecté à aucun stage");
				model.addAttribute("userView", userView);
				return "consultUser";
			}
		} else {
			if (!stageService.checkStageByUser(id, stageRepository)) {
				model.addAttribute("userView", userView);
				return "stage/noStage";
			} else {
				Stage stage = stageService.GetStageByIdUser(id, stageRepository);
				StageView stageView = stageService.parseStageToStageView(stage);
				model.addAttribute("userView", userView);
				model.addAttribute("stage", stageView);
				return "stage/consultStage";
			}
		}
	}
}
