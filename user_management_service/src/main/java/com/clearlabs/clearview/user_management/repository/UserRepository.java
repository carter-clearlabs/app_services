package com.clearlabs.clearview.user_management.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.clearlabs.clearview.user_management.model.User;

@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends PagingAndSortingRepository<User, Long> {

	List<User> findByLastName(String lastName);
	List<User> findByEmail(String email);
	
}
