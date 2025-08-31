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
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoginMapper extends AbstractMongoDBComon implements ILoginMapper {

    private final MongoTemplate mongodb;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Date kstMonthStart(Date anyDay) {
        YearMonth ym = YearMonth.from(anyDay.toInstant().atZone(KST));
        return Date.from(ym.atDay(1).atStartOfDay(KST).toInstant());
    }

    private Date kstNextMonthStart(Date anyDay) {
        YearMonth ym = YearMonth.from(anyDay.toInstant().atZone(KST)).plusMonths(1);
        return Date.from(ym.atDay(1).atStartOfDay(KST).toInstant());
    }

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

        LocalDate startLocalDate = pDTO.getStartDate().toInstant()
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate();

        int selectedYear = startLocalDate.getYear();
        int selectedMonth = startLocalDate.getMonthValue();

        Date startInclusive = kstMonthStart(pDTO.getStartDate());
        Date endExclusive   = kstNextMonthStart(pDTO.getStartDate());

        log.info("starDate : {}", pDTO.getStartDate());
        log.info("endDate : {}", pDTO.getEndDate());

        // 디버깅: UTC로도 찍어서 확인(중요)
        log.info("startInclusive(UTC): {}", startInclusive.toInstant()); // 예: 2025-07-31T15:00:00Z
        log.info("endExclusive(UTC)  : {}", endExclusive.toInstant());   // 예: 2025-08-31T15:00:00Z

        log.info("selectedYear : {}", selectedYear);
        log.info("selectedMonth : {}", selectedMonth);

        List<LoginDTO> rList;

        Aggregation agg = Aggregation.newAggregation(
                // ① KST 자정 경계로 매치
                Aggregation.match(new Criteria().andOperator(
                        Criteria.where("loginAt").gte(startInclusive).lt(endExclusive),
                        Criteria.where("role").is("user")
                )),

                // ② 날짜 문자열을 KST로 생성 (여기서 'loginAt'에 대해 추가 +9h 하지 말 것!)
                Aggregation.project("userId")
                        .and(
                                DateOperators.dateOf("loginAt")
                                        .toString("%Y-%m-%d")
                                        .withTimezone(DateOperators.Timezone.valueOf("Asia/Seoul"))
                        ).as("loginDateString"),

                // (선택) ③ 선택 월만 다시 한 번 안전 필터
                //   ex) 2025-08 월만 남기기
                Aggregation.match(
                        Criteria.where("loginDateString").regex("^" + selectedYear + "-" + String.format("%02d", selectedMonth) + "-")
                ),

                // ④ 날짜+유저로 유니크 만든 뒤 날짜만 카운트
                Aggregation.group("loginDateString", "userId"),
                Aggregation.group("_id.loginDateString").count().as("userCount"),
                Aggregation.project("userCount").and("_id").as("loginDateString"),
                Aggregation.sort(Sort.Direction.ASC, "loginDateString")
        );


        rList = mongodb.aggregate(agg, colNm, LoginDTO.class).getMappedResults();

        log.info("{}.getDailyDistinctUserCountByMonth End", this.getClass().getSimpleName());

        return rList;
    }
}
