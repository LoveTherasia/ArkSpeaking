package com.organization.repository;

import com.organization.pojo.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

//角色数据访问层
@Repository
public interface CharacterRepository extends JpaRepository<Character, Long>, JpaSpecificationExecutor<Character> {
    boolean existsByIsPresetTrueAndId(Long id);
}
