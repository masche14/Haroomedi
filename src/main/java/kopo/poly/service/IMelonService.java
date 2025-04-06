package kopo.poly.service;

import kopo.poly.dto.MelonDTO;

import java.util.List;

public interface IMelonService {

    int collectMelonSong() throws Exception;

    List<MelonDTO> getSongList() throws Exception;
}
