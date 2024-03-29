package com.utilsbot.repository;

import com.utilsbot.domain.UserData;
import com.utilsbot.domain.UserDataIds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, UserDataIds> {
}
