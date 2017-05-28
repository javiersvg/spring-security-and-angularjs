package com.example;

import org.springframework.data.repository.CrudRepository;

/**
 * Created by javier.mino on 5/28/2017.
 */
public interface ApplicationUserRepository extends CrudRepository<ApplicationUser, String> {
}
