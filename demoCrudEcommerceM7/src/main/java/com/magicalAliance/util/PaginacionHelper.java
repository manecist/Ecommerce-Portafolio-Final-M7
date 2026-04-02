package com.magicalAliance.util;

import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

/**
 * Utilidad de paginación in-memory para los listados de administración.
 * Recibe la lista completa, la página solicitada y el tamaño de página,
 * retorna la sublista correspondiente e inyecta los metadatos en el Model.
 */
public final class PaginacionHelper {

    private PaginacionHelper() {}

    /**
     * Pagina la lista y añade al model los atributos necesarios para el fragment de paginación.
     *
     * @param lista Lista completa de elementos
     * @param page  Página solicitada (0-indexed)
     * @param size  Cantidad de elementos por página
     * @param model Spring Model donde se añaden los metadatos
     * @return Sublista correspondiente a la página solicitada
     */
    public static <T> List<T> paginar(List<T> lista, int page, int size, Model model) {
        int safeSize   = Math.max(size, 1);
        int total      = lista.size();
        int totalPaginas  = total == 0 ? 1 : (int) Math.ceil((double) total / safeSize);
        int paginaSegura  = Math.min(Math.max(page, 0), totalPaginas - 1);
        int desde         = paginaSegura * safeSize;
        int hasta         = Math.min(desde + safeSize, total);
        List<T> pagina    = desde < total ? lista.subList(desde, hasta) : Collections.emptyList();

        int pagInicio = Math.max(0, paginaSegura - 2);
        int pagFin    = Math.min(totalPaginas - 1, paginaSegura + 2);

        model.addAttribute("paginaActual",    paginaSegura);
        model.addAttribute("totalPaginas",    totalPaginas);
        model.addAttribute("totalElementos",  total);
        model.addAttribute("tamanioPagina",   safeSize);
        model.addAttribute("pagInicio",       pagInicio);
        model.addAttribute("pagFin",          pagFin);
        model.addAttribute("primerElemento",  total > 0 ? paginaSegura * safeSize + 1 : 0);
        model.addAttribute("ultimoElemento",  hasta);

        return pagina;
    }
}
