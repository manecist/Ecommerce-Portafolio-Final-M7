/* =============================================================
   ECOMMERCE.JS — Magical Alliance
   Centraliza toda la lógica JavaScript del sitio.
   Cada sección usa guardas (if element) para ser segura
   en cualquier página donde se cargue el archivo.
   ============================================================= */

document.addEventListener("DOMContentLoaded", () => {

    /* =========================================================
       1. NAVEGACIÓN — Botón scroll arriba + carrito + alertas
       ========================================================= */
    const btnSubir = document.getElementById("btnSubir");
    if (btnSubir) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 200) {
                btnSubir.classList.add("btn-subir-visible");
            } else {
                btnSubir.classList.remove("btn-subir-visible");
            }
        });
        btnSubir.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    // El badge del carrito es renderizado por el servidor (Thymeleaf).
    // La función agregarAlCarrito lo actualiza vía AJAX tras cada adición.

    // Auto-cierre de alertas tras 5 segundos
    document.querySelectorAll('.alert').forEach(alerta => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alerta);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // (El reset del carrito lo maneja el servidor al cerrar sesión)

    /* =========================================================
       2. SUSCRIPCIÓN (Footer) — lee URL desde data-url del form
       ========================================================= */
    const formSuscripcion = document.getElementById('formSuscripcion');
    if (formSuscripcion) {
        const suscripcionUrl = formSuscripcion.dataset.url || '/subscribir';

        formSuscripcion.addEventListener('submit', function (e) {
            e.preventDefault();

            const emailInput = document.getElementById('emailSubscripcion');
            const feedback   = document.getElementById('feedbackEmail');
            const btn        = document.getElementById('btnSuscribir');
            const email      = emailInput.value.trim();

            if (!email || !emailInput.checkValidity()) {
                feedback.textContent = 'Por favor ingresa un correo válido.';
                feedback.style.setProperty('display', 'block', 'important');
                emailInput.classList.add('is-invalid');
                return;
            }

            feedback.style.setProperty('display', 'none', 'important');
            emailInput.classList.remove('is-invalid');
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Enviando...';

            const formData = new FormData();
            formData.append('email', email);

            fetch(suscripcionUrl, { method: 'POST', body: formData })
                .then(res => res.json())
                .then(data => {
                    const icono   = document.getElementById('modalSuscripcionIcono');
                    const mensaje = document.getElementById('modalSuscripcionMensaje');
                    if (data.estado === 'error') {
                        icono.className   = 'bi bi-exclamation-octagon display-3';
                        icono.style.color = '#ff4da6';
                    } else {
                        icono.className   = 'bi bi-envelope-check display-3';
                        icono.style.color = '#f480ff';
                        emailInput.value  = '';
                    }
                    mensaje.innerHTML = data.mensaje;
                    new bootstrap.Modal(document.getElementById('modalSuscripcion')).show();
                })
                .catch(() => {
                    const icono   = document.getElementById('modalSuscripcionIcono');
                    const mensaje = document.getElementById('modalSuscripcionMensaje');
                    icono.className   = 'bi bi-exclamation-octagon display-3';
                    icono.style.color = '#ff4da6';
                    mensaje.textContent = 'Ocurrió un error al procesar tu solicitud. Intenta de nuevo.';
                    new bootstrap.Modal(document.getElementById('modalSuscripcion')).show();
                })
                .finally(() => {
                    btn.disabled  = false;
                    btn.innerHTML = 'Unirse';
                });
        });
    }

    /* =========================================================
       3. FORMATEO DE RUT — Aplicado por ID específico
       ========================================================= */

    // Registro: formato sin puntos (12345678-9)
    const rutRegistro = document.getElementById('rutInput');
    if (rutRegistro) {
        rutRegistro.addEventListener('input', (e) => {
            e.target.value = formatearRut(e.target.value);
        });
    }

    // Admin usuario-form: formato con puntos (12.345.678-k)
    const rutAdmin = document.getElementById('rutAdmin');
    if (rutAdmin) {
        if (rutAdmin.value) rutAdmin.value = formatRUTCompleto(rutAdmin.value);
        rutAdmin.addEventListener('input', (e) => {
            e.target.value = formatRUTCompleto(e.target.value);
        });
    }

    // Detalle/perfil: inputs con clase .rut-input (con puntos)
    document.querySelectorAll('.rut-input').forEach(input => {
        if (input.value) input.value = formatRUTCompleto(input.value);
        input.addEventListener('input', function (e) {
            e.target.value = formatRUTCompleto(e.target.value);
        });
    });

    // Buscador de usuarios (con puntos, se limpia antes del submit)
    const busquedaRut = document.getElementById('busquedaRut');
    if (busquedaRut) {
        busquedaRut.addEventListener('input', (e) => {
            e.target.value = formatearRutVisual(e.target.value);
        });
        const formBusqueda = document.getElementById('formBusqueda');
        if (formBusqueda) {
            formBusqueda.addEventListener('submit', () => {
                busquedaRut.value = busquedaRut.value.replace(/[.\-]/g, '');
            });
        }
    }

    /* =========================================================
       4. VALIDACIÓN BOOTSTRAP GENÉRICA (.needs-validation)
          Se aplica a login.html y contacto.html
       ========================================================= */
    document.querySelectorAll('form.needs-validation').forEach(form => {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });

    /* =========================================================
       5. PÁGINA LOGIN
       ========================================================= */

    // Toggle mostrar/ocultar contraseña
    const btnToggleLogin = document.getElementById('btnToggle');
    if (btnToggleLogin) {
        btnToggleLogin.addEventListener('click', function () {
            const passInput = document.getElementById('pass');
            const eyeIcon   = document.getElementById('eyeIcon');
            if (!passInput) return;
            if (passInput.type === 'password') {
                passInput.type = 'text';
                if (eyeIcon) eyeIcon.classList.replace('bi-eye', 'bi-eye-slash');
            } else {
                passInput.type = 'password';
                if (eyeIcon) eyeIcon.classList.replace('bi-eye-slash', 'bi-eye');
            }
        });
    }

    /* =========================================================
       6. PÁGINA REGISTRO
       ========================================================= */
    const passRegistro = document.getElementById('passRegistro');
    if (passRegistro) {

        // Indicador de fuerza de contraseña
        passRegistro.addEventListener('input', function () {
            const strengthBar = document.getElementById('passStrength');
            if (!strengthBar) return;
            const val       = this.value;
            const hasUpper  = /[A-Z]/.test(val);
            const hasSymbol = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val);
            const isLong    = val.length >= 8;
            if (hasUpper && hasSymbol && isLong) {
                strengthBar.style.backgroundColor = "#00d4aa";
                strengthBar.style.width = "100%";
            } else if (val.length > 0) {
                strengthBar.style.backgroundColor = "#ff4da6";
                strengthBar.style.width = "40%";
            } else {
                strengthBar.style.width = "0%";
            }
        });

        // Toggle mostrar/ocultar contraseña en registro
        const btnToggleReg = document.getElementById('btnToggleReg');
        if (btnToggleReg) {
            btnToggleReg.addEventListener('click', function () {
                const icon   = document.getElementById('eyeIconReg');
                const isPass = passRegistro.type === 'password';
                passRegistro.type = isPass ? 'text' : 'password';
                if (icon) {
                    icon.classList.toggle('bi-eye', !isPass);
                    icon.classList.toggle('bi-eye-slash', isPass);
                }
            });
        }

        // Validación de edad en registro
        const fechaNacReg = document.getElementById('fechaNac');
        if (fechaNacReg) {
            fechaNacReg.addEventListener('change', function () {
                const badgeEdad    = document.getElementById('verEdad');
                const feedbackEdad = document.getElementById('feedbackEdad');
                const fechaDate    = new Date(this.value);
                const hoy          = new Date();

                if (isNaN(fechaDate.getTime())) return;

                let edad = hoy.getFullYear() - fechaDate.getFullYear();
                const mes = hoy.getMonth() - fechaDate.getMonth();
                if (mes < 0 || (mes === 0 && hoy.getDate() < fechaDate.getDate())) edad--;

                if (badgeEdad) {
                    badgeEdad.style.display     = 'inline-block';
                    badgeEdad.textContent       = edad + " años";
                    badgeEdad.style.backgroundColor = (edad < 18 || edad > 105) ? "#ff4da6" : "#00d4aa";
                }

                if (edad < 18 || edad > 105) {
                    this.setCustomValidity("Edad fuera de rango");
                    if (feedbackEdad) {
                        feedbackEdad.textContent = edad < 18
                            ? "Lo siento, debes ser mayor de 18 años."
                            : "La edad máxima es 105 años.";
                    }
                } else {
                    this.setCustomValidity("");
                }
            });
        }
    }

    /* =========================================================
       7. PÁGINA CONTACTO — Contador de caracteres
       ========================================================= */
    const textareaMensaje = document.getElementById('mensaje');
    const contadorMensaje = document.getElementById('contadorMensaje');
    if (textareaMensaje && contadorMensaje) {
        textareaMensaje.addEventListener('input', function () {
            const len = this.value.length;
            contadorMensaje.textContent = len + ' / 1000 caracteres';
            contadorMensaje.style.color = len > 900 ? '#ff4da6' : '';
        });
    }

    /* =========================================================
       8a. PRODUCTO FORM — aviso precio bajo en tiempo real
       ========================================================= */
    const inputPrecioProducto = document.getElementById('inputPrecioProducto');
    if (inputPrecioProducto) {
        inputPrecioProducto.addEventListener('input', actualizarAvisoPrecio);
        // Ejecutar al cargar por si viene con valor pre-llenado (modo edición)
        actualizarAvisoPrecio();
    }

    /* =========================================================
       8. VISTA PREVIA DE IMÁGENES
          Cubre producto-form.html y categorias-list.html
       ========================================================= */

    // Select galería con id="selectGaleria" (producto-form)
    const selectGaleria = document.getElementById('selectGaleria');
    if (selectGaleria) {
        selectGaleria.addEventListener('change', function () {
            const preview = document.getElementById('preview');
            if (preview && this.value) {
                preview.src = '/assets/img/' + this.value;
                const fileInput = document.getElementById('inputArchivo');
                if (fileInput) fileInput.value = '';
            }
        });
    }

    // Select galería sin id (categorias-list: name="imagenExistente")
    // Evitamos duplicar el listener si ya fue cubierto por selectGaleria
    document.querySelectorAll('select[name="imagenExistente"]').forEach(select => {
        if (select.id === 'selectGaleria') return; // ya tiene listener
        select.addEventListener('change', function () {
            const preview = document.getElementById('preview');
            if (preview && this.value) {
                preview.src = '/assets/img/' + this.value;
                const fileInput = document.querySelector('input[type="file"][name="archivoImagen"]');
                if (fileInput) fileInput.value = '';
            }
        });
    });

    // Input archivo con id="inputArchivo" (producto-form)
    const inputArchivo = document.getElementById('inputArchivo');
    if (inputArchivo) {
        inputArchivo.addEventListener('change', function (event) {
            if (!event.target.files[0]) return;
            const reader = new FileReader();
            reader.onload = () => {
                const preview = document.getElementById('preview');
                if (preview) preview.src = reader.result;
                if (selectGaleria) selectGaleria.value = '';
            };
            reader.readAsDataURL(event.target.files[0]);
        });
    }

    // Input archivo sin id (categorias-list: name="archivoImagen")
    document.querySelectorAll('input[type="file"][name="archivoImagen"]').forEach(input => {
        input.addEventListener('change', function (event) {
            if (!event.target.files[0]) return;
            const reader = new FileReader();
            reader.onload = () => {
                const preview = document.getElementById('preview');
                if (preview) preview.src = reader.result;
            };
            reader.readAsDataURL(event.target.files[0]);
        });
    });

    /* =========================================================
       9. LISTA DE USUARIOS (admin) — Borrado con event delegation
       ========================================================= */
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.btn-preparar-borrado');
        if (!btn) return;

        const id       = btn.getAttribute('data-id');
        const nombre   = btn.getAttribute('data-nombre');
        const nombreEl = document.getElementById('nombreUsuarioBorrar');
        const form     = document.getElementById('formBorrarAdmin');
        const modalEl  = document.getElementById('deleteUserModal');

        if (nombreEl) nombreEl.textContent = nombre;
        if (form)     form.action = '/usuarios/eliminar/' + id;
        if (modalEl)  new bootstrap.Modal(modalEl).show();
    });

    /* =========================================================
       10. FORMULARIO USUARIO ADMIN (usuario-form.html)
           Lee esNuevo desde data-es-nuevo en el form
       ========================================================= */
    const formUsuarioAdmin = document.getElementById('formUsuario');
    // Distinguimos de registro.html verificando #pwAdmin (solo existe en admin)
    const pwAdmin = document.getElementById('pwAdmin');

    if (formUsuarioAdmin && pwAdmin) {
        const esNuevo    = formUsuarioAdmin.dataset.esNuevo === 'true';
        const fecNacAdm  = document.getElementById('fecNacAdmin');
        const msgEdadAdm = document.getElementById('msgEdadAdmin');

        // Helper de edad para admin
        function mostrarEdadAdmin(fechaStr) {
            if (!fechaStr || !msgEdadAdm) return;
            const hoy    = new Date();
            const cumple = new Date(fechaStr);
            let edad = hoy.getFullYear() - cumple.getFullYear();
            if (hoy.getMonth() < cumple.getMonth() ||
               (hoy.getMonth() === cumple.getMonth() && hoy.getDate() < cumple.getDate())) edad--;
            if (edad < 18) {
                msgEdadAdm.innerHTML = `<span class="text-danger">❌ Menor de edad (${edad} años). Mínimo 18.</span>`;
            } else if (edad > 105) {
                msgEdadAdm.innerHTML = `<span class="text-danger">❌ Edad no válida (${edad} años). Máximo 105.</span>`;
            } else {
                msgEdadAdm.innerHTML = `<span class="text-success">✅ Edad válida: ${edad} años</span>`;
            }
        }

        if (fecNacAdm) {
            fecNacAdm.addEventListener('change', function () { mostrarEdadAdmin(this.value); });
            if (fecNacAdm.value) mostrarEdadAdmin(fecNacAdm.value);
        }

        // Toggle password admin
        const btnTogglePw = document.getElementById('btnTogglePw');
        if (btnTogglePw) {
            btnTogglePw.addEventListener('click', function () {
                const p = pwAdmin;
                const i = this.querySelector('i');
                p.type = p.type === 'password' ? 'text' : 'password';
                if (i) {
                    i.classList.toggle('bi-eye');
                    i.classList.toggle('bi-eye-slash');
                }
            });
        }

        // Helpers de validación inline
        function setError(inputEl, errDivId, msg) {
            if (inputEl) inputEl.classList.add('is-invalid');
            const errDiv = document.getElementById(errDivId);
            if (errDiv) errDiv.textContent = msg;
        }
        function clearError(inputEl, errDivId) {
            if (inputEl) inputEl.classList.remove('is-invalid');
            const errDiv = document.getElementById(errDivId);
            if (errDiv) errDiv.textContent = '';
        }

        // Validación al enviar
        formUsuarioAdmin.addEventListener('submit', function (e) {
            let hayError = false;

            const camposRequeridos = [
                ['nom',         'err-nombre',      'El nombre es obligatorio.'],
                ['ape',         'err-apellido',     'El apellido es obligatorio.'],
                ['rutAdmin',    'err-rut',          'El RUT es obligatorio.'],
                ['em',          'err-email',        'El email es obligatorio.'],
                ['dirAdmin',    'err-direccion',    'La dirección es obligatoria.'],
                ['ciudadAdmin', 'err-ciudad',       'La ciudad es obligatoria.'],
                ['regionAdmin', 'err-estadoRegion', 'La región es obligatoria.'],
                ['paisAdmin',   'err-pais',         'El país es obligatorio.'],
            ];

            camposRequeridos.forEach(([inputId, errId, msg]) => {
                const input = document.getElementById(inputId);
                if (!input) return;
                if (!input.value.trim()) { setError(input, errId, msg); hayError = true; }
                else clearError(input, errId);
            });

            const emailInput = document.getElementById('em');
            if (emailInput && emailInput.value.trim() &&
                !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.value.trim())) {
                setError(emailInput, 'err-email', 'El formato del email no es válido.');
                hayError = true;
            }

            if (fecNacAdm) {
                if (!fecNacAdm.value) {
                    fecNacAdm.classList.add('is-invalid');
                    if (msgEdadAdm) msgEdadAdm.innerHTML =
                        '<span class="text-danger">La fecha de nacimiento es obligatoria.</span>';
                    hayError = true;
                } else {
                    fecNacAdm.classList.remove('is-invalid');
                    const hoy    = new Date();
                    const cumple = new Date(fecNacAdm.value);
                    let edad = hoy.getFullYear() - cumple.getFullYear();
                    if (hoy.getMonth() < cumple.getMonth() ||
                       (hoy.getMonth() === cumple.getMonth() && hoy.getDate() < cumple.getDate())) edad--;
                    if (edad < 18 || edad > 105) {
                        fecNacAdm.classList.add('is-invalid');
                        mostrarEdadAdmin(fecNacAdm.value);
                        hayError = true;
                    }
                }
            }

            if (esNuevo && !pwAdmin.value.trim()) {
                setError(pwAdmin, 'err-password', 'La contraseña es obligatoria al crear un usuario.');
                hayError = true;
            } else {
                clearError(pwAdmin, 'err-password');
            }

            const rolSelect = document.getElementById('selectRol');
            if (rolSelect && !rolSelect.value) {
                setError(rolSelect, 'err-rol', 'Debes asignar un rango al usuario.');
                hayError = true;
            } else if (rolSelect) {
                clearError(rolSelect, 'err-rol');
            }

            if (hayError) {
                e.preventDefault();
                const primerError = document.querySelector('.is-invalid');
                if (primerError) primerError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return;
            }

            // Limpiar puntos del RUT antes de enviar
            const rutAdminInput = document.getElementById('rutAdmin');
            if (rutAdminInput) rutAdminInput.value = rutAdminInput.value.replace(/[.\-]/g, '');
        });
    }

    /* =========================================================
       11. PERFIL / DETALLE CLIENTE — ya cubierto por .rut-input
           (aplicarFormatoRUT ya se maneja en sección 3)
       ========================================================= */

}); // ─── Fin DOMContentLoaded ───────────────────────────────────


/* =============================================================
   FUNCIONES GLOBALES
   Llamadas directamente desde atributos onclick en el HTML
   ============================================================= */

/* --- Agregar al carrito (POST al servidor, actualiza badge vía AJAX) --- */
function agregarAlCarrito(productoId, nombre, cantidad) {
    const qty = cantidad || 1;
    fetch('/carrito/agregar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'productoId=' + productoId + '&cantidad=' + qty
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const numerito = document.getElementById('numerito');
            if (numerito && data.totalItems !== undefined) {
                numerito.innerText = data.totalItems;
            }
            const toastEl = document.getElementById('toastCarrito');
            if (toastEl) {
                const toast   = new bootstrap.Toast(toastEl);
                const mensaje = document.getElementById('mensajeToast');
                if (mensaje) mensaje.innerHTML =
                    `<b>✨ ¡Hechizo realizado!</b><br>${nombre}${qty > 1 ? ' (x' + qty + ')' : ''} añadido al caldero.`;
                toast.show();
            }
        } else {
            alert('⚠️ ' + (data.mensaje || 'No se pudo agregar al carrito'));
        }
    })
    .catch(() => alert('⚠️ Error de conexión al agregar el producto'));
}

/* --- Agregar al carrito desde tarjeta con selector de cantidad --- */
function agregarAlCarritoConCantidad(btn) {
    const id     = btn.getAttribute('data-id');
    const nombre = btn.getAttribute('data-nombre');
    const qtyInput = btn.closest('.card-body').querySelector('.qty-input');
    const qty = qtyInput ? parseInt(qtyInput.value) || 1 : 1;
    agregarAlCarrito(id, nombre, qty);
}

/* --- Cambiar cantidad en selector de tarjeta de producto --- */
function cambiarCantidad(btn, delta, maxStock) {
    const qtyInput = btn.closest('.qty-selector-magic').querySelector('.qty-input');
    if (!qtyInput) return;
    let val = parseInt(qtyInput.value) + delta;
    if (val < 1) val = 1;
    if (val > maxStock) val = maxStock;
    qtyInput.value = val;
}

/* --- Cambiar cantidad en modal de detalle --- */
function cambiarCantidadDetalle(delta) {
    const qtyInput = document.getElementById('qtyDetalleInput');
    if (!qtyInput) return;
    let val = parseInt(qtyInput.value) + delta;
    const max = parseInt(qtyInput.max) || 99;
    if (val < 1) val = 1;
    if (val > max) val = max;
    qtyInput.value = val;
}

/* --- Abrir modal de detalle de producto --- */
function abrirDetalleProducto(img) {
    const id      = img.getAttribute('data-id');
    const nombre  = img.getAttribute('data-nombre');
    const desc    = img.getAttribute('data-desc') || 'Sin descripción disponible.';
    const precio  = parseFloat(img.getAttribute('data-precio')) || 0;
    const stock   = parseInt(img.getAttribute('data-stock')) || 0;
    const imgSrc  = img.getAttribute('src');
    const subcat  = img.getAttribute('data-subcat') || '';
    const editUrl = img.getAttribute('data-edit-url') || '#';

    // Detectar si es admin
    const esAdminEl  = document.getElementById('esAdmin');
    const esAdmin    = esAdminEl && esAdminEl.getAttribute('data-admin') === 'true';

    // Populate modal
    document.getElementById('tituloDetalleProducto').innerHTML =
        '<i class="bi bi-stars me-2"></i>' + nombre;
    document.getElementById('nombreDetalleProducto').textContent = nombre;
    document.getElementById('descDetalleProducto').textContent  = desc;
    document.getElementById('subcatDetalleProducto').textContent = subcat;
    document.getElementById('imgDetalleProducto').src           = imgSrc;

    // Precio CLP
    const precioFormateado = '$ ' + Math.round(precio).toLocaleString('es-CL');
    document.getElementById('precioDetalleProducto').textContent = precioFormateado;

    // Stock badge
    const stockBadge = document.getElementById('stockDetalleProducto');
    if (stock > 0) {
        stockBadge.textContent = '✨ Stock: ' + stock;
        stockBadge.className = 'stock-badge-detalle';
        stockBadge.style.cssText = 'background:#d1f7ee; color:#0a7a5f; border:1px solid #00d4aa;';
    } else {
        stockBadge.textContent = 'Sin stock';
        stockBadge.className = 'stock-badge-detalle';
        stockBadge.style.cssText = 'background:#f8d7da; color:#842029;';
    }

    // Controles de cantidad y botón
    const controlesStock   = document.getElementById('controles-detalle-stock');
    const controlesAgotado = document.getElementById('controles-detalle-agotado');
    const qtyDetalle       = document.getElementById('qtyDetalleInput');
    const btnAgregar       = document.getElementById('btnAgregarDetalle');

    if (stock > 0) {
        controlesStock.style.display   = '';
        controlesAgotado.style.display = 'none';
        if (qtyDetalle) { qtyDetalle.value = 1; qtyDetalle.max = stock; }
        if (btnAgregar) {
            btnAgregar.setAttribute('data-id', id);
            btnAgregar.setAttribute('data-nombre', nombre);
        }
    } else {
        controlesStock.style.display   = 'none';
        controlesAgotado.style.display = '';
    }

    // Bloque admin: mostrar link editar solo si es admin
    const bloqueAdmin = document.getElementById('bloque-admin-detalle');
    const linkEditar  = document.getElementById('linkEditarDetalle');
    if (bloqueAdmin && linkEditar) {
        if (esAdmin) {
            bloqueAdmin.style.display = '';
            linkEditar.href = editUrl;
        } else {
            bloqueAdmin.style.display = 'none';
        }
    }

    new bootstrap.Modal(document.getElementById('modalDetalleProducto')).show();
}

/* --- Agregar desde modal de detalle --- */
function agregarDesdeDetalle() {
    const btn    = document.getElementById('btnAgregarDetalle');
    const id     = btn.getAttribute('data-id');
    const nombre = btn.getAttribute('data-nombre');
    const qty    = parseInt(document.getElementById('qtyDetalleInput').value) || 1;
    agregarAlCarrito(id, nombre, qty);
}

/* --- Confirmar eliminar PRODUCTO --- */
function confirmarEliminar(id, nombre) {
    const nombreEl = document.getElementById('nombreProdEliminar');
    if (nombreEl) nombreEl.innerText = nombre;
    const form = document.getElementById('formEliminarProd');
    if (form) form.action = '/productos/admin/eliminar/' + id;
    const modalEl = document.getElementById('modalEliminar');
    if (modalEl) new bootstrap.Modal(modalEl).show();
}

/* --- Preparar borrado de CATEGORÍA --- */
function prepararBorradoCat(id, nombre) {
    const nombreEl = document.getElementById('nombreCatBorrar');
    if (nombreEl) nombreEl.innerText = nombre;
    const form = document.getElementById('formEliminarCat');
    if (form) form.action = '/admin/categorias/eliminar/' + id;
    // El modal lo abre Bootstrap via data-bs-toggle en el botón
}

/* --- Preparar borrado de SUBCATEGORÍA --- */
function prepararBorradoSub(id, nombre) {
    const nombreEl = document.getElementById('nombreSubBorrar');
    if (nombreEl) nombreEl.innerText = nombre;
    const form = document.getElementById('formEliminarSub');
    if (form) form.action = '/admin/subcategorias/eliminar/' + id;
    // El modal lo abre Bootstrap via data-bs-toggle en el botón
}

/* --- Validar y confirmar formulario de PRODUCTO (producto-form) --- */
function validarYConfirmar() {
    const form = document.getElementById('formProducto');
    if (!form) return;

    // Actualizar aviso de precio bajo en tiempo real
    actualizarAvisoPrecio();

    if (form.checkValidity()) {
        const precioInput = document.getElementById('inputPrecioProducto');
        const precio = precioInput ? parseFloat(precioInput.value) : -1;

        // Si precio es 0 o entre 0 y 4999 — mostrar advertencia primero
        if (precio >= 0 && precio < 5000) {
            const textoPrecio = document.getElementById('textoPrecioAviso');
            if (textoPrecio) {
                textoPrecio.textContent = '$ ' + Math.round(precio).toLocaleString('es-CL') + ' CLP';
            }
            const modalWarn = document.getElementById('modalPrecioBajo');
            if (modalWarn) { new bootstrap.Modal(modalWarn).show(); return; }
        }

        // Precio normal: confirmar directamente
        const modalEl = document.getElementById('modalConfirmarProducto');
        if (modalEl) new bootstrap.Modal(modalEl).show();
    } else {
        form.classList.add('was-validated');
        form.reportValidity();
        const firstError = form.querySelector(':invalid');
        if (firstError) firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

/* --- Confirmar precio bajo y abrir modal de confirmación --- */
function confirmarPrecioYGuardar() {
    const modalWarn = bootstrap.Modal.getInstance(document.getElementById('modalPrecioBajo'));
    if (modalWarn) modalWarn.hide();
    document.getElementById('modalPrecioBajo').addEventListener('hidden.bs.modal', function handler() {
        this.removeEventListener('hidden.bs.modal', handler);
        const modalEl = document.getElementById('modalConfirmarProducto');
        if (modalEl) new bootstrap.Modal(modalEl).show();
    });
}

/* --- Actualizar aviso de precio bajo en tiempo real --- */
function actualizarAvisoPrecio() {
    const precioInput = document.getElementById('inputPrecioProducto');
    const aviso       = document.getElementById('avisoPrecionBajo');
    if (!precioInput || !aviso) return;
    const precio = parseFloat(precioInput.value);
    aviso.style.display = (!isNaN(precio) && precio >= 0 && precio < 5000) ? '' : 'none';
}

/* --- Enviar formulario de PRODUCTO desde modal de confirmación --- */
function enviarFormulario() {
    const form = document.getElementById('formProducto');
    if (form) form.submit();
}

/* --- Cerrar modal de error de login manualmente --- */
function cerrarModalError() {
    const modal = document.getElementById('errorLoginModal');
    if (modal) modal.style.display = 'none';
    const backdrop = document.querySelector('.modal-backdrop');
    if (backdrop) backdrop.remove();
}

/* --- Confirmar eliminación de MORADA (perfil cliente y admin) --- */
function abrirConfirmarEliminarDir(btn) {
    const formEl = document.getElementById('formEliminarDir');
    if (formEl) formEl.action = btn.getAttribute('data-action');
    const modalEl = document.getElementById('confirmDeleteModal');
    if (modalEl) new bootstrap.Modal(modalEl).show();
}

/* --- Modal EDITAR DIRECCIÓN (admin usuarios-list) --- */
function abrirModalEditarDir(boton) {
    const uId          = boton.getAttribute('data-id-usuario');
    const dId          = boton.getAttribute('data-id-dir');
    const calle        = boton.getAttribute('data-calle');
    const ciudad       = boton.getAttribute('data-ciudad');
    const region       = boton.getAttribute('data-region');
    const pais         = boton.getAttribute('data-pais');
    const codigoPostal = boton.getAttribute('data-codigoPostal');

    const form = document.getElementById('formEditarDireccion');
    if (form) form.action = `/usuarios/direcciones/editarAdmin/${uId}/${dId}`;

    const setVal = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.value = val || '';
    };
    setVal('editDireccion',   calle);
    setVal('editCiudad',      ciudad);
    setVal('editRegion',      region);
    setVal('editPais',        pais);
    setVal('editCodigoPostal', codigoPostal);

    const modalEl = document.getElementById('modalDireccion');
    if (modalEl) new bootstrap.Modal(modalEl).show();
}

/* --- Modal NUEVA DIRECCIÓN (admin usuarios-list) --- */
function abrirModalNuevaDir(boton) {
    const uId  = boton.getAttribute('data-id-usuario');
    const form = document.getElementById('formNuevaDireccion');
    if (form) {
        form.action = `/usuarios/direcciones/agregarAdmin/${uId}`;
        form.reset();
    }
    const modalEl = document.getElementById('modalNuevaDir');
    if (modalEl) new bootstrap.Modal(modalEl).show();
}

/* --- Validar y abrir modal de confirmación de REGISTRO --- */
function validarFormularioYAbrirModal() {
    const form      = document.getElementById('formUsuario');
    const passInput = document.getElementById('passRegistro');
    if (!form || !passInput) return;

    const passRegex = /^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
    if (!passRegex.test(passInput.value)) {
        passInput.setCustomValidity("Invalido");
    } else {
        passInput.setCustomValidity("");
    }

    if (form.checkValidity()) {
        const modalEl = document.getElementById('confirmModal');
        if (modalEl) new bootstrap.Modal(modalEl).show();
    } else {
        form.classList.add('was-validated');
        const primerError = form.querySelector(':invalid');
        if (primerError) primerError.focus();
    }
}

/* --- Enviar formulario final de REGISTRO (desde modal) --- */
function enviarFormularioFinal() {
    const rutInput = document.getElementById('rutInput');
    if (rutInput) rutInput.value = rutInput.value.replace(/\./g, '');
    const form = document.getElementById('formUsuario');
    if (form) form.submit();
}

/* =============================================================
   UTILIDADES DE FORMATO RUT
   ============================================================= */

/* Sin puntos, con guión: 12345678-9 (registro) */
function formatearRut(valor) {
    if (!valor) return valor;
    valor = valor.replace(/[^0-9kK]/g, '');
    if (valor.length > 1) {
        const cuerpo = valor.slice(0, -1);
        const dv     = valor.slice(-1).toUpperCase();
        return cuerpo + '-' + dv;
    }
    return valor.toUpperCase();
}

/* Con puntos y guión: 12.345.678-k (admin form, perfil) */
function formatRUTCompleto(rut) {
    if (!rut) return "";
    let value = rut.toString().replace(/[^0-9kK]/g, '');
    if (value.length < 2) return value.toUpperCase();
    const cuerpo = value.slice(0, -1);
    const dv     = value.slice(-1).toUpperCase();
    const cuerpoFormateado = cuerpo
        .split('').reverse().join('')
        .replace(/(\d{3})(?=\d)/g, '$1.')
        .split('').reverse().join('');
    return cuerpoFormateado + '-' + dv;
}

/* Con puntos y guión: para buscador visual */
function formatearRutVisual(valor) {
    if (!valor) return "";
    valor = valor.replace(/[^0-9kK]/g, '');
    if (valor.length <= 1) return valor.toUpperCase();
    const cuerpo = valor.slice(0, -1).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    const dv     = valor.slice(-1).toUpperCase();
    return cuerpo + '-' + dv;
}