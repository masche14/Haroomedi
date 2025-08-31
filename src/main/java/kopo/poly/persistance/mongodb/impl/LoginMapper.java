package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import kopo.poly.dto.LoginDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.ILoginMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoginMapper extends AbstractMongoDBComon implements ILoginMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertLoginInfo(String colNm, LoginDTO pDTO) throws Exception {
        log.info("{}.insertLoginInfo Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document doc = new Document();
        doc.append("userId", pDTO.getUserId());
        doc.append("userName", pDTO.getUserName());
        doc.append("role", pDTO.getRole());
        doc.append("loginAt", pDTO.getLoginAt());

        col.insertOne(doc);

        res = 1;

        log.info("{}.insertLoginInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int getTodayDistinctUserCount(String colNm) throws Exception {

        log.info("{}.getTodayDistinctUserCount Start", this.getClass().getSimpleName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        LocalDate today = LocalDate.now();
        Date start = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Query query = new Query(
                new Criteria().andOperator(
                        Criteria.where("loginAt").gte(start).lt(end),
                        Criteria.where("role").is("user")
                )
        );

        res = mongodb.findDistinct(query, "userId", colNm, String.class).size();

        log.info("{}.getTodayDistinctUserCount End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public List<LoginDTO> getDailyDistinctUserCountByMonth(String colNm, LoginDTO pDTO) throws Exception {

        log.info("{}.getDailyDistinctUserCountByMonth Start", this.getClass().getSimpleName());

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        cal.setTime(pDTO.getEndDate());
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endExclusive = cal.getTime();

        List<LoginDTO> rList = null;

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(
                        new Criteria().andOperator(
                                Criteria.where("loginAt").gte(pDTO.getStartDate()).lt(endExclusive),
                                Criteria.where("role").is("user") // ✅ "user" 권한만 포함
                        )
                ),
                Aggregation.project("userId")
                        .andExpression("dateToString('%Y-%m-%d', loginAt)").as("loginDateString"),
                Aggregation.group("loginDateString", "userId"),
                Aggregation.group("_id.loginDateString").count().as("userCount"),
                Aggregation.project("userCount")
                        .and("_id").as("loginDateString"),
                Aggregation.sort(Sort.Direction.ASC, "loginDateString")
        );

        rList = mongodb.aggregate(agg, colNm, LoginDTO.class).getMappedResults();

        log.info("{}.getDailyDistinctUserCountByMonth End", this.getClass().getSimpleName());

        return rList;
    }
}
