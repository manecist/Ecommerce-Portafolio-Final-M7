package com.magicalAliance.service.img;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IUploadFileService {

    String copiar(MultipartFile archivo, String subCarpeta) throws IOException;

    List<String> listarGaleria();

    // Actualizamos aquí para que reciba la subcarpeta (categorias o productos)
    boolean eliminar(String nombreImagen, String subCarpeta);
}