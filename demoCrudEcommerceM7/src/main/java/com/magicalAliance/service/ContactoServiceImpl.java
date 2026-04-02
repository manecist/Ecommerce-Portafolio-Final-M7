package com.magicalAliance.service;

import com.magicalAliance.entity.Contacto;
import com.magicalAliance.repository.ContactoRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactoServiceImpl implements IContactoService {

    @Autowired private ContactoRepository contactoRepository;
    @Autowired private IEmailService emailService;

    @Override
    @Transactional
    public void guardar(Contacto contacto) {
        contactoRepository.save(contacto);
        // Confirmación al remitente + notificación al admin (fallo silencioso)
        try { emailService.enviarNotificacionContacto(contacto); } catch (Exception ignored) {}
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contacto> listarTodos() {
        return contactoRepository.findAllByOrderByFechaEnvioDesc();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        contactoRepository.deleteById(id);
    }
}