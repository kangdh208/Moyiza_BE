package com.example.moyiza_be.event.service;


import com.example.moyiza_be.club.entity.Club;
import com.example.moyiza_be.club.repository.ClubRepository;
import com.example.moyiza_be.event.dto.EventAttendantResponseDto;
import com.example.moyiza_be.event.dto.EventCreateResponseDto;
import com.example.moyiza_be.event.dto.EventRequestDto;
import com.example.moyiza_be.event.dto.EventUpdateRequestDto;
import com.example.moyiza_be.event.entity.Event;
import com.example.moyiza_be.event.entity.EventAttendant;
import com.example.moyiza_be.event.repository.EventAttendantRepository;
import com.example.moyiza_be.event.repository.EventRepository;
import com.example.moyiza_be.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final EventAttendantRepository attendantRepository;

    // 이벤트 생성
    @Transactional
    public ResponseEntity<?> createEvent (EventRequestDto eventRequestDto, User user, long clubId) {
        // 클럽이 유효한가
        Optional<Club> club = clubRepository.findById(clubId);
        if (club.isEmpty()) {
            throw new IllegalArgumentException("404 Not Found");
        }
        // 작성자가 소유자인가
        if (user.getId()!=clubId) {
            throw new IllegalArgumentException("401 UnAuthorized");
        }
        // 생성 + 삭제상태 : false + 참석자수 : 1(방장)
        Event event = new Event(eventRequestDto, user.getId(), clubId); // 이미지 넣으면 user, image로 변경
        event.setDeleted(false);
        event.setAttendantsNum(1);
        eventRepository.saveAndFlush(event);
        return new ResponseEntity<>("생성 성공", HttpStatus.OK);
    }

    // 이벤트 수정 : 보류긴 한데...
    @Transactional
    public ResponseEntity<?> updateEvent(long id, EventUpdateRequestDto requestDto, User user) throws IOException {
        // 이벤트 가져오기
        Event event = eventRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("404 Not Found"));
        // 시간 처리 어떻게 해야 하는가?
        // 존재하는 글인가? 삭제 처리하는 Entity 값 추가 필요성 논의
//        if (event.isDeleted()) {
//            throw new IllegalArgumentException("404 Not Found");
//        }
        // 이미지?
//        String image = null;
//        if (!Objects.isNull(requestDto.getImage()) && !requestDto.getImage().isEmpty() && !requestDto.getImage().getContentType().isEmpty()) {
//            image = s3Uploader.upload(requestDto.getImage(), "image");
//        }
        // 작성자인가?
//        if (Objects.equals(user.getId(), event.getUser().getId())) {
//            event.updateAll(requestDto);
//            removeCache(event);
//        } else {
//            throw new IllegalArgumentException("401 UnAuthorized");
//        }
        // 작성자이면 수정
        // 아니면 PASS
        return new ResponseEntity<>("수정 성공", HttpStatus.OK);
    }

    // 이벤트 조회
    @Transactional
    public Optional<Event> getEvent(long clubId, long eventId) {
        Optional<Event> eventDetailResponseDto = eventRepository.findById(eventId);
        // 있는 모임인가?
        if (eventDetailResponseDto.isEmpty()) throw new IllegalArgumentException("400 Bad Request");
        return eventDetailResponseDto;
    }

    // 전체 이벤트 조회 : 보류긴 한데
    public List<Event> getEventList(long clubId) { //ResponseEntity GenericType ListEntity
        List<Event> eventList = eventRepository.findAllByClubId(clubId);
        return eventList;
    }

    // 이벤트 삭제
    @Transactional
    public ResponseEntity<?> deleteEvent(long clubId, long eventId, User user) {
//        User user = SecurityUtil.getCurrentUser(); 이방식 말고 일단은 AuthPrincipal로 먼저
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new IllegalArgumentException("404 event Not found"));
        if (event.isDeleted()) { // 삭제를 T?F로 처리하면 좋을것 같은데...?
            throw new IllegalArgumentException("404 event not found");
        }
        if (user.getId().equals(event.getOwnerId())) {
            eventRepository.deleteById(eventId);
            event.setDeleted(true);
        } else {
            throw new IllegalArgumentException("401 Not Authorized");
        }
        return new ResponseEntity<>("삭제 성공", HttpStatus.OK);
    }

    // 이벤트 참석 / 취소
    @Transactional
    public EventAttendantResponseDto addAttendant(long clubId, long eventId, User user) {
        // user 로그인 되어있니?
        if (user == null) throw new IllegalArgumentException("401 UnAuthorized");
        // club 가입은 했니?
        // 여기 어떻게 짤까
        Event event = (Event) eventRepository.findByIdAndDeletedIsFalse(eventId).orElseThrow(
                () -> new IllegalArgumentException("404 Not Found")
        );
        // 참석취소자가 방장일경우 참석취소 불가
        if(event.getOwnerId()==user.getId()){
            throw new IllegalArgumentException("방장은 취소가 불가능해요 ㅠ.ㅠ");
        }
        // 참석자테이블에 존재하는가??
        EventAttendant attendant = (EventAttendant) attendantRepository.findByEventAndUser(event, user).orElseGet(()->new EventAttendant(event, user));

        if (attendant == null) {
            // 최대정원 도달시 참석불가
            if (event.getEventGroupSize() <= event.getAttendantsNum()) {
                throw new IllegalArgumentException("Fully Occupied");
            }
            // 참석하지 않은 유저인 경우 참석으로 하고 참석자수++
            EventAttendant eventAttendant = attendantRepository.save(new EventAttendant(event, user));
            event.addAttend();
            return new EventAttendantResponseDto(eventAttendant);
        } else {
            // 기존에 참석했던 유저의 경우 참석자 명단에서 삭제하고 참석자수--
            attendantRepository.delete(attendant);
            event.cancelAttend();
            return null;
        }
    }

}
