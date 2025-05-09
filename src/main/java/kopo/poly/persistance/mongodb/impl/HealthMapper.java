package kopo.poly.persistance.mongodb.impl;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.TilkoDTO;
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
    public PrescriptionDTO getLatestRecord(TilkoDTO pDTO) throws Exception {
        return null;
    }

    @Override
    public int insertRecord(PrescriptionDTO pDTO) throws Exception {
        return 0;
    }
}
