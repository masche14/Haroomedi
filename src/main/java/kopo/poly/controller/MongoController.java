package kopo.poly.controller;

import jakarta.validation.Valid;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MongoDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.service.IMongoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value = "/mongo/v1")
@RequiredArgsConstructor
@RestController
public class MongoController {

    private final IMongoService mongoService;

    @PostMapping(value = "basic")
    public ResponseEntity basic (@Valid @RequestBody MongoDTO pDTO, BindingResult bindingResult) throws Exception{
        log.info(this.getClass().getName()+".basic Start!");

        if (bindingResult.hasErrors()) {
            return CommonResponse.getErrors(bindingResult);
        }

        String msg="";

        log.info("pDTO : "+pDTO);

        int res = mongoService.mognoTest(pDTO);

        if (res==1) {
            msg = "저장 성공하였습니다.";
        } else {
            msg = "저장 실패하였습니다.";
        }

        MsgDTO dto = MsgDTO.builder().result(res).msg(msg).build();

        log.info(this.getClass().getName()+".basic End!");

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
        );
    }
}
