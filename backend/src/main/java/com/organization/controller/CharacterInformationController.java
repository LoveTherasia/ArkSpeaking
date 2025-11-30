package com.organization.controller;

import com.organization.pojo.CharacterInformation;
import com.organization.service.LoadCharacterInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CharacterInformationController {
    @Autowired
    private LoadCharacterInformationService loadCharacterInformationService;

    @GetMapping("/chat/fetch")
    public CharacterInformation fetchCharacterInformation(@RequestParam(name="characterName")String characterName) {
        return loadCharacterInformationService.loadCharacterInformation(characterName);
    }
}
