package com.organization.service;

import com.organization.pojo.Character;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CharacterService {
    public void initPresetCharacters();

    public List<Character> getCharacterList();

    public Character getCharacterById(Long id);

    public String getCharacterPromptById(Long Id)throws IOException;

    public Character createCustomCharacter(String name, MultipartFile avatarFile, String promptContent) throws IOException;

    public boolean deleteCharacter(Long Id);


}
