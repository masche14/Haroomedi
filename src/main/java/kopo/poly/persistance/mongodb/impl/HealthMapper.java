package kopo.poly.persistance.mongodb.impl;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IHealthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthMapper extends AbstractMongoDBComon implements IHealthMapper {
    @Override
    public HRecordDTO getLatestRecord(HRecordDTO pDTO) throws Exception {
        return null;
    }

    @Override
    public int insertRecord(HRecordDTO pDTO) throws Exception {
        return 0;
    }
}
