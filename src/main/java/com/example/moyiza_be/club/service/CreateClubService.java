package com.example.moyiza_be.club.service;

import com.example.moyiza_be.club.dto.*;
import com.example.moyiza_be.club.entity.CreateClub;
import com.example.moyiza_be.club.repository.CreateClubRepository;
import com.example.moyiza_be.common.enums.CategoryEnum;
import com.example.moyiza_be.common.enums.GenderPolicyEnum;
import com.example.moyiza_be.common.enums.TagEnum;
import com.example.moyiza_be.common.security.userDetails.UserDetailsImpl;
import com.example.moyiza_be.common.utils.Message;
import com.example.moyiza_be.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateClubService {
    private final CreateClubRepository createClubRepository;
    private final ClubService clubService;
    public ResponseEntity<?> initCreateClubId(Long userId) {
        CreateClub previousCreate = createClubRepository.findByOwnerId(userId);
        if(previousCreate != null){
            log.info("found existing club by user " + userId);
            return new ResponseEntity<>(new ResumeIdResponse(previousCreate.getId()), HttpStatus.ACCEPTED);
        }
        CreateClub createClub = new CreateClub();
        createClub.setOwnerId(userId);
        createClubRepository.saveAndFlush(createClub);
        log.info("new club created : club ID " + createClub.getId());
        CreateClubIdResponse createClubIdResponse = new CreateClubIdResponse(createClub.getId());
        return new ResponseEntity<>(createClubIdResponse, HttpStatus.CREATED);
    }

    public ResponseEntity<ResumeCreationDto> getPreviousCreateClub(Long userId, Long createclub_id) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        log.info("returning previous createclub : " + createclub_id);
        return ResponseEntity.ok(new ResumeCreationDto(createClub));
    }

    public ResponseEntity<Message> setCategory(User user, Long createclub_id, CategoryEnum categoryEnum) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        createClub.setCategory(categoryEnum);
        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<Message> setTag(User user, Long createclub_id, List<TagEnum> tagEnumList) {
        CreateClub createClub = loadCreateClubById(createclub_id);

        String newString = "0".repeat(TagEnum.values().length);

        StringBuilder sb = new StringBuilder(newString);
        for(TagEnum tagEnum : tagEnumList){
            sb.setCharAt(tagEnum.ordinal(), '1');
        }
        createClub.setTagString(sb.toString());

        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<Message> setTitle(User user, Long createclub_id, String title) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        createClub.setTitle(title);
        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<Message> setContent(User user, Long createclub_id, String content) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        createClub.setContent(content);
        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<Message> setPolicy
            ( User user, Long createclub_id, Integer agePolicy, GenderPolicyEnum genderPolicyEnum)
    {
        CreateClub createclub = loadCreateClubById(createclub_id);
        createclub.setGenderPolicy(genderPolicyEnum);
        createclub.setAgePolicy(agePolicy);   // 모두 가능할시 null로 받아서 null로 세팅 가능한지 알아봐야함

        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<Message> setMaxGroupSize(User user, Long createclub_id, Integer requestMaxSize) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        createClub.setMaxGroupSize(requestMaxSize);
        return ResponseEntity.ok(new Message("성공"));
    }

    public ResponseEntity<ClubResponseDto> confirmCreation(User User, Long createclub_id) {
        CreateClub createClub = loadCreateClubById(createclub_id);
        ConfirmClubCreationDto confirmClubCreationDto = new ConfirmClubCreationDto(createClub);
        ClubResponseDto newClub = clubService.createClub(confirmClubCreationDto);
        return ResponseEntity.ok(newClub);
    }

    ///////////////////////private methods///////////////////

    private CreateClub loadCreateClubById(Long createclub_id) {
        return createClubRepository.findById(createclub_id)
                .orElseThrow(() -> new NullPointerException("생성중인 클럽을 찾을 수 없습니다"));
    }
}
