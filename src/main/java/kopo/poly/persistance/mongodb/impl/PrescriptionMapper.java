package kopo.poly.persistance.mongodb.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IPrescriptionMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionMapper extends AbstractMongoDBComon implements IPrescriptionMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertPrescriptionInfo(String colNm, List<PrescriptionDTO> pList) throws Exception {

        log.info("{}.insertPrescriptionInfo Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        for (PrescriptionDTO pDTO : pList) {
            Document doc = new Document();
            doc.append("userId", CmmUtil.nvl(pDTO.getUserId()));
            doc.append("prescriptionDate", pDTO.getPrescriptionDate()); // Date 타입 직접 삽입
            doc.append("storeName", CmmUtil.nvl(pDTO.getStoreName()));
            doc.append("prescriptionPeriod", pDTO.getPrescriptionPeriod());
            doc.append("drugList", pDTO.getDrugList()); // 이미 Map 구조이므로 바로 넣음
            doc.append("dailyIntakeCnt", pDTO.getDailyIntakeCnt());
            doc.append("remindYn", CmmUtil.nvl(pDTO.getRemindYn()));

            col.insertOne(doc);
        }

        res = 1;

        log.info("{}.insertPrescriptionInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public List<PrescriptionDTO> getPrescriptionList(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.getPrescriptionInfo Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        List<PrescriptionDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userId", CmmUtil.nvl(pDTO.getUserId()));

        Document projection = new Document();
        projection.append("userId", 1);
        projection.append("prescriptionDate", 1);
        projection.append("storeName", 1);
        projection.append("prescriptionPeriod", 1);
        projection.append("drugList", 1);
        projection.append("dailyIntakeCnt", 1);
        projection.append("remindYn", 1);
        projection.append("_id", 1);

        Document sort = new Document("prescriptionDate", -1);

        FindIterable<Document> rs = col.find(query)
                .projection(projection)
                .sort(sort);

        for (Document doc : rs) {
            if (doc != null) {
                PrescriptionDTO rDTO = new PrescriptionDTO();

                rDTO.setUserId(CmmUtil.nvl(doc.getString("userId")));

                Object dateObj = doc.get("prescriptionDate");
                if (dateObj instanceof Date) {
                    rDTO.setPrescriptionDate((Date) dateObj);
                } else if (dateObj instanceof Long) {
                    rDTO.setPrescriptionDate(new Date((Long) dateObj));
                }

                rDTO.setStoreName(CmmUtil.nvl(doc.getString("storeName")));
                rDTO.setPrescriptionPeriod(doc.getInteger("prescriptionPeriod", 0));
                rDTO.setDailyIntakeCnt(doc.getInteger("dailyIntakeCnt", 0));
                rDTO.setRemindYn(CmmUtil.nvl(doc.getString("remindYn")));

                List<Document> drugListDocs = doc.getList("drugList", Document.class);
                List<Map<String, Object>> drugList = new ArrayList<>();
                if (drugListDocs != null) {
                    for (Document drugDoc : drugListDocs) {
                        drugList.add(drugDoc);
                    }
                }
                rDTO.setDrugList(drugList);

                ObjectId id = doc.getObjectId("_id");
                if (id != null) {
                    rDTO.setPrescriptionId(id.toHexString());
                }

                rList.add(rDTO);
            }
        }

        return rList;
    }

    @Override
    public PrescriptionDTO getLatestPrescriptionInfo(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.getLatestPrescriptionInfo Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        PrescriptionDTO rDTO = new PrescriptionDTO();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document();
        query.append("userId", CmmUtil.nvl(pDTO.getUserId()));

        Document projection = new Document();
        projection.append("userId", 1);
        projection.append("prescriptionDate", 1);
        projection.append("storeName", 1);

        Document latestDoc = col.find(query)
                .projection(projection)
                .sort(new Document("prescriptionDate", -1))
                .first();

        if (latestDoc != null) {
            rDTO.setUserId(CmmUtil.nvl(latestDoc.getString("userId")));

            Object dateObj = latestDoc.get("prescriptionDate");
            if (dateObj instanceof Date) {
                rDTO.setPrescriptionDate((Date) dateObj);
            } else if (dateObj instanceof Long) {
                rDTO.setPrescriptionDate(new Date((Long) dateObj));
            }

            rDTO.setStoreName(CmmUtil.nvl(latestDoc.getString("storeName")));

            return rDTO;
        }

        return null;
    }

    @Override
    public int updatePrescriptionInfo(String colNm, PrescriptionDTO pDTO) throws Exception {

        log.info("{}.updateReminderInfo Start", this.getClass().getName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // ObjectId로 변환
        ObjectId objectId = new ObjectId(CmmUtil.nvl(pDTO.getPrescriptionId()));

        // Update 문서 구성
        Document updateFields = new Document();
        updateFields.append("remindYn", CmmUtil.nvl(pDTO.getRemindYn()));
        updateFields.append("dailyIntakeCnt", pDTO.getDailyIntakeCnt());

        Document updateDoc = new Document("$set", updateFields);

        // 업데이트 실행
        UpdateResult result = col.updateOne(Filters.eq("_id", objectId), updateDoc);

        if (result.getModifiedCount() > 0) {
            res = 1;
        }

        log.info("{}.updateReminderInfo End", this.getClass().getName());

        return res;
    }

    @Override
    public PrescriptionDTO getPrescriptionById(String colNm, PrescriptionDTO pDTO) throws Exception {

        log.info("{}.getPrescriptionById Start", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        ObjectId objectId = new ObjectId(CmmUtil.nvl(pDTO.getPrescriptionId()));

        Document result = col.find(Filters.eq("_id", objectId)).first();

        PrescriptionDTO rDTO = new PrescriptionDTO();

        if (result != null) {
            rDTO.setPrescriptionId(result.getObjectId("_id").toHexString());
            rDTO.setUserId(result.getString("userId"));
            rDTO.setPrescriptionDate(result.getDate("prescriptionDate"));
            rDTO.setStoreName(result.getString("storeName"));
            rDTO.setPrescriptionPeriod(result.getInteger("prescriptionPeriod", 0));
            rDTO.setRemindYn(result.getString("remindYn"));
            rDTO.setDailyIntakeCnt(result.getInteger("dailyIntakeCnt", 0));
            // 필요한 경우 drugList 등 추가
        }

        log.info("{}.getPrescriptionById End", this.getClass().getName());

        return rDTO;
    }

    @Override
    public int updateUserId(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.updateMealTime Start", this.getClass().getSimpleName());

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String userId = pDTO.getOrgId();
        String updateUserId = pDTO.getUserId();

        Document query = new Document("userId", userId);
        Document update = new Document("$set", new Document("userId", updateUserId));

        UpdateResult result = col.updateMany(query, update);

        int toUpdateCnt = (int) result.getMatchedCount();
        int updatedCnt = (int) result.getModifiedCount();

        log.info("업데이트 대상 문서 수 : {}", toUpdateCnt);
        log.info("실제 업데이트된 문서 수 : {}", updatedCnt);

        if(toUpdateCnt == updatedCnt) {
            res = 1;
        }

        log.info("{}.updateMealTimeByUserId End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int deleteAllPrescription(String colNm, UserInfoDTO pDTO) throws Exception {

        log.info("{}.deleteAllPrescription Start", this.getClass().getSimpleName());

        // Mongo 컬렉션 가져오기
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 삭제 조건 (userId 기준)
        String userId = CmmUtil.nvl(pDTO.getUserId());

        // 실제 삭제 실행 (딱 하나만)
        long deletedCount = col.deleteMany(Filters.eq("userId", userId)).getDeletedCount();

        log.info("Deleted Prescription count: {}", deletedCount);

        log.info("{}.deleteAllPrescription End", this.getClass().getSimpleName());

        return (int) deletedCount;

    }
}
