package com.clearlabs.clearview.user_management.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.clearlabs.clearview.user_management.model.User;
import com.clearlabs.clearview.user_management.repository.UserRepository;

@RestController
//@RequestMapping("/users")
public class UserController {

/*	private UserRepository userRepository;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public User findById(@PathVariable("id") Long id) {
		return userRepository.findOne(id);
	}*/
}
