package kopo.poly.service.impl;

import kopo.poly.dto.MelonDTO;
import kopo.poly.persistance.mongodb.IMelonMapper;
import kopo.poly.service.IMelonService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MelonService implements IMelonService {

    private final IMelonMapper melonMapper;

    private List<MelonDTO> doCollect() throws Exception{
        log.info("{}.doCollect start",this.getClass().getSimpleName());

        List<MelonDTO> pList = new LinkedList<>();

        String url = "https://www.melon.com/chart/index.htm";

        Document doc = Jsoup.connect(url).get();

        Elements element = doc.select("div.service_list_song");

        for (Element songInfo : element.select("div.wrap_song_info")) {

            String song = CmmUtil.nvl(songInfo.select("div.ellipsis.rank01 a").text());
            String singer = CmmUtil.nvl(songInfo.select("div.ellipsis.rank02 a").eq(0).text());

            log.info("song : {}",  song);
            log.info("singer : {}",  singer);

            if (!song.isEmpty() && !singer.isEmpty()) {

                MelonDTO pDTO = MelonDTO.builder().collectTime(DateUtil.getDateTime("yyyyMMddhhmmss"))
                        .song(song).singer(singer).build();

                pList.add(pDTO);
            }
        }

        log.info("{}.doCollect end",this.getClass().getSimpleName());

        return pList;
    }

    @Override
    public int collectMelonSong() throws Exception {

        int res;

        String colNm = "Melon_"+DateUtil.getDateTime("yyyyMMdd");

        List<MelonDTO> rList = this.doCollect();

        res = melonMapper.insertSong(rList, colNm);

        log.info("{}.collectMelonSong end",this.getClass().getSimpleName());

        return res;
    }

    @Override
    public List<MelonDTO> getSongList() throws Exception {

        log.info("{}.getSongList start",this.getClass().getSimpleName());

        String colNm = "Melon_"+DateUtil.getDateTime("yyyyMMdd");
        List<MelonDTO> rList = melonMapper.getSongList(colNm);
        log.info("{}.getSongList end",this.getClass().getSimpleName());

        return rList;
    }
}
