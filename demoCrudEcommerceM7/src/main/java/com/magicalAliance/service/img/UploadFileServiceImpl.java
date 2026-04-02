package com.magicalAliance.service.img;

import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class UploadFileServiceImpl implements IUploadFileService {

    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * Guardo archivos nuevos en mi almacenamiento externo (Disco C).
     */
    @Override
    public String copiar(MultipartFile archivo, String subCarpeta) {
        try {
            // 1. Genero un nombre único para que no haya choques de energía entre archivos.
            String nombreUnico = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();

            // 2. Busco la ruta física en mi disco (ej: C:/magical-alliance-uploads/categorias).
            Path rutaCarpeta = Paths.get(uploadPath).resolve(subCarpeta);

            // 3. Si la carpeta no existe en mi disco, la manifiesto (creo).
            if (!Files.exists(rutaCarpeta)) {
                Files.createDirectories(rutaCarpeta);
            }

            // 4. Copio el flujo de datos del archivo a mi carpeta física.
            Files.copy(archivo.getInputStream(), rutaCarpeta.resolve(nombreUnico));

            // Retorno solo el nombre único; mi MvcConfig se encarga del resto.
            return nombreUnico;

        } catch (IOException e) {
            throw new MagicalBusinessException("Mane, tuve un error de alquimia: No pude guardar la imagen en el servidor.");
        }
    }

    /**
     * Elimino archivos con sabiduría: protejo mis assets y limpio el Disco C.
     */
    @Override
    public boolean eliminar(String rutaImagen, String subCarpeta) {
        // 1. SEGURIDAD: Si no hay imagen o es mi banner base (banner-simple.jpg), no hago nada.
        if (rutaImagen == null || rutaImagen.isEmpty() || rutaImagen.equals("banner-simple.jpg")) {
            return false;
        }

        // 2. DETECCIÓN DE ASSETS:
        // Si la ruta contiene una barra (ej: "accesorios/foto.jpg"), es de mis carpetas internas.
        // ¡PROHIBIDO BORRAR DEL DISCO! Estos archivos son parte de mi código.
        if (rutaImagen.contains("/")) {
            // Retorno true para permitir que la DB limpie la referencia, pero el archivo físico vive.
            return true;
        }

        // 3. BORRADO FÍSICO (Solo para el Disco C):
        // Si el nombre es un UUID suelto, lo busco y lo elimino físicamente para no dejar basura.
        try {
            Path rutaAbsoluta = Paths.get(uploadPath).resolve(subCarpeta).resolve(rutaImagen);
            File archivo = rutaAbsoluta.toFile();

            if (archivo.exists() && archivo.canRead()) {
                return archivo.delete();
            }
        } catch (Exception e) {
            throw new MagicalBusinessException("Mane, hubo una perturbación al intentar eliminar el archivo físico: " + e.getMessage());
        }

        return false;
    }

    /**
     * Exploro mis carpetas internas para mostrarle al admin mi galería mística.
     */
    @Override
    public List<String> listarGaleria() {
        List<String> fotos = new ArrayList<>();
        try {
            // Intento localizar mi carpeta de recursos en el classpath (accesorios, vestuario, etc.)
            Resource resource = new ClassPathResource("static/assets/img/");

            if (!resource.exists()) {
                throw new MagicalNotFoundException("Mane, no pude encontrar mi carpeta de imágenes en static/assets/img/");
            }

            File rootFolder = resource.getFile();
            // Empiezo mi búsqueda recursiva por todas mis subcarpetas.
            buscarFotosRecursivo(rootFolder, "", fotos);

        } catch (IOException e) {
            throw new MagicalBusinessException("Mane, tuve un problema técnico al leer mis archivos de assets: " + e.getMessage());
        }
        return fotos;
    }

    /**
     * Mi método privado para navegar por cada rincón de mis carpetas de imágenes.
     */
    private void buscarFotosRecursivo(File folder, String pathAcumulado, List<String> listaFotos) {
        File[] files = folder.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Si entro en una subcarpeta, acumulo su nombre (ej: "accesorios/")
                buscarFotosRecursivo(file, pathAcumulado + file.getName() + "/", listaFotos);
            } else if (file.isFile() && esImagenValida(file.getName())) {
                // No incluyo mi banner base en la lista de productos para evitar confusiones.
                if (!file.getName().equals("banner-simple.jpg")) {
                    listaFotos.add(pathAcumulado + file.getName());
                }
            }
        }
    }

    /**
     * Verifico que el archivo sea una de las extensiones que permito en mi e-commerce.
     */
    private boolean esImagenValida(String nombreArchivo) {
        String n = nombreArchivo.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".jpeg") || n.endsWith(".webp");
    }
}