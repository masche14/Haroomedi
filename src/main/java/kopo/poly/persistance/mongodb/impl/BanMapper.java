package kopo.poly.persistance.mongodb.impl;

import kopo.poly.dto.BanDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IBanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanMapper extends AbstractMongoDBComon implements IBanMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertBanInfo(String colNm, BanDTO pDTO) throws Exception {
        return 0;
    }

    @Override
    public List<BanDTO> getBanList(String colNm) throws Exception {
        return List.of();
    }
}
