package ru.practicum.ewm.user.core;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.user.core.model.User;


public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

}
