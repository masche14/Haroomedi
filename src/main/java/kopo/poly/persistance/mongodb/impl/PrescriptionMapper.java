package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IPrescriptionMapper;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.Map;

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
            col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class)));
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
        projection.append("userId", "$userId");
        projection.append("prescriptionDate", "$prescriptionDate");
        projection.append("storeName", "$storeName");
        projection.append("prescriptionPeriod", "$prescriptionPeriod");
        projection.append("drugList", "$drugList");
        projection.append("dailyIntakeCnt", "$dailyIntakeCnt");
        projection.append("remindYn", "$remindYn");
        projection.append("_id", 1);

        // 날짜 기준 내림차순 정렬
        Document sort = new Document("prescriptionDate", -1);

        // 쿼리 실행
        FindIterable<Document> rs = col.find(query)
                .projection(projection)
                .sort(sort);  // 정렬 추가

        for (Document doc : rs) {
            if (doc != null) {
                PrescriptionDTO rDTO = new PrescriptionDTO();

                String userId = CmmUtil.nvl(doc.getString("userId"));
                Object dateObj = doc.get("prescriptionDate");

                Date prescriptionDate;
                if (dateObj instanceof Date) {
                    prescriptionDate = (Date) dateObj;
                } else if (dateObj instanceof Long) {
                    prescriptionDate = new Date((Long) dateObj);
                } else {
                    throw new IllegalArgumentException("Unsupported date format: " + dateObj.getClass().getName());
                }

                String storeName = CmmUtil.nvl(doc.getString("storeName"));
                int prescriptionPeriod = doc.getInteger("prescriptionPeriod");

                List<Document> drugListDocs = doc.getList("drugList", Document.class);
                List<Map<String, Object>> drugList = new ArrayList<>();
                for (Document drugDoc : drugListDocs) {
                    drugList.add(drugDoc);
                }

                int dailyIntakeCnt = doc.getInteger("dailyIntakeCnt");
                ObjectId id = doc.getObjectId("_id");

                rDTO.setUserId(userId);
                rDTO.setPrescriptionDate(prescriptionDate);
                rDTO.setStoreName(storeName);
                rDTO.setPrescriptionPeriod(prescriptionPeriod);
                rDTO.setDrugList(drugList);
                rDTO.setDailyIntakeCnt(dailyIntakeCnt);
                rDTO.setRemindYn(doc.getString("remindYn"));
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
        projection.append("userId", "$userId");
        projection.append("prescriptionDate", "$prescriptionDate");
        projection.append("storeName", "$storeName");

        Document latestDoc = col.find(query)
                .projection(projection)
                .sort(new Document("prescriptionDate", -1))
                .first();

        if (latestDoc != null) {
            rDTO.setUserId(CmmUtil.nvl(latestDoc.getString("userId")));
            Object dateObj = latestDoc.get("prescriptionDate");

            Date prescriptionDate;
            if (dateObj instanceof Date) {
                prescriptionDate = (Date) dateObj;
            } else if (dateObj instanceof Long) {
                prescriptionDate = new Date((Long) dateObj);
            } else {
                throw new IllegalArgumentException("Unsupported date format: " + dateObj.getClass().getName());
            }

            rDTO.setPrescriptionDate(prescriptionDate);
            rDTO.setStoreName(latestDoc.getString("storeName"));

            return rDTO;
        }

        return null;
    }
}
