package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IUserInfoMapper;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInfoMapper extends AbstractMongoDBComon implements IUserInfoMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.insertUserInfo Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class)));

        res = 1;

        log.info("{}.insertUserInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public UserInfoDTO checkFieldExists(String colNm, UserInfoDTO pDTO) {

        log.info("{}.checkFieldExists Start", this.getClass().getSimpleName());

        Query query = new Query(Criteria.where(pDTO.getFieldName()).is(pDTO.getValue()));
        String existYn = mongodb.exists(query, colNm)? "Y" : "N";

        log.info("existYn : {}", existYn);

        UserInfoDTO rDTO = new UserInfoDTO();

        switch (pDTO.getFieldName()) {
            case "userEmail" -> {
                rDTO.setUserEmail(pDTO.getValue());
                rDTO.setExistYn(existYn);
            }
            case "userId" -> {
                rDTO.setUserId(pDTO.getValue());
                rDTO.setExistYn(existYn);
            }
            case "userNickname" -> {
                rDTO.setUserNickname(pDTO.getValue());
                rDTO.setExistYn(existYn);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 필드명입니다: " + pDTO.getFieldName());
        }


        log.info("rDTO : {}", rDTO);

        log.info("{}.checkFieldExists End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public UserInfoDTO getUserIdAndUserNameByUserEmail(String colNm, UserInfoDTO pDTO) {

        log.info("{}.getUserId Start", this.getClass().getSimpleName());

        UserInfoDTO rDTO = new UserInfoDTO();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userEmail", CmmUtil.nvl(pDTO.getValue()));

        Document projection = new Document();
        projection.append("userId", "$userId");
        projection.append("userName", "$userName");
        projection.append("_id", 0);

        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {
            if (doc != null) {
                String userId = CmmUtil.nvl(doc.getString("userId"));
                String userName = CmmUtil.nvl(doc.getString("userName"));

                rDTO.setUserId(userId);
                rDTO.setUserName(userName);
                break;
            }
        }

        log.info("{}.getUserId End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public UserInfoDTO getUserInfoByUserId(String colNm, UserInfoDTO pDTO) throws Exception {
        log.info("{}.getUserInfoByUserId Start", this.getClass().getSimpleName());

        UserInfoDTO rDTO = new UserInfoDTO();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userId", CmmUtil.nvl(pDTO.getUserId()));

        Document projection = new Document();
        projection.append("userId", "$userId");
        projection.append("userName", "$userName");
        projection.append("phoneNumber", "$phoneNumber");
        projection.append("userEmail", "$userEmail");
        projection.append("_id", 0);

        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {
            if (doc != null) {
                String userId = CmmUtil.nvl(doc.getString("userId"));
                String userName = CmmUtil.nvl(doc.getString("userName"));
                String phoneNumber = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("phoneNumber")));
                String userEmail = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("userEmail")));

                rDTO.setUserId(userId);
                rDTO.setUserName(userName);
                rDTO.setPhoneNumber(phoneNumber);
                rDTO.setUserEmail(userEmail);
                break;
            }
        }

        log.info("{}.getUserInfoByUserId End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public UserInfoDTO getLogin(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.getLogin Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        UserInfoDTO rDTO = new UserInfoDTO();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userId", CmmUtil.nvl(pDTO.getUserId()));

        Document projection = new Document();
        projection.append("userId", "$userId");
        projection.append("password", "$password");
        projection.append("userName", "$userName");
        projection.append("userEmail", "$userEmail");
        projection.append("userNickname", "$userNickname");
        projection.append("gender", "$gender");
        projection.append("birthDate", "$birthDate");
        projection.append("mealTime", "$mealTime");
        projection.append("phoneNumber", "$phoneNumber");
        projection.append("_id", 0);

        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {
            if (doc != null) {
                String userId = CmmUtil.nvl(doc.getString("userId"));
                String password = CmmUtil.nvl(doc.getString("password"));
                String userName = CmmUtil.nvl(doc.getString("userName"));
                String userEmail = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("userEmail")));
                String userNickname = CmmUtil.nvl(doc.getString("userNickname"));
                String gender = CmmUtil.nvl(doc.getString("gender"));
                String birthDate = CmmUtil.nvl(doc.getString("birthDate"));
                List<String> mealTime = doc.getList("mealTime", String.class);
                String phoneNumber = EncryptUtil.decAES128CBC(CmmUtil.nvl(doc.getString("phoneNumber")));


                rDTO.setUserId(userId);
                rDTO.setUserName(userName);
                rDTO.setUserEmail(userEmail);
                rDTO.setUserNickname(userNickname);
                rDTO.setPassword(password);
                rDTO.setGender(gender);
                rDTO.setBirthDate(birthDate);
                rDTO.setMealTime(mealTime);
                rDTO.setPhoneNumber(phoneNumber);

                break; // 어차피 하나만 찾을 거니까 첫 번째에서 바로 종료
            }
        }

        return rDTO;
    }

    @Override
    public int updateUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {
        Query query = new Query(Criteria.where("userId").is(pDTO.getOrgId())); // 수정 기준

        Update updateFields = new Update();

        int res = 0;

        // null 체크 후 업데이트 대상에 추가
        if (!CmmUtil.nvl(pDTO.getUserId()).isBlank()) {
            updateFields.set("userId", pDTO.getUserId());
        }
        if (!CmmUtil.nvl(pDTO.getPassword()).isBlank()) {
            updateFields.set("password", pDTO.getPassword());
        }
        if (!CmmUtil.nvl(pDTO.getUserName()).isBlank()) {
            updateFields.set("userName", pDTO.getUserName());
        }
        if (!CmmUtil.nvl(pDTO.getUserEmail()).isBlank()) {
            updateFields.set("userEmail", pDTO.getUserEmail());
        }
        if (!CmmUtil.nvl(pDTO.getUserNickname()).isBlank()) {
            updateFields.set("userNickname", pDTO.getUserNickname());
        }
        if (!CmmUtil.nvl(pDTO.getGender()).isBlank()) {
            updateFields.set("gender", pDTO.getGender());
        }
        if (!CmmUtil.nvl(pDTO.getChgId()).isBlank()) {
            updateFields.set("chgId", pDTO.getChgId());
        }
        if (!CmmUtil.nvl(pDTO.getChgDt()).isBlank()) {
            updateFields.set("chgDt", pDTO.getChgDt());
        }
        if (pDTO.getMealTime() != null && !pDTO.getMealTime().isEmpty()) {
            updateFields.set("mealTime", pDTO.getMealTime());
        }
        if (pDTO.getMealCnt() != 0) {
            updateFields.set("mealCnt", pDTO.getMealCnt());
        }
        if (!CmmUtil.nvl(pDTO.getPhoneNumber()).isBlank()) {
            updateFields.set("phoneNumber", pDTO.getPhoneNumber());
        }

        if (updateFields.getUpdateObject().isEmpty()) {
            res = 1;// 수정할 값 없음
            return res;
        }

        UpdateResult result = mongodb.updateFirst(query, updateFields, colNm);

        res = (int) result.getModifiedCount();

        return res;
    }

    @Override
    public int deleteUserInfo(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.deleteUserInfo Start", this.getClass().getSimpleName());

        // Mongo 컬렉션 가져오기
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 삭제 조건 (userId 기준)
        String userId = CmmUtil.nvl(pDTO.getUserId());

        // 실제 삭제 실행 (딱 하나만)
        long deletedCount = col.deleteOne(Filters.eq("userId", userId)).getDeletedCount();

        log.info("Deleted user count: {}", deletedCount);
        log.info("{}.deleteUserInfo End", this.getClass().getSimpleName());

        return (int) deletedCount;

    }
}
