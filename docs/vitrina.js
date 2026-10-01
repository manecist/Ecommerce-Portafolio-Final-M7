/* =====================================================================
   Magical Alliance · Vitrina interactiva (vanilla JS, sin build)
   Re-crea en el navegador los flujos del proyecto Spring Boot:
   catálogo → caldero → checkout → pedido → historial → panel admin.
   Reglas de negocio portadas desde:
     - ProductoServiceImpl.listar()          (filtros, orden, ocultar sin stock)
     - PaginacionHelper.paginar()            (paginación in-memory, ventana ±2)
     - CarritoServiceImpl                    (stock, cantidades, recálculo de cupón)
     - DescuentoServiceImpl / Descuento / Cupon (mejor descuento, validación de cupón)
     - PedidoServiceImpl                     (IVA 19%, snapshot, stock, cancelar, devolución)
     - UsuarioServiceImpl.registrarUsuario() (edad 18–105, contraseña, RUT limpio)
   Nada se envía a ningún servidor: el estado vive en localStorage.
   ===================================================================== */
(function () {
    'use strict';

    // ------------------------------------------------------------------
    // 0. Utilidades
    // ------------------------------------------------------------------
    const STORAGE_KEY = 'magicalAlliance.vitrina.v1';
    const TASA_IVA = 0.19;                // PedidoServiceImpl.TASA_IVA
    const TAM_PAGINA = 12;                // ProductoController: @RequestParam(defaultValue = "12") size
    const STOCK_BAJO = 3;
    const REPO = 'https://github.com/manecist/Ecommerce-Portafolio-Final-M7/blob/main/demoCrudEcommerceM7/src/main/';
    const ESTADOS = ['PENDIENTE', 'CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'CANCELADO', 'DEVOLUCION_SOLICITADA', 'DEVOLUCION_REALIZADA'];
    const FLUJO_ESTADOS = ['PENDIENTE', 'CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO'];

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
    const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const clp = (n) => '$ ' + Math.round(Number(n) || 0).toLocaleString('es-CL');
    const hoyISO = () => new Date().toISOString().slice(0, 10);
    const sumarDias = (dias) => { const d = new Date(); d.setDate(d.getDate() + dias); return d.toISOString().slice(0, 10); };
    const fechaCorta = (iso) => new Date(iso).toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const fechaLarga = (iso) => new Date(iso).toLocaleString('es-CL', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    const estadoLabel = (e) => e.replace(/_/g, ' ');
    const img = (f) => 'img/' + f;

    class MagicalBusinessException extends Error {}

    // ------------------------------------------------------------------
    // 1. Datos semilla (categorías y subcategorías reales + catálogo demo)
    // ------------------------------------------------------------------
    const CATEGORIAS = [
        { id: 1, nombre: 'ACCESORIOS', banner: 'banner-accesorios.webp' },
        { id: 2, nombre: 'COSMETICOS', banner: 'banner-cosmeticos.webp' },
        { id: 3, nombre: 'VESTUARIO', banner: 'banner-vestuario.webp' }
    ];
    const SUBCATEGORIAS = [
        { id: 1, nombre: 'Anillos', cat: 1 }, { id: 2, nombre: 'Aros y dijes', cat: 1 },
        { id: 3, nombre: 'Relojes', cat: 1 }, { id: 4, nombre: 'Carteras y mochilas', cat: 1 },
        { id: 5, nombre: 'Perfumes', cat: 1 }, { id: 6, nombre: 'Cosmetiqueros', cat: 1 },
        { id: 7, nombre: 'Ojos', cat: 2 }, { id: 8, nombre: 'Rostro', cat: 2 },
        { id: 9, nombre: 'Labios', cat: 2 }, { id: 10, nombre: 'Herramientas', cat: 2 },
        { id: 11, nombre: 'Calzado', cat: 3 }, { id: 12, nombre: 'Matrimonio', cat: 3 },
        { id: 13, nombre: 'Vestidos', cat: 3 }, { id: 14, nombre: 'Abrigos y chaquetas', cat: 3 }
    ];
    // [id, nombre, descripción, precio, stock, imagen, subcategoría]
    const PRODUCTOS_SEMILLA = [
        [1, 'Reloj Sakura Timepiece', 'Reloj de cuarzo con esfera de nácar rosado, correa de cuero sintético y dije de báculo estrella. Viene en estuche de regalo.', 89990, 6, 'p-reloj-sakura.webp', 3],
        [2, 'Reloj Sailor Mercury', 'Esfera turquesa con brillo de cristal, correa azul marino y colgante de lazo. Resistente al agua 3 ATM.', 94990, 4, 'p-reloj-mercury.webp', 3],
        [3, 'Reloj Magical Alliance Noche', 'Edición de la casa: esfera azul noche, caja plateada y estuche lila con el sello de la Alianza.', 119990, 2, 'p-reloj-alliance.webp', 3],
        [4, 'Mochila Alas de Sakura', 'Mochila de cuero vegano con alas desmontables y broche de corazón alado. Bolsillo interior acolchado.', 64990, 5, 'p-mochila-alas.webp', 4],
        [5, 'Mochila Cristal Celeste', 'Celeste pastel con lazo crema, herrajes dorados y broches de las guardianas. Capacidad 12 L.', 59990, 7, 'p-mochila-cristal.webp', 4],
        [6, 'Mochila Lazo Carmesí', 'Mochila crema con gran lazo rojo y bordado dorado estilo uniforme escolar mágico.', 57990, 0, 'p-mochila-lazo.webp', 4],
        [7, 'Perfume Corona Rosa', 'Eau de toilette floral afrutado en frasco de cristal con tapa de corona dorada. 50 ml.', 42990, 9, 'p-perfume-corona.webp', 5],
        [8, 'Trío de Perfumes Guardianas', 'Set de tres fragancias: pétalo de cerezo, brisa marina y almizcle lunar. 3 × 30 ml.', 79990, 3, 'p-perfume-trio.webp', 5],
        [9, 'Perfume Luna Creciente', 'Notas de vainilla, peonía y sándalo. Frasco con tapa de luna y corazón alado. 45 ml.', 38990, 8, 'p-perfume-luna.webp', 5],
        [10, 'Cosmetiquero Flor de Cerezo', 'Maletín rígido con espejo, bandejas extensibles y estampado de sakura. Incluye set de brochas.', 49990, 4, 'p-cosmetiquero-sakura.webp', 6],
        [11, 'Maletín Sailor Sisterhood', 'Organizador profesional con franjas pastel, lazo de satín y compartimentos de terciopelo.', 69990, 2, 'p-maletin-sailor.webp', 6],
        [12, 'Anillo Corazón de Cristal', 'Plata 925 con circonia rosada en corte corazón y alas grabadas. Tallas 6 a 9.', 129990, 3, 'p-anillo-corazon.webp', 1],
        [13, 'Anillo Sol Dorado', 'Baño de oro rosa con piedra central y detalles de luna y estrella. Ajustable.', 74990, 6, 'p-anillo-sol.webp', 1],
        [14, 'Delineador Estelar', 'Delineador líquido de punta fina, negro intenso y larga duración. A prueba de agua.', 12990, 25, 'p-delineador-estelar.webp', 7],
        [15, 'Máscara de Pestañas Volumen Lunar', 'Tres cepillos intercambiables para alargar, curvar y dar volumen sin grumos.', 14990, 18, 'p-mascara-pestanas.webp', 7],
        [16, 'Sombras en Gel Galaxia', 'Cuatro pigmentos en gel con micro brillo: rosa, celeste, lila y rubí.', 19990, 10, 'p-sombras-gel.webp', 7],
        [17, 'Iluminador Perla Lunar', 'Iluminador horneado en espiral con acabado perlado para pómulos y lagrimal.', 18990, 12, 'p-iluminador-lunar.webp', 8],
        [18, 'Polvo Compacto Cristal', 'Polvo traslúcido sellador con borla y espejo. Controla brillo hasta 12 horas.', 21990, 9, 'p-polvo-compacto.webp', 8],
        [19, 'Sérum Elixir de Cristal', 'Sérum hidratante con ácido hialurónico y extracto de flor de cerezo. 30 ml.', 27990, 7, 'p-serum-cristal.webp', 8],
        [20, 'Primer Velo Mágico', 'Prebase en spray que fija el maquillaje y suaviza poros. 100 ml.', 16990, 1, 'p-primer-magico.webp', 8],
        [21, 'Gloss Alas de Ángel', 'Brillo labial con partículas doradas y aplicador de tapa alada.', 9990, 30, 'p-gloss-estelar.webp', 9],
        [22, 'Set Labiales Satín Real', 'Cinco labiales de acabado satinado en tonos nude, coral, rojo, frambuesa y vino.', 34990, 6, 'p-labiales-satin.webp', 9],
        [23, 'Cepillo Encantado', 'Cepillo desenredante con cerdas flexibles y mango de corazón alado.', 15990, 11, 'p-cepillo-encantado.webp', 10],
        [24, 'Set de Brochas Sakura', 'Siete brochas de fibra sintética suave con estuche cilíndrico rosado.', 29990, 8, 'p-brochas-sakura.webp', 10],
        [25, 'Espejo Luna de Tocador', 'Espejo giratorio de mesa con marco dorado de luna creciente.', 39990, 5, 'p-espejo-luna.webp', 10],
        [26, 'Secador Cristal Rosa', 'Secador iónico de 1800 W con boquilla concentradora y diseño de broche mágico.', 54990, 0, 'p-secador-cristal.webp', 10],
        [27, 'Vestido Lila Constelación', 'Vestido corte A con corsé, falda de dos capas y estampado de lunas. Tallas S a XL.', 79990, 4, 'p-vestido-lila.webp', 13],
        [28, 'Vestido Aurora Bicolor', 'Rosa y celeste en mitades, con corsé estructurado y falda con vuelo. Tallas S a XL.', 79990, 3, 'p-vestido-aurora.webp', 13],
        [29, 'Vestido Sakura Card Captor', 'Vestido con lazo rojo, enagua de tul y accesorios a juego para cosplay. Tallas S a L.', 89990, 2, 'p-vestido-sakura.webp', 13],
        [30, 'Abrigo Celeste Mercury', 'Abrigo largo de paño con botones dorados y cinturón. Forro interior satinado.', 99990, 5, 'p-abrigo-celeste.webp', 14],
        [31, 'Abrigo Rosa Chibi', 'Abrigo cruzado rosa pastel con cinturón de broche lunar. Tallas S a XL.', 99990, 4, 'p-abrigo-rosa.webp', 14],
        [32, 'Blazer Dual Luna y Sol', 'Blazer de sastrería bicolor rosa y celeste con solapas bordadas.', 84990, 6, 'p-blazer-dual.webp', 14],
        [33, 'Zapatos Sakura Carmesí', 'Taco de 7 cm, charol burdeos con correa y lazo. Talla 35 a 40.', 88000, 5, 'p-zapatos-sakura.webp', 11],
        [34, 'Sandalias Pastel Alliance', 'Sandalias de taco fino bicolor con tira al tobillo. Talla 35 a 39.', 72000, 7, 'p-zapatos-alliance.webp', 11],
        [35, 'Traje Caballero de la Luna', 'Traje de novio azul medianoche con bordado de constelaciones y pañuelo de gala.', 289000, 2, 'p-traje-caballero.webp', 12],
        [36, 'Vestido Serenity Degradé', 'Vestido de novia con corsé de perlas y falda degradé rosa con encaje.', 310000, 1, 'p-vestido-serenity.webp', 12],
        // ---- Productos visibles en las capturas de la app real ----
        [37, 'Aros Sailor', 'Aros colgantes con lazo rojo, estrella y gota de cristal fucsia. Cierre de clip.', 12000, 15, 'p-aros-sailor.webp', 2],
        [38, 'Anillo Magic', 'Anillo de plata y oro rosa con flores talladas y gema violeta en corte corazón.', 1800000, 1, 'p-anillo-magic.webp', 1],
        [39, 'Sombras Magical', 'Sombras multicolor efecto matte y brillos, incluye un iluminador central', 23500, 14, 'p-sombras-magical.webp', 7],
        [40, 'Bases liquidas', 'Bases de diferentes colores, hidratantes y reafirmantes, el blanco elimina lineas de expresion y porosidad de la piel pero no pigmenta, su gama de colores va del más claro al más profundo.', 30000, 20, 'p-bases-liquidas.webp', 8],
        [41, 'Zapatos Sailor Moon', 'Zapatos con 9 cm de altura, con plata de goma y plantilla arch-fit para mejor comodidad. Inspirados en Sailor Cosmos. Talla 38, 39, 42', 92000, 6, 'p-zapatos-sailor-moon.webp', 11],
        [42, 'Vestido Matrimonio Magical', 'Vestido inspirado en Disney con corte princesa, falso con barillas, multiples capas de tela y encaje con colores, ademas de un faldon que se puede extraer. Talla Ajustable s-m, m-L, L-xL', 345000, 3, 'p-vestido-matrimonio-magical.webp', 12]
    ];

    const USUARIO_DEMO = { nombre: 'Usuario', apellido: 'Pruebas', email: 'usuario@magical.cl', rut: '222222222', telefono: '+56911111111', calle: 'Av. Los Andes 1234', ciudad: 'Santiago', estadoRegion: 'Región Metropolitana', pais: 'Chile', codigoPostal: '8320000' };
    const ADMIN_DEMO = { nombre: 'Administrador', apellido: 'Principal', email: 'admin@magical.cl', rut: '111111111', telefono: '+56900000000', calle: 'Pasaje Estelar 77', ciudad: 'Valparaíso', estadoRegion: 'Valparaíso', pais: 'Chile', codigoPostal: '2340000' };

    function semilla() {
        const productos = PRODUCTOS_SEMILLA.map(([id, nombre, descripcion, precio, stock, imagen, sub]) => ({ id, nombre, descripcion, precio, stock, imagen, sub }));
        const descuentos = [
            { id: 1, nombre: 'Temporada Sakura', tipo: 'PORCENTAJE', valor: 15, alcance: 'CATEGORIA', objetivo: 2, inicio: sumarDias(-10), fin: sumarDias(60), activo: true },
            { id: 2, nombre: 'Oferta Relojes Estelares', tipo: 'MONTO_FIJO', valor: 10000, alcance: 'SUBCATEGORIA', objetivo: 3, inicio: sumarDias(-5), fin: sumarDias(45), activo: true },
            { id: 3, nombre: 'Lanzamiento Vestido Serenity', tipo: 'PORCENTAJE', valor: 10, alcance: 'PRODUCTO', objetivo: 36, inicio: sumarDias(-2), fin: sumarDias(30), activo: true },
            { id: 4, nombre: 'Cyber Alianza', tipo: 'PORCENTAJE', valor: 20, alcance: 'GLOBAL', objetivo: null, inicio: sumarDias(20), fin: sumarDias(23), activo: true }
        ];
        const cupones = [
            { id: 1, codigo: 'MAGIC20', nombre: 'Descuento de bienvenida', tipo: 'PORCENTAJE', valor: 20, montoMinimo: 50000, limiteUsos: 100, usosActuales: 12, fechaExpiracion: sumarDias(90), activo: true },
            { id: 2, codigo: 'LUNA5000', nombre: 'Regalo de luna llena', tipo: 'MONTO_FIJO', valor: 5000, montoMinimo: 0, limiteUsos: null, usosActuales: 41, fechaExpiracion: null, activo: true },
            { id: 3, codigo: 'SAILOR50', nombre: 'Flash Sailor (agotado)', tipo: 'PORCENTAJE', valor: 50, montoMinimo: 0, limiteUsos: 3, usosActuales: 3, fechaExpiracion: sumarDias(30), activo: true },
            { id: 4, codigo: 'SAKURA2025', nombre: 'Campaña Sakura 2025', tipo: 'PORCENTAJE', valor: 25, montoMinimo: 0, limiteUsos: null, usosActuales: 88, fechaExpiracion: '2025-12-31', activo: true },
            { id: 5, codigo: 'ESTRELLA', nombre: 'Cupón en pausa', tipo: 'MONTO_FIJO', valor: 8000, montoMinimo: 30000, limiteUsos: null, usosActuales: 0, fechaExpiracion: null, activo: false }
        ];
        const snap = (pid, cantidad, precioUnitario, precioOriginal) => {
            const p = productos.find((x) => x.id === pid);
            return { productoId: pid, nombreProducto: p.nombre, imagenProducto: p.imagen, cantidad, precioUnitario, precioOriginal: precioOriginal ?? null };
        };
        const armarPedido = (id, dias, datos, items, estado, cupon) => {
            const neto = items.reduce((a, i) => a + i.precioUnitario * i.cantidad, 0);
            const ahorro = items.reduce((a, i) => a + (i.precioOriginal ? (i.precioOriginal - i.precioUnitario) * i.cantidad : 0), 0);
            const descCupon = cupon ? cupon.monto : 0;
            const subtotal = Math.max(0, neto - descCupon);
            const iva = subtotal * TASA_IVA;
            const f = new Date(); f.setDate(f.getDate() - dias); f.setHours(11 + (id % 7), 12 + id, 0, 0);
            return {
                id, fecha: f.toISOString(), clienteEmail: datos.clienteEmail ?? null,
                nombreContacto: datos.nombre, emailContacto: datos.email, telefonoContacto: datos.telefono || '',
                direccionEntrega: datos.direccion, notasPedido: datos.notas || '',
                subtotal, iva, total: subtotal + iva,
                montoAhorroProductos: ahorro > 0 ? ahorro : null,
                montoDescuentoCupon: descCupon > 0 ? descCupon : null,
                cuponAplicado: cupon ? cupon.codigo : null, estado, items
            };
        };
        const pedidos = [
            // Pedido de la captura PEDIDO.png / historial.png (Usuario Pruebas, 6 ítems)
            armarPedido(1, 26, { clienteEmail: 'usuario@magical.cl', nombre: 'Usuario Pruebas', email: 'usuario@magical.cl', telefono: '+56911111111', direccion: 'Av. Los Andes 1234, Santiago, Región Metropolitana, Chile (8320000)' },
                [snap(42, 1, 345000), snap(41, 1, 92000), snap(40, 1, 30000), snap(38, 1, 1800000), snap(37, 1, 12000), snap(39, 1, 23500)], 'ENTREGADO'),
            armarPedido(2, 9, { nombre: 'Camila Rojas', email: 'camila.rojas@correo.cl', telefono: '+56922223333', direccion: 'Los Aromos 455, Concepción, Biobío, Chile' },
                [snap(7, 1, 42990), snap(21, 2, 8491.5, 9990)], 'ENVIADO'),
            armarPedido(3, 4, { nombre: 'Valentina Soto', email: 'vale.soto@correo.cl', telefono: '+56944445555', direccion: 'Calle Larga 1020, Valdivia, Los Ríos, Chile', notas: 'Dejar en conserjería' },
                [snap(27, 1, 79990), snap(33, 1, 88000)], 'CONFIRMADO', { codigo: 'MAGIC20', monto: 33598 }),
            armarPedido(4, 1, { clienteEmail: 'usuario@magical.cl', nombre: 'Usuario Pruebas', email: 'usuario@magical.cl', telefono: '+56911111111', direccion: 'Av. Los Andes 1234, Santiago, Región Metropolitana, Chile (8320000)' },
                [snap(1, 1, 79990, 89990)], 'EN_PREPARACION')
        ];
        return {
            version: 1,
            rol: 'INVITADO',
            usuario: null,
            productos, descuentos, cupones, pedidos,
            carrito: { items: [], cuponAplicado: null, montoDescuentoCupon: null },
            suscriptores: [],
            seq: { producto: 42, pedido: 4 }
        };
    }

    // ------------------------------------------------------------------
    // 2. Estado + persistencia (localStorage envuelto en try/catch)
    // ------------------------------------------------------------------
    let S = cargar();
    const ui = {
        catalogo: { cat: null, sub: null, q: '', orden: 'recom', page: 0 },
        adminProductos: { q: '' },
        adminPedidos: { estado: 'TODOS', email: '' },
        ultimoPedido: null
    };

    function cargar() {
        try {
            const raw = window.localStorage.getItem(STORAGE_KEY);
            if (raw) {
                const data = JSON.parse(raw);
                if (data && data.version === 1 && Array.isArray(data.productos)) return data;
            }
        } catch (e) { /* almacenamiento bloqueado: seguimos en memoria */ }
        return semilla();
    }
    function guardar() {
        try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(S)); } catch (e) { /* sin persistencia */ }
    }
    function reiniciar() {
        try { window.localStorage.removeItem(STORAGE_KEY); } catch (e) { /* nada */ }
        S = semilla();
        ui.catalogo = { cat: null, sub: null, q: '', orden: 'recom', page: 0 };
        guardar();
        renderHeader();
        if (location.hash === '#/inicio') render(); else location.hash = '#/inicio';
        toast('La vitrina volvió a su estado inicial', 'info', 'bi-arrow-counterclockwise');
    }

    // ------------------------------------------------------------------
    // 3. Dominio: productos, descuentos y cupones
    // ------------------------------------------------------------------
    const producto = (id) => S.productos.find((p) => p.id === Number(id));
    const subcat = (id) => SUBCATEGORIAS.find((s) => s.id === Number(id));
    const categoria = (id) => CATEGORIAS.find((c) => c.id === Number(id));
    const catDeProducto = (p) => categoria(subcat(p.sub).cat);
    const esAdmin = () => S.rol === 'ADMIN';
    const autenticado = () => S.rol !== 'INVITADO' && !!S.usuario;

    // Descuento.estaVigente(): activo && hoy ∈ [inicio, fin]
    const vigente = (d) => d.activo && hoyISO() >= d.inicio && hoyISO() <= d.fin;
    // Descuento.calcularAhorro()
    const ahorroDe = (d, precio) => d.tipo === 'PORCENTAJE' ? precio * (d.valor / 100) : Math.min(d.valor, precio);
    // DescuentoServiceImpl.obtenerMejorDescuento(): producto > subcategoría > categoría > global, gana el mayor ahorro
    function mejorDescuento(p) {
        const sub = subcat(p.sub);
        const candidatos = S.descuentos.filter((d) => vigente(d) && (
            (d.alcance === 'PRODUCTO' && d.objetivo === p.id) ||
            (d.alcance === 'SUBCATEGORIA' && d.objetivo === sub.id) ||
            (d.alcance === 'CATEGORIA' && d.objetivo === sub.cat) ||
            d.alcance === 'GLOBAL'));
        if (!candidatos.length) return null;
        return candidatos.reduce((a, b) => (ahorroDe(b, p.precio) > ahorroDe(a, p.precio) ? b : a));
    }
    const precioConDescuento = (p) => { const d = mejorDescuento(p); return d ? Math.max(0, p.precio - ahorroDe(d, p.precio)) : p.precio; };
    const etiquetaDescuento = (d) => d.tipo === 'PORCENTAJE' ? `-${d.valor}%` : `-${clp(d.valor)}`;

    // Cupon.calcularDescuento() / Cupon.estaDisponible()
    const descuentoCupon = (c, total) => c.tipo === 'PORCENTAJE' ? total * (c.valor / 100) : Math.min(c.valor, total);
    const cuponDisponible = (c) => c.activo && (!c.fechaExpiracion || hoyISO() <= c.fechaExpiracion) && (c.limiteUsos == null || c.usosActuales < c.limiteUsos);
    // DescuentoServiceImpl.validarCupon()
    function validarCupon(codigo, totalCarrito) {
        const c = S.cupones.find((x) => x.codigo.toLowerCase() === codigo.trim().toLowerCase());
        if (!c) throw new MagicalBusinessException(`El código "${codigo}" no existe`);
        if (!c.activo) throw new MagicalBusinessException(`El cupón "${codigo}" está desactivado`);
        if (c.fechaExpiracion && hoyISO() > c.fechaExpiracion) throw new MagicalBusinessException(`El cupón "${codigo}" ha expirado`);
        if (c.limiteUsos != null && c.usosActuales >= c.limiteUsos) throw new MagicalBusinessException(`El cupón "${codigo}" ha alcanzado su límite de usos`);
        if (totalCarrito < c.montoMinimo) throw new MagicalBusinessException(`El cupón requiere un mínimo de ${clp(c.montoMinimo)} en tu carrito`);
        return c;
    }

    // ProductoServiceImpl.listar(): filtros + orden; invitados/clientes no ven productos sin stock
    function listarProductos({ q, cat, sub, orden }, admin) {
        const busqueda = (q || '').trim().toLowerCase();
        let lista = S.productos.filter((p) => {
            const s = subcat(p.sub);
            return (!busqueda || p.nombre.toLowerCase().includes(busqueda)) && (!cat || s.cat === cat) && (!sub || s.id === sub);
        });
        const cmp = {
            pmin: (a, b) => a.precio - b.precio,
            pmax: (a, b) => b.precio - a.precio,
            az: (a, b) => a.nombre.localeCompare(b.nombre, 'es'),
            za: (a, b) => b.nombre.localeCompare(a.nombre, 'es')
        }[orden] || ((a, b) => b.id - a.id);
        lista = lista.sort(cmp);
        return admin ? lista : lista.filter((p) => p.stock > 0);
    }

    // PaginacionHelper.paginar()
    function paginar(lista, page, size) {
        const safeSize = Math.max(size, 1);
        const total = lista.length;
        const totalPaginas = total === 0 ? 1 : Math.ceil(total / safeSize);
        const paginaSegura = Math.min(Math.max(page, 0), totalPaginas - 1);
        const desde = paginaSegura * safeSize;
        const hasta = Math.min(desde + safeSize, total);
        return {
            pagina: desde < total ? lista.slice(desde, hasta) : [],
            paginaActual: paginaSegura, totalPaginas, totalElementos: total,
            pagInicio: Math.max(0, paginaSegura - 2), pagFin: Math.min(totalPaginas - 1, paginaSegura + 2),
            primerElemento: total > 0 ? paginaSegura * safeSize + 1 : 0, ultimoElemento: hasta
        };
    }

    // ------------------------------------------------------------------
    // 4. Carrito (CarritoServiceImpl) y pedidos (PedidoServiceImpl)
    // ------------------------------------------------------------------
    const C = () => S.carrito;
    const totalCarrito = () => C().items.reduce((a, i) => a + i.precioUnitario * i.cantidad, 0);       // Carrito.getTotal()
    const ahorroCarrito = () => C().items.reduce((a, i) => a + (i.precioOriginal ? (i.precioOriginal - i.precioUnitario) * i.cantidad : 0), 0);
    const unidadesCarrito = () => C().items.reduce((a, i) => a + i.cantidad, 0);

    function agregarProducto(productoId, cantidad) {
        const p = producto(productoId);
        if (!p) throw new MagicalBusinessException('El producto no existe en el catálogo');
        if (p.stock <= 0) throw new MagicalBusinessException(`'${p.nombre}' está agotado`);
        const precioFinal = precioConDescuento(p);
        const tieneDescuento = precioFinal < p.precio;
        const existente = C().items.find((i) => i.productoId === p.id);
        const nuevaCantidad = (existente ? existente.cantidad : 0) + cantidad;
        if (nuevaCantidad > p.stock) throw new MagicalBusinessException(`Solo hay ${p.stock} unidades disponibles de '${p.nombre}'`);
        if (existente) {
            existente.cantidad = nuevaCantidad;
            existente.precioUnitario = precioFinal;
            existente.precioOriginal = tieneDescuento ? p.precio : null;
        } else {
            C().items.push({ productoId: p.id, cantidad, precioUnitario: precioFinal, precioOriginal: tieneDescuento ? p.precio : null });
        }
        recalcularCuponSiAplica();
        guardar();
    }
    function actualizarCantidad(productoId, nuevaCantidad) {
        const item = C().items.find((i) => i.productoId === productoId);
        if (!item) throw new MagicalBusinessException('Ítem no encontrado en el carrito');
        if (nuevaCantidad <= 0) return eliminarItem(productoId);
        const p = producto(productoId);
        if (nuevaCantidad > p.stock) throw new MagicalBusinessException(`Solo hay ${p.stock} unidades disponibles de '${p.nombre}'`);
        item.cantidad = nuevaCantidad;
        recalcularCuponSiAplica();
        guardar();
    }
    function eliminarItem(productoId) {
        C().items = C().items.filter((i) => i.productoId !== productoId);
        recalcularCuponSiAplica();
        guardar();
    }
    function vaciarCarrito() {
        S.carrito = { items: [], cuponAplicado: null, montoDescuentoCupon: null };
        guardar();
    }
    function aplicarCupon(codigo) {
        const total = totalCarrito();
        const c = validarCupon(codigo, total);
        C().cuponAplicado = c.codigo.toUpperCase();
        C().montoDescuentoCupon = descuentoCupon(c, total);
        guardar();
        return c;
    }
    function quitarCupon() { C().cuponAplicado = null; C().montoDescuentoCupon = null; guardar(); }
    // CarritoServiceImpl.recalcularCuponSiAplica(): si deja de cumplir, el cupón se quita
    function recalcularCuponSiAplica() {
        if (!C().cuponAplicado) return null;
        const c = S.cupones.find((x) => x.codigo.toUpperCase() === C().cuponAplicado);
        const total = totalCarrito();
        if (c && cuponDisponible(c) && total >= c.montoMinimo) {
            C().montoDescuentoCupon = descuentoCupon(c, total);
            return null;
        }
        const quitado = C().cuponAplicado;
        C().cuponAplicado = null; C().montoDescuentoCupon = null;
        return quitado;
    }
    // Productos borrados por la administradora desaparecen del caldero
    function limpiarCarritoHuerfano() {
        const antes = C().items.length;
        C().items = C().items.filter((i) => producto(i.productoId));
        if (C().items.length !== antes) recalcularCuponSiAplica();
    }
    function resumenCarrito() {
        const neto = totalCarrito();
        const cupon = C().montoDescuentoCupon || 0;
        const subtotal = Math.max(0, neto - cupon);
        const iva = subtotal * TASA_IVA;
        return { neto, cupon, ahorro: ahorroCarrito(), subtotal, iva, total: subtotal + iva, unidades: unidadesCarrito() };
    }

    // PedidoServiceImpl.crearDesdeCarrito()
    function crearPedido(dto) {
        if (!C().items.length) throw new MagicalBusinessException('El carrito está vacío. Agrega productos antes de continuar');
        const errores = C().items.filter((i) => producto(i.productoId).stock < i.cantidad)
            .map((i) => `'${producto(i.productoId).nombre}': solicitado ${i.cantidad}, disponible ${producto(i.productoId).stock}`);
        if (errores.length) throw new MagicalBusinessException('Stock insuficiente para: ' + errores.join(', '));
        const r = resumenCarrito();
        const cupon = C().cuponAplicado;
        const direccion = [dto.calle, dto.ciudad, dto.estadoRegion, dto.pais].filter(Boolean).join(', ') + (dto.codigoPostal ? ` (${dto.codigoPostal})` : '');
        const pedido = {
            id: ++S.seq.pedido, fecha: new Date().toISOString(),
            clienteEmail: autenticado() ? S.usuario.email : null, local: !autenticado(),
            nombreContacto: dto.nombreContacto, emailContacto: dto.emailContacto, telefonoContacto: dto.telefonoContacto || '',
            direccionEntrega: direccion, notasPedido: dto.notasPedido || '',
            subtotal: r.subtotal, iva: r.iva, total: r.total,
            montoAhorroProductos: r.ahorro > 0 ? r.ahorro : null,
            montoDescuentoCupon: r.cupon > 0 ? r.cupon : null,
            cuponAplicado: cupon, estado: 'PENDIENTE',
            items: C().items.map((i) => {
                const p = producto(i.productoId);
                return { productoId: p.id, nombreProducto: p.nombre, imagenProducto: p.imagen, cantidad: i.cantidad, precioUnitario: i.precioUnitario, precioOriginal: i.precioOriginal };
            })
        };
        S.pedidos.push(pedido);
        if (cupon) { const c = S.cupones.find((x) => x.codigo.toUpperCase() === cupon); if (c) c.usosActuales += 1; }
        C().items.forEach((i) => { producto(i.productoId).stock -= i.cantidad; });   // el stock baja al confirmar
        S.carrito = { items: [], cuponAplicado: null, montoDescuentoCupon: null };   // EstadoCarrito.COMPLETADO
        guardar();
        return pedido;
    }
    const pedidoPorId = (id) => S.pedidos.find((p) => p.id === Number(id));
    const esDueno = (p) => (autenticado() && p.clienteEmail === S.usuario.email) || (!autenticado() && p.local);
    // PedidoServiceImpl.cancelarPedido(): restaura stock
    function cancelarPedido(id) {
        const p = pedidoPorId(id);
        if (!p) throw new MagicalBusinessException(`Pedido #${id} no encontrado`);
        if (!esDueno(p)) throw new MagicalBusinessException('No tienes permiso para cancelar este pedido');
        if (['ENVIADO', 'ENTREGADO', 'CANCELADO', 'DEVOLUCION_SOLICITADA'].includes(p.estado)) throw new MagicalBusinessException('No es posible cancelar el pedido en estado: ' + p.estado);
        p.items.forEach((i) => { const prod = producto(i.productoId); if (prod) prod.stock += i.cantidad; });
        p.estado = 'CANCELADO';
        guardar();
    }
    // PedidoServiceImpl.solicitarDevolucion(): solo desde ENVIADO
    function solicitarDevolucion(id) {
        const p = pedidoPorId(id);
        if (!p) throw new MagicalBusinessException(`Pedido #${id} no encontrado`);
        if (!esDueno(p)) throw new MagicalBusinessException('No tienes permiso para solicitar devolución de este pedido');
        if (p.estado !== 'ENVIADO') throw new MagicalBusinessException('Solo puedes solicitar devolución de pedidos con estado ENVIADO');
        p.estado = 'DEVOLUCION_SOLICITADA';
        guardar();
    }
    const puedeCancelar = (p) => !['ENVIADO', 'ENTREGADO', 'CANCELADO', 'DEVOLUCION_SOLICITADA', 'DEVOLUCION_REALIZADA'].includes(p.estado);

    // ------------------------------------------------------------------
    // 5. Sesión simulada (AuthController / UsuarioServiceImpl)
    // ------------------------------------------------------------------
    const REGEX_PASSWORD = /^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
    const limpiarRut = (rut) => (rut || '').replace(/[.\-\s]/g, '').toUpperCase();
    function edad(fechaISO) {
        const n = new Date(fechaISO + 'T00:00:00'); const h = new Date();
        let e = h.getFullYear() - n.getFullYear();
        const m = h.getMonth() - n.getMonth();
        if (m < 0 || (m === 0 && h.getDate() < n.getDate())) e--;
        return e;
    }
    function iniciarSesion(rol, usuario) {
        S.rol = rol;
        S.usuario = usuario;
        guardar();
        renderHeader();
    }
    function cambiarRol(rol) {
        if (rol === S.rol) return;
        if (rol === 'INVITADO') {
            S.rol = 'INVITADO'; S.usuario = null; guardar(); renderHeader();
            toast('Sesión cerrada: ahora navegas como invitado/a', 'info', 'bi-person');
        } else if (rol === 'ADMIN') {
            iniciarSesion('ADMIN', { ...ADMIN_DEMO });
            toast('Hola, Administrador · Panel de gestión mágica activo', 'ok', 'bi-shield-check');
        } else {
            const previo = S.ultimoCliente || USUARIO_DEMO;
            iniciarSesion('CLIENT', { ...previo });
            toast(`Hola, ${previo.nombre} · sesión de cliente iniciada`, 'ok', 'bi-person-heart');
        }
        const r = ruta();
        if (r.vista === 'admin' && rol !== 'ADMIN') location.hash = '#/catalogo';
        else if (rol === 'ADMIN' && (r.vista === 'inicio')) location.hash = '#/admin/resumen';
        else render();
    }

    // ------------------------------------------------------------------
    // 6. Interfaz común: toasts, confirmaciones, header
    // ------------------------------------------------------------------
    function toast(mensaje, tipo = 'ok', icono = 'bi-stars', imagen = null) {
        const cont = $('#toasts');
        const el = document.createElement('div');
        el.className = `toast toast-magico ${tipo === 'error' ? 'error' : tipo === 'info' ? 'info' : ''}`;
        el.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
        el.innerHTML = `<div class="d-flex"><div class="toast-body">${imagen ? `<img src="${img(imagen)}" alt="">` : `<i class="bi ${icono} fs-5" aria-hidden="true"></i>`}<span>${esc(mensaje)}</span></div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button></div>`;
        cont.appendChild(el);
        const visibles = $$('.toast', cont); if (visibles.length > 3) visibles.slice(0, visibles.length - 3).forEach((v) => v.remove());
        const t = bootstrap.Toast.getOrCreateInstance(el, { delay: tipo === 'error' ? 5200 : 3200 });
        el.addEventListener('hidden.bs.toast', () => el.remove());
        t.show();
    }
    let accionConfirmada = null;
    function confirmar(titulo, html, textoOk, accion) {
        $('#confirmarTitulo').textContent = titulo;
        $('#confirmarBody').innerHTML = html;
        $('#confirmarOk').textContent = textoOk;
        accionConfirmada = accion;
        bootstrap.Modal.getOrCreateInstance($('#modalConfirmar')).show();
    }
    $('#confirmarOk').addEventListener('click', () => {
        bootstrap.Modal.getOrCreateInstance($('#modalConfirmar')).hide();
        if (accionConfirmada) { const a = accionConfirmada; accionConfirmada = null; a(); }
    });

    function renderHeader() {
        $$('.role-btn').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.rol === S.rol)));
        $$('[data-numerito]').forEach((n) => { n.textContent = unidadesCarrito(); });
        $('#adminBar').hidden = !esAdmin();
        $('#menuCategorias').innerHTML = CATEGORIAS.map((c) => `<li><a class="dropdown-item" href="#/catalogo" data-action="ir-categoria" data-cat="${c.id}">${esc(c.nombre)}</a></li>`).join('') +
            `<li><hr class="dropdown-divider"></li><li><a class="dropdown-item text-info-magic" href="#/catalogo" data-action="ir-categoria" data-cat="">✨ Ver todo el catálogo</a></li>`;
        const nav = $('#navUser');
        if (autenticado()) {
            nav.innerHTML = `<div class="nav-item dropdown">
                <button class="btn btn-perfil-magic dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-person-circle fs-5" aria-hidden="true"></i><span>Hola, ${esc(S.usuario.nombre)}</span></button>
                <ul class="dropdown-menu dropdown-menu-end dropdown-menu-dark dropdown-magic">
                    <li><span class="dropdown-item-text small text-white-50">${esc(S.usuario.email)} · ${S.rol === 'ADMIN' ? 'ROLE_ADMIN' : 'ROLE_CLIENT'}</span></li>
                    <li><a class="dropdown-item" href="#/mis-pedidos"><i class="bi bi-bag-heart me-2" aria-hidden="true"></i>Mis Pedidos</a></li>
                    ${esAdmin() ? '<li><a class="dropdown-item" href="#/admin/resumen"><i class="bi bi-shield-check me-2" aria-hidden="true"></i>Gestión mágica</a></li>' : ''}
                    <li><hr class="dropdown-divider"></li>
                    <li><button class="dropdown-item fw-bold" type="button" data-action="rol" data-rol="INVITADO"><i class="bi bi-box-arrow-right me-2" aria-hidden="true"></i>Cerrar Sesión</button></li>
                </ul></div>`;
        } else {
            nav.innerHTML = `<button type="button" class="btn-login-circle" data-action="abrir-auth" data-tab="login" title="Iniciar sesión" aria-label="Iniciar sesión o registrarse">
                <img src="img/inicio-sesion.webp" width="52" height="52" alt=""></button>`;
        }
    }
    function bumpCarrito() {
        $$('[data-numerito]').forEach((n) => { n.classList.remove('bump'); void n.offsetWidth; n.classList.add('bump'); });
    }
    // Micro-animación: la imagen vuela hasta el caldero dejando chispas
    function volarAlCaldero(origenImg) {
        const destino = $$('.btn-carrito-magic').find((b) => b.offsetParent !== null);
        if (!origenImg || !destino || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        const a = origenImg.getBoundingClientRect(); const b = destino.getBoundingClientRect();
        if (!a.width) return;
        const tam = Math.min(90, a.width * .55);
        const clon = document.createElement('img');
        clon.src = origenImg.currentSrc || origenImg.src; clon.className = 'vuelo'; clon.alt = '';
        Object.assign(clon.style, { width: tam + 'px', height: tam + 'px', left: (a.left + a.width / 2 - tam / 2) + 'px', top: (a.top + a.height / 2 - tam / 2) + 'px' });
        document.body.appendChild(clon);
        requestAnimationFrame(() => {
            const dx = b.left + b.width / 2 - (a.left + a.width / 2); const dy = b.top + b.height / 2 - (a.top + a.height / 2);
            clon.style.transform = `translate(${dx}px, ${dy}px) scale(.18) rotate(200deg)`; clon.style.opacity = '.4';
        });
        setTimeout(() => {
            clon.remove();
            for (let k = 0; k < 7; k++) {
                const s = document.createElement('span'); s.className = 'chispa'; s.textContent = k % 2 ? '✦' : '✧';
                const ang = (Math.PI * 2 * k) / 7; s.style.setProperty('--dx', Math.cos(ang) * 34 + 'px'); s.style.setProperty('--dy', Math.sin(ang) * 34 + 'px');
                s.style.left = (b.left + b.width / 2 - 8) + 'px'; s.style.top = (b.top + b.height / 2 - 8) + 'px';
                document.body.appendChild(s); setTimeout(() => s.remove(), 950);
            }
            bumpCarrito();
        }, 820);
    }
    function confeti() {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        const simbolos = ['✦', '✧', '★', '♥', '☾', '✿'];
        const colores = ['#f480ff', '#6907ab', '#00d4aa', '#ffd166', '#f915a2', '#7bc1ff'];
        for (let i = 0; i < 38; i++) {
            const c = document.createElement('span'); c.className = 'confeti';
            c.textContent = simbolos[i % simbolos.length]; c.style.color = colores[i % colores.length];
            c.style.left = Math.random() * 100 + 'vw'; c.style.fontSize = (12 + Math.random() * 16) + 'px';
            c.style.animationDuration = (2.2 + Math.random() * 1.8) + 's'; c.style.animationDelay = (Math.random() * .6) + 's';
            document.body.appendChild(c); setTimeout(() => c.remove(), 4800);
        }
    }

    // ------------------------------------------------------------------
    // 7. Router por hash
    // ------------------------------------------------------------------
    function ruta() {
        const partes = (location.hash || '#/inicio').replace(/^#\/?/, '').split('/');
        return { vista: partes[0] || 'inicio', arg: partes[1] || null };
    }
    function render() {
        const { vista, arg } = ruta();
        limpiarCarritoHuerfano();
        const app = $('#app');
        let html;
        switch (vista) {
            case 'catalogo': html = vistaCatalogo(); break;
            case 'caldero': html = vistaCaldero(); break;
            case 'checkout': html = vistaCheckout(); break;
            case 'confirmacion': html = vistaConfirmacion(arg); break;
            case 'mis-pedidos': html = vistaMisPedidos(); break;
            case 'admin': html = esAdmin() ? vistaAdmin(arg || 'resumen') : vistaSinPermiso(); break;
            default: html = vistaInicio();
        }
        app.innerHTML = `<div class="view">${html}</div>`;
        $$('.nav-link-magico').forEach((a) => a.classList.toggle('active', a.dataset.nav === vista || (vista === 'inicio' && arg === 'tips' && a.dataset.nav === 'tips')));
        $$('.admin-nav a').forEach((a) => a.classList.toggle('active', a.getAttribute('href') === `#/admin/${arg || 'resumen'}` && vista === 'admin'));
        renderHeader();
        despuesDeRender(vista, arg);
    }
    function despuesDeRender(vista, arg) {
        if (vista === 'inicio') {
            const car = $('#carruselCats');
            if (car) bootstrap.Carousel.getOrCreateInstance(car, { interval: 4000, ride: 'carousel' });
            if (arg === 'tips') { const t = $('#tipBelleza'); if (t) setTimeout(() => t.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60); return; }
        }
        if (vista === 'admin' && (arg || 'resumen') === 'resumen') {
            requestAnimationFrame(() => $$('.relleno').forEach((r) => { r.style.width = r.dataset.w; }));
        }
        if (vista === 'confirmacion' && ui.ultimoPedido === Number(arg)) { confeti(); ui.ultimoPedido = null; }
        window.scrollTo({ top: 0, behavior: 'instant' in window ? 'instant' : 'auto' });
    }

    // ------------------------------------------------------------------
    // 8. Vistas
    // ------------------------------------------------------------------
    function vistaInicio() {
        const novedades = listarProductos({ orden: 'recom' }, false).slice(0, 3);
        return `
        <div class="container mt-4"><nav class="subcategorias-nav" aria-label="Categorías">
            ${CATEGORIAS.map((c) => `<a class="btn-subcategoria" href="#/catalogo" data-action="ir-categoria" data-cat="${c.id}">${esc(c.nombre)}</a>`).join('')}
        </nav></div>
        <section class="hero-home" aria-labelledby="heroTitulo">
            <h1 class="visually-hidden" id="heroTitulo">Magical Alliance</h1>
            <img src="img/logo-texto.webp" alt="Magical Alliance" class="logo-texto" width="400" height="245">
            <p class="lead">¿Recuerdas la magia de la transformación? Inspirada en la fuerza estelar de Sailor Moon y el encanto dulce de Sakura Card Captor, Magical Alliance es una invitación a vivir tu propia fantasía.</p>
            <p class="fw-bolder fst-italic mb-0 mt-2">¡Es hora de dejar que tu luz brille!</p>
        </section>
        <section class="tour container" aria-label="Recorrido sugerido">
            <a class="tour-paso text-decoration-none" href="#/catalogo"><span class="tour-num">1</span><span><strong>Explora y agrega</strong><span>Filtra por categoría, busca y lleva tesoros al caldero.</span></span></a>
            <a class="tour-paso text-decoration-none" href="#/caldero"><span class="tour-num">2</span><span><strong>Prueba el checkout</strong><span>Aplica el cupón <b>MAGIC20</b>, confirma y revisa tu historial.</span></span></a>
            <button type="button" class="tour-paso" data-action="rol" data-rol="ADMIN"><span class="tour-num">3</span><span><strong>Gestiona como administradora</strong><span>Edita stock, crea productos y cambia estados de pedidos.</span></span></button>
        </section>
        <div id="carruselCats" class="carousel slide carrusel-cats" aria-label="Colecciones">
            <div class="carousel-indicators">
                ${CATEGORIAS.map((c, i) => `<button type="button" data-bs-target="#carruselCats" data-bs-slide-to="${i}" ${i === 0 ? 'class="active" aria-current="true"' : ''} aria-label="${esc(c.nombre)}"></button>`).join('')}
            </div>
            <div class="carousel-inner">
                ${CATEGORIAS.map((c, i) => `<div class="carousel-item ${i === 0 ? 'active' : ''}">
                    <a href="#/catalogo" data-action="ir-categoria" data-cat="${c.id}"><img src="${img(c.banner)}" alt="Colección ${esc(c.nombre)}" ${i ? 'loading="lazy"' : ''}>
                    <div class="carousel-caption"><h2 class="text-uppercase">${esc(c.nombre)}</h2><p class="mb-0">Haz clic para explorar esta colección mágica</p></div></a></div>`).join('')}
            </div>
            <button class="carousel-control-prev" type="button" data-bs-target="#carruselCats" data-bs-slide="prev"><span class="carousel-control-prev-icon" aria-hidden="true"></span><span class="visually-hidden">Anterior</span></button>
            <button class="carousel-control-next" type="button" data-bs-target="#carruselCats" data-bs-slide="next"><span class="carousel-control-next-icon" aria-hidden="true"></span><span class="visually-hidden">Siguiente</span></button>
        </div>
        <section class="container" aria-labelledby="novTitulo">
            <div class="seccion-titulo"><h2 id="novTitulo">✨ Tesoros Recién Llegados ✨</h2><p>Las últimas novedades de nuestro reino mágico</p></div>
            <div class="grid-productos" style="max-width:980px">${novedades.map(tarjetaProducto).join('')}</div>
        </section>
        <section class="tip-card" id="tipBelleza" aria-labelledby="tipTitulo">
            <div>
                <h2 id="tipTitulo">🎨 El Secreto de la Transformación: Encuentra tu Estación Mágica</h2>
                <p class="mt-3">Conocer tu colorimetría es el primer paso para dominar tu poder estelar. Se basa en identificar si tus tonos base son fríos (azules o rosados) o cálidos (dorados o melocotón) y qué tan vibrantes o suaves son, clasificándote en una de las cuatro estaciones.</p>
                <p class="mb-0"><strong class="fst-italic">🔍 Paso 1: Identifica tu Tono Base.</strong> Mira tus venas en la muñeca a la luz natural: si son verdes eres cálido, si son azules o violetas eres frío, y si no distingues el color eres neutral… ¡todos los colores te quedan estupendo!</p>
            </div>
            <img src="img/colorimetria.webp" alt="Rueda de estaciones de colorimetría" loading="lazy" width="260" height="260">
        </section>
        <section class="nosotros" aria-labelledby="nosTitulo">
            <h2 id="nosTitulo">✨ Acerca de Nosotros: Magical Alliance</h2>
            <p>Magical Alliance no es solo una marca; es una microempresa familiar nacida de la ambición de transformar la experiencia de la belleza. Nos impulsa la nostalgia y el deseo de revivir la alegría y el color de la infancia, inspirándonos en las icónicas guerreras mágicas de los años 90.</p>
            <div class="sellos" aria-label="Sellos de la marca">
                <img src="img/sello-cruelty-free.webp" alt="Sello Cruelty-Free" loading="lazy"><img src="img/sello-vegan.webp" alt="Sello Vegan" loading="lazy">
                <img src="img/sello-work-ethics.webp" alt="Sello Work Ethics" loading="lazy"><img src="img/sello-sustentable.webp" alt="Sello Sustainable Packaging" loading="lazy">
                <img src="img/sello-pureglam.webp" alt="Sello PureGlam" loading="lazy">
            </div>
        </section>`;
    }

    function badgeStock(p) {
        if (p.stock <= 0) return `<span class="stock-agotado"><i class="bi bi-x-circle" aria-hidden="true"></i> Agotado</span>`;
        return `<span class="stock-disponible ${p.stock <= STOCK_BAJO ? 'stock-bajo' : ''}"><i class="bi bi-stars" aria-hidden="true"></i> Stock: ${p.stock}${p.stock <= STOCK_BAJO ? ' · ¡últimas!' : ''}</span>`;
    }
    function tarjetaProducto(p) {
        const sub = subcat(p.sub);
        const d = mejorDescuento(p);
        const final = precioConDescuento(p);
        const enCaldero = (C().items.find((i) => i.productoId === p.id) || {}).cantidad || 0;
        const maxAgregable = Math.max(0, p.stock - enCaldero);
        return `<article class="card-magica ${p.stock <= 0 ? 'agotado' : ''}" data-producto="${p.id}">
            ${d ? `<span class="ribbon-desc" title="${esc(d.nombre)}">${etiquetaDescuento(d)} ${esc(d.nombre)}</span>` : ''}
            <button type="button" class="card-img-btn" data-action="detalle" data-id="${p.id}" aria-label="Ver detalle de ${esc(p.nombre)}">
                <img src="${img(p.imagen)}" alt="${esc(p.nombre)}" loading="lazy" width="300" height="300"></button>
            <h3>${esc(p.nombre)}</h3>
            <p class="desc">${esc(p.descripcion)}</p>
            <span class="badge-sub">${esc(sub.nombre)}</span>
            <span class="precio-magico">${d ? `<span class="precio-tachado">${clp(p.precio)}</span>` : ''}${clp(final)}</span>
            ${badgeStock(p)}
            <div class="card-acciones">
                ${p.stock > 0 ? `
                <div class="cantidad-ctrl" role="group" aria-label="Cantidad de ${esc(p.nombre)}">
                    <button type="button" class="btn-cant" data-action="cant-card" data-delta="-1" aria-label="Restar uno">−</button>
                    <input class="input-cant" type="number" value="1" min="1" max="${Math.max(1, maxAgregable)}" readonly aria-label="Cantidad">
                    <button type="button" class="btn-cant" data-action="cant-card" data-delta="1" aria-label="Sumar uno" ${maxAgregable <= 1 ? 'disabled' : ''}>+</button>
                </div>
                <button type="button" class="btn btn-magico btn-anadir" data-action="agregar" data-id="${p.id}" ${maxAgregable <= 0 ? 'disabled' : ''}>
                    <i class="bi bi-cart-plus" aria-hidden="true"></i> <span>${maxAgregable <= 0 ? 'Todo en tu caldero' : 'Añadir<span class="txt-largo"> al caldero</span>'}</span> <i class="bi bi-moon-stars-fill" aria-hidden="true"></i></button>`
                : `<button class="btn btn-secondary w-100 rounded-pill" disabled>Sin Stock</button>`}
                ${esAdmin() ? `<button type="button" class="link-admin" data-action="editar-producto" data-id="${p.id}"><i class="bi bi-pencil-square" aria-hidden="true"></i> Editar producto</button>` : ''}
            </div>
        </article>`;
    }

    function vistaCatalogo() {
        const f = ui.catalogo;
        const lista = listarProductos(f, esAdmin());
        const pg = paginar(lista, f.page, TAM_PAGINA);
        f.page = pg.paginaActual;
        const subs = f.cat ? SUBCATEGORIAS.filter((s) => s.cat === f.cat) : [];
        const titulo = f.sub ? subcat(f.sub).nombre : f.cat ? categoria(f.cat).nombre : 'Catálogo Mágico';
        let paginas = '';
        for (let i = pg.pagInicio; i <= pg.pagFin; i++) paginas += `<button type="button" data-action="pagina" data-page="${i}" ${i === pg.paginaActual ? 'aria-current="page"' : ''} aria-label="Página ${i + 1}">${i + 1}</button>`;
        return `
        <div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">🔮</span>${esc(titulo)}</h1>
            <p>${esAdmin() ? 'Vista de administradora: también ves los productos agotados.' : 'Los productos sin stock se ocultan para invitados y clientes, igual que en ProductoServiceImpl.'}</p></div>
        ${esAdmin() ? `<div class="text-center mb-3"><button type="button" class="btn btn-magico px-4 py-2 fs-5" data-action="nuevo-producto"><i class="bi bi-plus-circle-fill" aria-hidden="true"></i> Registrar Nuevo Tesoro Mágico</button></div>` : ''}
        <form class="barra-filtros container" id="formFiltros" role="search" aria-label="Filtrar catálogo">
            <label class="visually-hidden" for="txtBuscar">Buscar por nombre</label>
            <input id="txtBuscar" class="form-control" type="search" placeholder="Buscar por nombre..." value="${esc(f.q)}" autocomplete="off">
            <label class="visually-hidden" for="selOrden">Ordenar</label>
            <select id="selOrden" class="form-select">
                ${[['recom', '✨ Recomendados'], ['az', '🔤 Nombre: A - Z'], ['za', '🔤 Nombre: Z - A'], ['pmin', '💰 Precio: Menor a Mayor'], ['pmax', '💰 Precio: Mayor a Menor']]
                    .map(([v, t]) => `<option value="${v}" ${f.orden === v ? 'selected' : ''}>${t}</option>`).join('')}
            </select>
            <button type="submit" class="btn btn-magico">Filtrar</button>
            <button type="button" class="btn-filtro-magic" data-action="limpiar-filtros">Limpiar</button>
        </form>
        <div class="text-center mt-3"><button type="button" class="btn-catalogo-completo" data-action="ir-categoria" data-cat="">✨ Ver Catálogo Completo</button></div>
        <div class="chips-cat container" role="group" aria-label="Categorías">
            ${CATEGORIAS.map((c) => `<button type="button" class="chip-cat ${f.cat === c.id ? 'active' : ''}" data-action="filtro-cat" data-cat="${c.id}" aria-pressed="${f.cat === c.id}">${esc(c.nombre)}</button>`).join('')}
        </div>
        ${subs.length ? `<div class="chips-sub container" role="group" aria-label="Subcategorías">
            <button type="button" class="chip-sub ${!f.sub ? 'active' : ''}" data-action="filtro-sub" data-sub="" aria-pressed="${!f.sub}">Todas</button>
            ${subs.map((s) => `<button type="button" class="chip-sub ${f.sub === s.id ? 'active' : ''}" data-action="filtro-sub" data-sub="${s.id}" aria-pressed="${f.sub === s.id}">${esc(s.nombre)}</button>`).join('')}</div>` : ''}
        <p class="resumen-resultados" role="status">${pg.totalElementos ? `Mostrando ${pg.primerElemento}–${pg.ultimoElemento} de ${pg.totalElementos} tesoros` : ''}</p>
        ${pg.totalElementos ? `<div class="grid-productos">${pg.pagina.map(tarjetaProducto).join('')}</div>` :
            `<div class="panel vacio container" style="max-width:720px"><i class="bi bi-search-heart" aria-hidden="true"></i><h2>No encontramos tesoros con ese hechizo</h2><p class="text-muted">Prueba con otra palabra o limpia los filtros.</p><button type="button" class="btn btn-magico" data-action="limpiar-filtros">✨ Ver todo</button></div>`}
        ${pg.totalPaginas > 1 ? `<nav class="paginacion" aria-label="Paginación">
            <button type="button" data-action="pagina" data-page="${pg.paginaActual - 1}" ${pg.paginaActual === 0 ? 'disabled' : ''} aria-label="Página anterior"><i class="bi bi-chevron-left" aria-hidden="true"></i></button>
            ${paginas}
            <button type="button" data-action="pagina" data-page="${pg.paginaActual + 1}" ${pg.paginaActual >= pg.totalPaginas - 1 ? 'disabled' : ''} aria-label="Página siguiente"><i class="bi bi-chevron-right" aria-hidden="true"></i></button>
        </nav>` : ''}`;
    }

    function abrirDetalle(id) {
        const p = producto(id); if (!p) return;
        const sub = subcat(p.sub); const cat = categoria(sub.cat);
        const d = mejorDescuento(p); const final = precioConDescuento(p);
        const enCaldero = (C().items.find((i) => i.productoId === p.id) || {}).cantidad || 0;
        const max = Math.max(0, p.stock - enCaldero);
        // ProductoController: relacionados por subcategoría, luego por categoría (máx. 8; mostramos 4)
        let rel = S.productos.filter((x) => x.sub === p.sub && x.id !== p.id);
        if (rel.length < 4) rel = rel.concat(S.productos.filter((x) => subcat(x.sub).cat === sub.cat && x.sub !== p.sub && x.id !== p.id));
        rel = rel.filter((x) => esAdmin() || x.stock > 0).slice(0, 4);
        $('#detalleTitulo').textContent = p.nombre;
        $('#detalleBody').innerHTML = `
            <div class="detalle-grid" data-producto="${p.id}">
                <div class="detalle-img"><img src="${img(p.imagen)}" alt="${esc(p.nombre)}"></div>
                <div>
                    <span class="badge-cat">${esc(cat.nombre)}</span> <span class="badge-subcat">${esc(sub.nombre)}</span>
                    <p class="detalle-nombre" aria-hidden="true">${esc(p.nombre)}</p>
                    <p class="fs-6 text-secondary" style="line-height:1.7">${esc(p.descripcion)}</p>
                    <div class="d-flex flex-wrap align-items-center gap-3 my-3">
                        <span class="detalle-precio">${clp(final)}</span>
                        ${d ? `<span class="precio-tachado fs-5">${clp(p.precio)}</span><span class="ribbon-desc position-static">${etiquetaDescuento(d)} · ${esc(d.nombre)}</span>` : ''}
                        ${badgeStock(p)}
                    </div>
                    ${p.stock > 0 ? `
                    <div class="cantidad-ctrl mb-3" role="group" aria-label="Cantidad">
                        <button type="button" class="btn-cant" data-action="cant-card" data-delta="-1" aria-label="Restar uno">−</button>
                        <input class="input-cant" type="number" value="1" min="1" max="${Math.max(1, max)}" readonly aria-label="Cantidad">
                        <button type="button" class="btn-cant" data-action="cant-card" data-delta="1" aria-label="Sumar uno" ${max <= 1 ? 'disabled' : ''}>+</button>
                    </div>
                    <button type="button" class="btn btn-magico px-4 py-2 fs-5 btn-anadir" style="width:auto" data-action="agregar" data-id="${p.id}" ${max <= 0 ? 'disabled' : ''}>
                        <i class="bi bi-cart-plus" aria-hidden="true"></i> ${max <= 0 ? 'Ya tienes todo el stock en tu caldero' : 'Añadir al caldero'} <i class="bi bi-moon-stars-fill" aria-hidden="true"></i></button>
                    ${enCaldero ? `<p class="small text-muted mt-2 mb-0"><i class="bi bi-basket2" aria-hidden="true"></i> Ya tienes ${enCaldero} en tu caldero.</p>` : ''}` :
                    `<button class="btn btn-secondary rounded-pill px-4" disabled><i class="bi bi-x-circle me-2" aria-hidden="true"></i>Sin Stock</button>`}
                    ${d ? `<p class="small mt-3 mb-0 alerta-info"><i class="bi bi-magic" aria-hidden="true"></i> Descuento automático (${esc(d.alcance.toLowerCase())}): el precio rebajado se congela al añadirlo al caldero, como en <code>CarritoServiceImpl.agregarProducto()</code>.</p>` : ''}
                    ${esAdmin() ? `<hr><button type="button" class="link-admin fs-6" data-action="editar-producto" data-id="${p.id}"><i class="bi bi-pencil-square" aria-hidden="true"></i> Editar producto</button>` : ''}
                </div>
            </div>
            ${rel.length ? `<h3 class="h6 mt-4 mb-3 fw-bold" style="color:var(--ma-purple)">También te puede encantar</h3>
            <div class="relacionados">${rel.map((r) => `<button type="button" class="mini-card" data-action="detalle" data-id="${r.id}"><img src="${img(r.imagen)}" alt="" loading="lazy"><span>${esc(r.nombre)}</span><span style="color:var(--ma-purple)">${clp(precioConDescuento(r))}</span></button>`).join('')}</div>` : ''}`;
        const el = $('#modalDetalle');
        if (el.classList.contains('show')) {
            const foco = $('[data-action="agregar"]:not(:disabled)', el) || $('.btn-close', el);
            foco.focus({ preventScroll: true });
        } else {
            bootstrap.Modal.getOrCreateInstance(el).show();
            $('.modal-body', el).scrollTop = 0;
        }
    }

    function vistaCaldero() {
        const items = C().items;
        const cab = `<div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">🛒</span>Tu Caldero Mágico</h1><p>Revisa tus tesoros antes de continuar</p></div>`;
        if (!items.length) {
            return cab + `<div class="panel vacio" style="max-width:1100px;margin:0 auto"><i class="bi bi-bag-x" aria-hidden="true"></i><h2>Tu caldero está vacío</h2>
                <p class="text-muted">¡Agrega tesoros mágicos para comenzar tu aventura!</p><a class="btn btn-magico" href="#/catalogo">✨ Explorar el Catálogo</a></div>`;
        }
        const r = resumenCarrito();
        const cuponesDemo = S.cupones.map((c) => `<button type="button" data-action="usar-cupon" data-codigo="${c.codigo}">${c.codigo}</button>`).join('');
        return cab + `<div class="layout-compra">
            <div>
                <div class="panel"><table class="tabla-caldero">
                    <caption class="visually-hidden">Productos en tu caldero</caption>
                    <thead><tr><th scope="col">Producto</th><th scope="col" class="text-center">Precio</th><th scope="col" class="text-center">Cantidad</th><th scope="col" class="text-center">Subtotal</th><th scope="col" class="text-center">Acción</th></tr></thead>
                    <tbody>${items.map((i) => {
                        const p = producto(i.productoId);
                        return `<tr>
                            <td class="c-prod"><div class="item-prod"><button type="button" class="p-0 border-0 bg-transparent" data-action="detalle" data-id="${p.id}" aria-label="Ver ${esc(p.nombre)}"><img src="${img(p.imagen)}" alt=""></button>
                                <div><strong>${esc(p.nombre)}</strong><small>${esc(subcat(p.sub).nombre)}</small>${i.precioOriginal ? `<br><span class="tag-ahorro">Antes ${clp(i.precioOriginal)}</span>` : ''}</div></div></td>
                            <td class="c-precio text-center precio-celda">${clp(i.precioUnitario)}</td>
                            <td class="c-cant text-center"><div class="cantidad-ctrl" role="group" aria-label="Cantidad de ${esc(p.nombre)}">
                                <button type="button" class="btn-cant" data-action="cant-item" data-id="${p.id}" data-delta="-1" aria-label="${i.cantidad === 1 ? 'Quitar' : 'Restar uno de'} ${esc(p.nombre)}">−</button>
                                <span class="fw-bold" style="min-width:22px;display:inline-block" aria-live="polite">${i.cantidad}</span>
                                <button type="button" class="btn-cant" data-action="cant-item" data-id="${p.id}" data-delta="1" aria-label="Sumar uno de ${esc(p.nombre)}" ${i.cantidad >= p.stock ? 'disabled title="No hay más stock"' : ''}>+</button></div>
                                ${i.cantidad >= p.stock ? '<small class="d-block text-muted mt-1">máx. stock</small>' : ''}</td>
                            <td class="c-sub text-center precio-celda">${clp(i.precioUnitario * i.cantidad)}</td>
                            <td class="c-acc text-center"><button type="button" class="btn-icono peligro rounded-circle" data-action="quitar-item" data-id="${p.id}" aria-label="Eliminar ${esc(p.nombre)}"><i class="bi bi-trash3" aria-hidden="true"></i></button></td>
                        </tr>`;
                    }).join('')}</tbody></table></div>
                <div class="d-flex justify-content-between flex-wrap gap-2 mt-3">
                    <a class="btn btn-outline-magico" href="#/catalogo"><i class="bi bi-arrow-left" aria-hidden="true"></i> Seguir comprando</a>
                    <button type="button" class="btn btn-outline-magico" data-action="vaciar"><i class="bi bi-trash3" aria-hidden="true"></i> Vaciar carrito</button>
                </div>
            </div>
            <aside class="panel sticky-col resumen-pedido" aria-labelledby="resTitulo">
                <div class="panel-header grad" id="resTitulo">✨ Resumen del Pedido</div>
                <div class="panel-body">
                    <div class="linea"><span>Neto (${r.unidades} uds.)</span><strong>${clp(r.neto)}</strong></div>
                    ${r.ahorro > 0 ? `<div class="linea ahorro small"><span><i class="bi bi-magic" aria-hidden="true"></i> Ya ahorraste con descuentos</span><strong>${clp(r.ahorro)}</strong></div>` : ''}
                    ${r.cupon > 0 ? `<div class="linea ahorro"><span>Cupón ${esc(C().cuponAplicado)}</span><strong>− ${clp(r.cupon)}</strong></div>` : ''}
                    <div class="linea"><span>IVA (19%)</span><strong>${clp(r.iva)}</strong></div>
                    <div class="linea"><span>Envío</span><span class="envio">Por coordinar</span></div>
                    <div class="linea total"><span>Total</span><strong>${clp(r.total)}</strong></div>
                    <div class="mt-3 cupon-box">
                        ${C().cuponAplicado ? `<div class="cupon-aplicado"><span><i class="bi bi-ticket-perforated-fill text-success" aria-hidden="true"></i> <strong>${esc(C().cuponAplicado)}</strong> aplicado</span>
                            <button type="button" class="btn btn-sm btn-outline-peligro" data-action="quitar-cupon">Quitar</button></div>` : `
                        <form id="formCupon" novalidate>
                            <label for="inputCupon" class="small fw-bold mb-1">¿Tienes un código de descuento?</label>
                            <div class="input-group"><input id="inputCupon" class="form-control" placeholder="EJ: MAGIC20" autocomplete="off" maxlength="50"><button type="submit" class="btn btn-magico">Aplicar</button></div>
                        </form>
                        <p class="cupones-demo mb-0">Cupones de la demo (cada uno prueba una regla de <code>validarCupon</code>): ${cuponesDemo}</p>`}
                    </div>
                    <a class="btn btn-magico w-100 mt-3 py-2" href="#/checkout">Proceder al Checkout <i class="bi bi-arrow-right" aria-hidden="true"></i></a>
                </div>
            </aside>
        </div>`;
    }

    function vistaCheckout() {
        if (!C().items.length) { setTimeout(() => { location.hash = '#/caldero'; }, 0); return ''; }
        const u = autenticado() ? S.usuario : {};
        const r = resumenCarrito();
        const campo = (id, label, valor, req, attrs = '', ancho = 'col-md-6') => `<div class="${ancho}">
            <label class="form-label fw-bold small" for="${id}">${label}${req ? ' <span class="req" aria-hidden="true">*</span>' : ''}</label>
            <input class="form-control" id="${id}" name="${id}" value="${esc(valor || '')}" ${req ? 'required aria-required="true"' : ''} ${attrs}>
            <div class="invalid-feedback"></div></div>`;
        return `<div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">🌙</span>Datos de tu Pedido</h1><p>${autenticado() ? 'Prellenamos tus datos y tu dirección principal, como CarritoController.verCheckout().' : 'Compras como invitado/a: el checkout está permitido sin sesión (/carrito/** es permitAll).'}</p></div>
        <form class="layout-compra" id="formCheckout" novalidate>
            <div class="d-flex flex-column gap-3">
                ${!autenticado() ? `<div class="alerta-info"><i class="bi bi-info-circle" aria-hidden="true"></i> ¿Tienes cuenta? <button type="button" class="btn btn-link p-0 align-baseline fw-bold" data-action="abrir-auth" data-tab="login">Inicia sesión</button> para autocompletar y ver el pedido en tu historial.</div>` : ''}
                <section class="panel"><h2 class="panel-header h6 mb-0"><i class="bi bi-person-circle" aria-hidden="true"></i> Datos de Contacto</h2>
                    <div class="panel-body row g-3">
                        ${campo('nombreContacto', 'Nombre completo', u.nombre ? `${u.nombre} ${u.apellido || ''}`.trim() : '', true, 'autocomplete="name" maxlength="120"')}
                        ${campo('emailContacto', 'Email', u.email, true, 'type="email" autocomplete="email"')}
                        ${campo('telefonoContacto', 'Teléfono', u.telefono, false, 'type="tel" autocomplete="tel" placeholder="+569..."')}
                    </div></section>
                <section class="panel"><h2 class="panel-header h6 mb-0"><i class="bi bi-geo-alt" aria-hidden="true"></i> Dirección de Entrega</h2>
                    <div class="panel-body row g-3">
                        ${campo('calle', 'Calle y número', u.calle, true, 'placeholder="Ej: Av. Los Andes 1234" autocomplete="street-address"', 'col-12')}
                        ${campo('ciudad', 'Ciudad', u.ciudad, true, 'placeholder="Ej: Santiago"')}
                        ${campo('estadoRegion', 'Región / Estado', u.estadoRegion, true, 'placeholder="Ej: Región Metropolitana"')}
                        ${campo('pais', 'País', u.pais, true, 'placeholder="Ej: Chile" autocomplete="country-name"')}
                        ${campo('codigoPostal', 'Código Postal', u.codigoPostal, false, 'placeholder="Ej: 8320000" autocomplete="postal-code"')}
                    </div></section>
                <section class="panel"><h2 class="panel-header h6 mb-0"><i class="bi bi-chat-heart" aria-hidden="true"></i> Notas del Pedido (opcional)</h2>
                    <div class="panel-body"><label class="visually-hidden" for="notasPedido">Notas del pedido</label>
                    <textarea class="form-control" id="notasPedido" rows="3" maxlength="500" placeholder="Instrucciones de entrega, mensajes especiales, etc."></textarea></div></section>
            </div>
            <aside class="panel sticky-col resumen-pedido" aria-labelledby="resCheckout">
                <div class="panel-header grad" id="resCheckout">✨ Resumen del Pedido</div>
                <div class="panel-body">
                    ${C().items.map((i) => { const p = producto(i.productoId); return `<div class="linea"><span class="small fw-bold">${esc(p.nombre)} <span class="badge text-bg-light">x${i.cantidad}</span></span><strong style="color:var(--ma-purple)">${clp(i.precioUnitario * i.cantidad)}</strong></div>`; }).join('')}
                    <hr>
                    <div class="linea"><span>Neto</span><strong>${clp(r.neto)}</strong></div>
                    ${r.cupon > 0 ? `<div class="linea ahorro"><span>Cupón ${esc(C().cuponAplicado)}</span><strong>− ${clp(r.cupon)}</strong></div>` : ''}
                    <div class="linea"><span>IVA (19%)</span><strong>${clp(r.iva)}</strong></div>
                    <div class="linea"><span>Envío</span><span class="envio">Por coordinar</span></div>
                    <div class="linea total"><span>Total</span><strong>${clp(r.total)}</strong></div>
                    <div id="errorCheckout" class="alerta-magica mt-2" role="alert" hidden></div>
                    <button type="submit" class="btn btn-magico w-100 mt-3 py-2 fs-5"><i class="bi bi-bag-check" aria-hidden="true"></i> Confirmar Pedido</button>
                    <a class="btn btn-outline-magico w-100 mt-2" href="#/caldero"><i class="bi bi-arrow-left" aria-hidden="true"></i> Volver al carrito</a>
                    <p class="small text-muted mt-3 mb-0"><i class="bi bi-shield-lock" aria-hidden="true"></i> Demo: no se procesa ningún pago ni se envían tus datos.</p>
                </div>
            </aside>
        </form>`;
    }

    function tablaItemsPedido(p) {
        return `<table class="tabla-recibo">
            <caption class="visually-hidden">Productos del pedido #${p.id}</caption>
            <thead><tr><th scope="col">Producto</th><th scope="col" class="text-center">Cant.</th><th scope="col" class="num">Precio Unit.</th><th scope="col" class="num">Subtotal</th></tr></thead>
            <tbody>${p.items.map((i) => `<tr><td><div class="item-prod"><img src="${img(i.imagenProducto)}" alt="" loading="lazy"><strong>${esc(i.nombreProducto)}</strong></div></td>
                <td class="text-center fw-bold">${i.cantidad}</td>
                <td class="num precio-celda">${i.precioOriginal ? `<span class="precio-tachado d-block">${clp(i.precioOriginal)}</span>` : ''}${clp(i.precioUnitario)}</td>
                <td class="num precio-celda">${clp(i.precioUnitario * i.cantidad)}</td></tr>`).join('')}</tbody>
            <tfoot>
                ${p.montoAhorroProductos ? `<tr><td colspan="3" class="num text-muted">Ahorro por descuentos automáticos:</td><td class="num" style="color:#b0126f">− ${clp(p.montoAhorroProductos)}</td></tr>` : ''}
                ${p.montoDescuentoCupon ? `<tr><td colspan="3" class="num text-muted">Cupón ${esc(p.cuponAplicado)}:</td><td class="num" style="color:#b0126f">− ${clp(p.montoDescuentoCupon)}</td></tr>` : ''}
                <tr><td colspan="3" class="num text-muted fw-bold">Neto (tras descuentos):</td><td class="num precio-celda">${clp(p.subtotal)}</td></tr>
                <tr><td colspan="3" class="num text-muted fw-bold">IVA (19%):</td><td class="num precio-celda">${clp(p.iva)}</td></tr>
                <tr><td colspan="3" class="num fw-bold fs-5">Total:</td><td class="num precio-celda fs-5">${clp(p.total)}</td></tr>
            </tfoot></table>`;
    }
    function datosEntrega(p) {
        return `<div class="row dato-entrega">
            <div class="col-sm-6"><small>Nombre de contacto</small><strong>${esc(p.nombreContacto)}</strong></div>
            <div class="col-sm-6"><small>Email</small><strong>${esc(p.emailContacto)}</strong></div>
            <div class="col-sm-6"><small>Teléfono</small><strong>${esc(p.telefonoContacto || '—')}</strong></div>
            <div class="col-sm-6"><small>Fecha</small><strong>${fechaLarga(p.fecha)}</strong></div>
            <div class="col-12"><small>Dirección de entrega</small><strong>${esc(p.direccionEntrega)}</strong></div>
            ${p.notasPedido ? `<div class="col-12"><small>Notas</small><strong>${esc(p.notasPedido)}</strong></div>` : ''}
        </div>`;
    }
    function lineaTiempo(p) {
        if (!FLUJO_ESTADOS.includes(p.estado)) return '';
        const idx = FLUJO_ESTADOS.indexOf(p.estado);
        return `<div class="linea-tiempo" aria-label="Progreso: ${estadoLabel(p.estado)}">${FLUJO_ESTADOS.map((e, i) => `<span class="${i <= idx ? 'on' : ''}" title="${estadoLabel(e)}"></span>`).join('')}</div>`;
    }

    function vistaConfirmacion(id) {
        const p = pedidoPorId(id);
        if (!p || (!esDueno(p) && !esAdmin())) return vistaSinPermiso();
        return `<div class="confirmacion-hero">
                <div class="check-magico" aria-hidden="true"><i class="bi bi-check-lg"></i></div>
                <h1 class="mt-3 fw-bold" style="font-size:clamp(1.6rem,4vw,2.4rem)">¡Pedido #${p.id} manifestado con éxito!</h1>
                <p class="text-muted mb-1">Gracias, ${esc(p.nombreContacto.split(' ')[0])}. Tu pedido quedó en estado <span class="estado-pedido-badge ${p.estado}">${estadoLabel(p.estado)}</span></p>
                <p class="small text-muted">El stock de cada producto ya se descontó y el caldero quedó como COMPLETADO.</p>
            </div>
            <div class="recibo">
                <div class="d-flex flex-column gap-3">
                    <section class="panel"><h2 class="panel-header h6 mb-0"><i class="bi bi-bag-heart" aria-hidden="true"></i> Resumen del Pedido</h2>
                        <div class="table-responsive">${tablaItemsPedido(p)}</div></section>
                    <section class="panel"><h2 class="panel-header h6 mb-0"><i class="bi bi-geo-alt" aria-hidden="true"></i> Datos de Entrega</h2>
                        <div class="panel-body">${datosEntrega(p)}</div></section>
                </div>
                <aside class="panel panel-body que-sigue sticky-col">
                    <i class="bi bi-clock-history" aria-hidden="true"></i>
                    <h2 class="h5 fw-bold mt-2">¿Qué sigue?</h2>
                    <p class="small text-muted">En la app real, <code>EmailServiceImpl</code> te envía un correo con el detalle y la coordinación de pago y envío. Aquí no se envía nada.</p>
                    <a class="btn btn-magico w-100 mb-2" href="#/mis-pedidos"><i class="bi bi-bag-heart" aria-hidden="true"></i> Ver mis pedidos</a>
                    <a class="btn btn-magico w-100" href="#/catalogo"><i class="bi bi-stars" aria-hidden="true"></i> Seguir comprando</a>
                    ${esAdmin() ? '' : `<button type="button" class="btn btn-outline-magico w-100 mt-2" data-action="rol" data-rol="ADMIN"><i class="bi bi-shield-check" aria-hidden="true"></i> Verlo como administradora</button>`}
                </aside>
            </div>`;
    }

    function tarjetaPedido(p) {
        const nombres = p.items.slice(0, 2).map((i) => `${esc(i.nombreProducto)} x${i.cantidad}`).join('<br>');
        return `<article class="panel pedido-card">
            <div class="num"><small>Pedido</small><strong>#${p.id}</strong><small>${fechaCorta(p.fecha)}</small></div>
            <div class="prods"><strong class="small">Productos:</strong><br>${nombres}${p.items.length > 2 ? `<br><small class="text-muted">+ ${p.items.length - 2} más...</small>` : ''}</div>
            <div class="tot"><small class="text-muted">Total</small><strong>${clp(p.total)}</strong></div>
            <div class="estado text-center"><span class="estado-pedido-badge ${p.estado}">${estadoLabel(p.estado)}</span>${lineaTiempo(p)}</div>
            <div class="acciones">
                <button type="button" class="btn btn-magico btn-sm" data-action="ver-pedido" data-id="${p.id}"><i class="bi bi-eye" aria-hidden="true"></i> Ver</button>
                ${puedeCancelar(p) ? `<button type="button" class="btn btn-outline-peligro btn-sm" data-action="cancelar-pedido" data-id="${p.id}"><i class="bi bi-x-circle" aria-hidden="true"></i> Cancelar</button>` : ''}
                ${p.estado === 'ENVIADO' ? `<button type="button" class="btn btn-outline-magico btn-sm" data-action="devolucion" data-id="${p.id}"><i class="bi bi-arrow-return-left" aria-hidden="true"></i> Solicitar devolución</button>` : ''}
            </div></article>`;
    }
    function vistaMisPedidos() {
        const cab = `<div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">🛍️</span>Mis Pedidos</h1><p>Historial completo de tus compras mágicas</p></div>`;
        if (!autenticado()) {
            const locales = S.pedidos.filter((p) => p.local).sort((a, b) => b.id - a.id);
            return cab + `<div class="lista-pedidos">
                <div class="panel panel-body text-center">
                    <p class="mb-2"><i class="bi bi-lock-fill" aria-hidden="true" style="color:var(--ma-purple)"></i> En la app real <code>/mis-pedidos/**</code> exige <strong>ROLE_CLIENT</strong> o <strong>ROLE_ADMIN</strong>.</p>
                    <div class="d-flex flex-wrap justify-content-center gap-2">
                        <button type="button" class="btn btn-magico" data-action="abrir-auth" data-tab="login">Iniciar sesión</button>
                        <button type="button" class="btn btn-outline-magico" data-action="rol" data-rol="CLIENT">Entrar como cliente demo</button>
                    </div>
                </div>
                ${locales.length ? `<h2 class="h5 fw-bold mt-2 mb-0 text-center" style="color:var(--ma-purple)">Pedidos como invitado/a en este navegador</h2>
                    <p class="small text-center text-muted mb-0">La vitrina los recuerda para que puedas seguirlos; en producción llegarían por correo.</p>
                    ${locales.map(tarjetaPedido).join('')}` : ''}
            </div>`;
        }
        const mios = S.pedidos.filter((p) => p.clienteEmail === S.usuario.email).sort((a, b) => b.id - a.id);
        if (!mios.length) {
            return cab + `<div class="panel vacio" style="max-width:1100px;margin:0 auto"><i class="bi bi-bag-x" aria-hidden="true"></i><h2>Aún no has realizado pedidos</h2>
                <a class="btn btn-magico mt-2" href="#/catalogo">✨ Explorar el Catálogo</a></div>`;
        }
        return cab + `<div class="lista-pedidos">${mios.map(tarjetaPedido).join('')}</div>`;
    }
    function abrirPedido(id) {
        const p = pedidoPorId(id); if (!p) return;
        $('#pedidoTitulo').innerHTML = `Pedido #${p.id} <span class="estado-pedido-badge ${p.estado} ms-2">${estadoLabel(p.estado)}</span>`;
        $('#pedidoBody').innerHTML = `${lineaTiempo(p)}<div class="table-responsive">${tablaItemsPedido(p)}</div><hr>${datosEntrega(p)}`;
        bootstrap.Modal.getOrCreateInstance($('#modalPedido')).show();
    }

    function vistaSinPermiso() {
        return `<div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">🛡️</span>Acceso denegado</h1><p>Esta sección requiere ROLE_ADMIN (SecurityConfig).</p></div>
            <div class="panel vacio" style="max-width:720px;margin:0 auto"><i class="bi bi-shield-lock" aria-hidden="true"></i><h2>Solo la administradora puede entrar aquí</h2>
            <button type="button" class="btn btn-magico mt-2" data-action="rol" data-rol="ADMIN"><i class="bi bi-shield-check" aria-hidden="true"></i> Cambiar a Administradora</button></div>`;
    }

    // ---------------------- Panel de administración ----------------------
    function vistaAdmin(tab) {
        const tabs = [['resumen', 'bi-speedometer2', 'Resumen'], ['productos', 'bi-bag-heart', 'Productos'], ['pedidos', 'bi-bag-check', 'Pedidos'], ['descuentos', 'bi-percent', 'Descuentos'], ['cupones', 'bi-ticket-perforated', 'Cupones']];
        const cuerpo = { productos: adminProductos, pedidos: adminPedidos, descuentos: adminDescuentos, cupones: adminCupones }[tab] || adminResumen;
        return `<div class="titulo-vista"><h1><span class="emoji" aria-hidden="true">📦</span>Gestión Mágica</h1><p>Administra el inventario y los pedidos del reino</p></div>
            <div class="admin-wrap"><nav class="admin-tabs" aria-label="Secciones de administración">
            ${tabs.map(([id, ic, t]) => `<a href="#/admin/${id}" class="${tab === id ? 'active' : ''}" ${tab === id ? 'aria-current="page"' : ''}><i class="bi ${ic}" aria-hidden="true"></i> ${t}</a>`).join('')}
            </nav>${cuerpo()}</div>`;
    }
    function metricas() {
        const validos = S.pedidos.filter((p) => !['CANCELADO', 'DEVOLUCION_REALIZADA'].includes(p.estado));
        const ventas = validos.reduce((a, p) => a + p.total, 0);
        const porCat = CATEGORIAS.map((c) => ({ c, v: validos.reduce((a, p) => a + p.items.reduce((b, i) => {
            const prod = producto(i.productoId); const sub = prod ? subcat(prod.sub) : null;
            return b + (sub && sub.cat === c.id ? i.precioUnitario * i.cantidad : 0);
        }, 0), 0) }));
        return { ventas, validos, porCat, bajo: S.productos.filter((p) => p.stock <= STOCK_BAJO).sort((a, b) => a.stock - b.stock) };
    }
    function adminResumen() {
        const m = metricas();
        const pendientes = S.pedidos.filter((p) => p.estado === 'PENDIENTE').length;
        const maxCat = Math.max(1, ...m.porCat.map((x) => x.v));
        const agotados = S.productos.filter((p) => p.stock <= 0).length;
        return `<div class="metricas">
                <div class="panel metrica"><i class="bi bi-cash-coin icono" aria-hidden="true"></i><small>Ventas</small><strong>${clp(m.ventas)}</strong><span>${m.validos.length} pedidos válidos (IVA incl.)</span></div>
                <div class="panel metrica"><i class="bi bi-bag-check icono" aria-hidden="true"></i><small>Pedidos</small><strong>${S.pedidos.length}</strong><span>${pendientes} pendiente${pendientes === 1 ? '' : 's'} por confirmar</span></div>
                <div class="panel metrica"><i class="bi bi-gem icono" aria-hidden="true"></i><small>Productos</small><strong>${S.productos.length}</strong><span>${agotados} agotado${agotados === 1 ? '' : 's'} (ocultos al público)</span></div>
                <div class="panel metrica alerta"><i class="bi bi-exclamation-triangle icono" aria-hidden="true"></i><small>Bajo stock</small><strong>${m.bajo.length}</strong><span>con ${STOCK_BAJO} unidades o menos</span></div>
            </div>
            <div class="row g-3 mt-1">
                <div class="col-lg-6"><section class="panel h-100"><h2 class="panel-header h6 mb-0"><i class="bi bi-bar-chart" aria-hidden="true"></i> Ventas netas por categoría</h2>
                    <div class="panel-body barras">${m.porCat.map((x) => `<div class="barra-fila"><span>${esc(x.c.nombre)}</span><div class="pista" role="img" aria-label="${esc(x.c.nombre)}: ${clp(x.v)}"><div class="relleno" data-w="${(x.v / maxCat * 100).toFixed(1)}%"></div></div><span class="valor">${clp(x.v)}</span></div>`).join('')}
                    <p class="small text-muted mb-0 mt-1">Suma de ítems (neto, antes de IVA) de pedidos no cancelados.</p></div></section></div>
                <div class="col-lg-6"><section class="panel h-100"><h2 class="panel-header h6 mb-0"><i class="bi bi-exclamation-diamond" aria-hidden="true"></i> Productos con bajo stock</h2>
                    <div class="panel-body p-0"><div class="table-responsive"><table class="tabla-admin"><caption class="visually-hidden">Productos con bajo stock</caption><tbody>
                    ${m.bajo.length ? m.bajo.slice(0, 6).map((p) => `<tr data-fila="${p.id}"><td><img src="${img(p.imagen)}" alt=""></td><td class="fw-bold">${esc(p.nombre)}<br><small class="text-muted">${esc(subcat(p.sub).nombre)}</small></td><td class="text-end">${editorStock(p)}</td></tr>`).join('') :
                    '<tr><td class="text-center text-muted p-4">Todo el inventario está sano ✨</td></tr>'}
                    ${m.bajo.length > 6 ? `<tr><td colspan="3" class="text-center"><a href="#/admin/productos" class="link-admin">y ${m.bajo.length - 6} más · ver inventario completo</a></td></tr>` : ''}
                    </tbody></table></div></div></section></div>
            </div>
            <section class="panel mt-3"><h2 class="panel-header h6 mb-0"><i class="bi bi-clock-history" aria-hidden="true"></i> Últimos pedidos</h2>
                <div class="table-responsive">${tablaPedidosAdmin(S.pedidos.slice().sort((a, b) => b.id - a.id).slice(0, 4))}</div>
                <div class="panel-body pt-2 text-end"><a href="#/admin/pedidos" class="btn btn-outline-magico btn-sm">Ver todos los pedidos</a></div></section>`;
    }
    function editorStock(p) {
        return `<div class="stock-editor" role="group" aria-label="Stock de ${esc(p.nombre)}">
            <button type="button" class="btn-cant" data-action="stock-delta" data-id="${p.id}" data-delta="-1" aria-label="Restar stock" ${p.stock <= 0 ? 'disabled' : ''}>−</button>
            <input type="number" min="0" step="1" value="${p.stock}" data-action="stock-input" data-id="${p.id}" class="${p.stock <= 0 ? 'cero' : p.stock <= STOCK_BAJO ? 'bajo' : ''}" aria-label="Stock de ${esc(p.nombre)}">
            <button type="button" class="btn-cant" data-action="stock-delta" data-id="${p.id}" data-delta="1" aria-label="Sumar stock">+</button></div>`;
    }
    function adminProductos() {
        const q = ui.adminProductos.q.trim().toLowerCase();
        const lista = S.productos.filter((p) => !q || p.nombre.toLowerCase().includes(q)).sort((a, b) => b.id - a.id);
        return `<div class="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
                <div class="flex-grow-1" style="max-width:420px"><label class="visually-hidden" for="buscarAdmin">Buscar producto</label>
                <input id="buscarAdmin" class="form-control" type="search" placeholder="Buscar producto por nombre..." value="${esc(ui.adminProductos.q)}" style="border-radius:30px"></div>
                <button type="button" class="btn btn-magico" data-action="nuevo-producto"><i class="bi bi-plus-circle-fill" aria-hidden="true"></i> Nuevo producto</button>
            </div>
            <section class="panel"><div class="table-responsive"><table class="tabla-admin">
                <caption class="visually-hidden">Inventario de productos</caption>
                <thead><tr><th scope="col">ID</th><th scope="col">Imagen</th><th scope="col">Nombre</th><th scope="col">Categoría</th><th scope="col" class="text-end">Precio</th><th scope="col" class="text-center">Stock</th><th scope="col" class="text-center">Acciones</th></tr></thead>
                <tbody>${lista.map((p) => `<tr data-fila="${p.id}">
                    <td class="fw-bold">#${p.id}</td><td><img src="${img(p.imagen)}" alt="" loading="lazy"></td>
                    <td class="fw-bold" style="min-width:180px">${esc(p.nombre)}</td>
                    <td style="min-width:140px"><span class="badge rounded-pill" style="background:var(--ma-turq)">${esc(catDeProducto(p).nombre)}</span><br><small>${esc(subcat(p.sub).nombre)}</small></td>
                    <td class="text-end precio-celda">${clp(p.precio)}</td>
                    <td class="text-center">${editorStock(p)}</td>
                    <td class="text-center text-nowrap"><button type="button" class="btn-icono" data-action="editar-producto" data-id="${p.id}" aria-label="Editar ${esc(p.nombre)}"><i class="bi bi-pencil-square" aria-hidden="true"></i></button>
                        <button type="button" class="btn-icono peligro" data-action="eliminar-producto" data-id="${p.id}" aria-label="Eliminar ${esc(p.nombre)}"><i class="bi bi-trash3" aria-hidden="true"></i></button></td>
                </tr>`).join('') || '<tr><td colspan="7" class="text-center text-muted p-4">Sin resultados</td></tr>'}</tbody></table></div>
                <p class="small text-muted px-3 py-2 mb-0">Mostrando ${lista.length} de ${S.productos.length} · El stock se guarda al instante y se refleja en el catálogo público.</p></section>`;
    }
    function tablaPedidosAdmin(lista) {
        if (!lista.length) return `<div class="vacio"><i class="bi bi-inbox" aria-hidden="true"></i><h2>No hay pedidos que coincidan con los filtros aplicados</h2><button type="button" class="btn btn-magico btn-sm" data-action="filtro-estado" data-estado="TODOS"><i class="bi bi-arrow-repeat" aria-hidden="true"></i> Ver todos</button></div>`;
        return `<table class="tabla-admin"><caption class="visually-hidden">Pedidos</caption>
            <thead><tr><th scope="col">#</th><th scope="col">Cliente</th><th scope="col">Email</th><th scope="col" class="text-center">Ítems</th><th scope="col" class="text-end">Monto</th><th scope="col">Fecha</th><th scope="col">Estado</th><th scope="col">Cambiar estado</th></tr></thead>
            <tbody>${lista.map((p) => `<tr data-fila-pedido="${p.id}">
                <td><button type="button" class="link-admin" data-action="ver-pedido" data-id="${p.id}">#${p.id}</button></td>
                <td class="fw-bold text-nowrap">${esc(p.nombreContacto)}${p.clienteEmail ? '' : '<br><small class="text-muted fw-normal">invitado</small>'}</td>
                <td class="small">${esc(p.emailContacto)}</td>
                <td class="text-center">${p.items.reduce((a, i) => a + i.cantidad, 0)}</td>
                <td class="text-end precio-celda">${clp(p.total)}</td>
                <td class="small text-nowrap">${fechaCorta(p.fecha)}</td>
                <td><span class="estado-pedido-badge ${p.estado}">${estadoLabel(p.estado)}</span></td>
                <td><label class="visually-hidden" for="estado-${p.id}">Estado del pedido ${p.id}</label>
                    <select class="form-select form-select-sm select-estado" id="estado-${p.id}" data-action="cambiar-estado" data-id="${p.id}">
                    ${ESTADOS.map((e) => `<option value="${e}" ${e === p.estado ? 'selected' : ''}>${estadoLabel(e)}</option>`).join('')}</select></td>
            </tr>`).join('')}</tbody></table>`;
    }
    function adminPedidos() {
        const f = ui.adminPedidos;
        const lista = S.pedidos.filter((p) => (f.estado === 'TODOS' || p.estado === f.estado) && (!f.email || p.emailContacto.toLowerCase().includes(f.email.toLowerCase())))
            .sort((a, b) => b.fecha.localeCompare(a.fecha));
        return `<section class="panel panel-body mb-3">
                <form id="formFiltroPedidos" class="d-flex flex-wrap gap-2 align-items-end" role="search">
                    <div class="flex-grow-1"><label class="label-magico" for="emailBuscar">Buscar por email</label>
                        <input id="emailBuscar" type="search" class="form-control" placeholder="cliente@email.com" value="${esc(f.email)}"></div>
                    <button type="submit" class="btn btn-magico btn-sm"><i class="bi bi-search" aria-hidden="true"></i> Filtrar</button>
                    <button type="button" class="btn btn-outline-magico btn-sm" data-action="filtro-estado" data-estado="TODOS" data-limpiar="1"><i class="bi bi-x-circle" aria-hidden="true"></i> Limpiar</button>
                </form>
                <div class="d-flex flex-wrap gap-2 align-items-center mt-3" role="group" aria-label="Filtrar por estado">
                    <span class="label-magico me-1">Estado:</span>
                    ${['TODOS'].concat(ESTADOS).map((e) => `<button type="button" class="filtro-estado-btn ${f.estado === e ? 'active' : ''}" data-action="filtro-estado" data-estado="${e}" aria-pressed="${f.estado === e}">${e === 'TODOS' ? 'Todos' : estadoLabel(e)}</button>`).join('')}
                </div></section>
            <p class="small fw-bold"><i class="bi bi-list-ul" aria-hidden="true"></i> ${lista.length} pedido(s) encontrado(s)</p>
            <section class="panel"><div class="table-responsive">${tablaPedidosAdmin(lista)}</div></section>
            <p class="small text-muted mt-2">Como en <code>PedidoServiceImpl.actualizarEstado()</code>, la administradora puede fijar cualquier estado; el cliente solo puede cancelar (antes del envío, restaurando stock) o pedir devolución (desde ENVIADO).</p>`;
    }
    const alcanceTexto = (d) => ({ GLOBAL: 'Global (todos los productos)', CATEGORIA: 'Por Categoría', SUBCATEGORIA: 'Por Subcategoría', PRODUCTO: 'Producto Específico' }[d.alcance]);
    const objetivoTexto = (d) => d.alcance === 'GLOBAL' ? 'Todo el catálogo' : d.alcance === 'CATEGORIA' ? categoria(d.objetivo).nombre : d.alcance === 'SUBCATEGORIA' ? subcat(d.objetivo).nombre : (producto(d.objetivo) || { nombre: '(eliminado)' }).nombre;
    function adminDescuentos() {
        return `<div class="alerta-info mb-3"><i class="bi bi-magic" aria-hidden="true"></i> <strong>Descuentos automáticos:</strong> se aplican al añadir al caldero. Si varios aplican a un producto (producto, subcategoría, categoría o global), gana el que genera <strong>mayor ahorro</strong>. Activa o desactiva uno y mira el catálogo.</div>
            <section class="panel"><div class="table-responsive"><table class="tabla-admin"><caption class="visually-hidden">Descuentos automáticos</caption>
            <thead><tr><th scope="col">Nombre</th><th scope="col">Tipo</th><th scope="col">Valor</th><th scope="col">Alcance</th><th scope="col">Objetivo</th><th scope="col">Vigencia</th><th scope="col">Estado</th><th scope="col">Activo</th></tr></thead>
            <tbody>${S.descuentos.map((d) => `<tr><td class="fw-bold">${esc(d.nombre)}</td><td>${d.tipo === 'PORCENTAJE' ? 'Porcentaje (%)' : 'Monto Fijo ($)'}</td>
                <td class="fw-bold" style="color:var(--ma-purple)">${d.tipo === 'PORCENTAJE' ? d.valor + '%' : clp(d.valor)}</td><td class="small">${alcanceTexto(d)}</td><td>${esc(objetivoTexto(d))}</td>
                <td class="small text-nowrap">${fechaCorta(d.inicio + 'T12:00:00')} – ${fechaCorta(d.fin + 'T12:00:00')}</td>
                <td>${vigente(d) ? '<span class="estado-pedido-badge ENTREGADO">VIGENTE</span>' : d.activo ? '<span class="estado-pedido-badge PENDIENTE">PROGRAMADO</span>' : '<span class="estado-pedido-badge CANCELADO">INACTIVO</span>'}</td>
                <td><div class="form-check form-switch"><input class="form-check-input" type="checkbox" role="switch" data-action="toggle-descuento" data-id="${d.id}" ${d.activo ? 'checked' : ''} aria-label="Activar ${esc(d.nombre)}"></div></td></tr>`).join('')}
            </tbody></table></div></section>`;
    }
    function adminCupones() {
        return `<div class="alerta-info mb-3"><i class="bi bi-ticket-perforated" aria-hidden="true"></i> <strong>Cupones:</strong> se validan en el caldero (existe, activo, no expirado, bajo el límite de usos y con monto mínimo). Al confirmar un pedido se incrementa <code>usosActuales</code>.</div>
            <section class="panel"><div class="table-responsive"><table class="tabla-admin"><caption class="visually-hidden">Cupones</caption>
            <thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Valor</th><th scope="col" class="text-end">Mínimo</th><th scope="col" class="text-center">Usos</th><th scope="col">Expira</th><th scope="col">Estado</th><th scope="col">Activo</th></tr></thead>
            <tbody>${S.cupones.map((c) => `<tr><td><code class="fw-bold" style="color:var(--ma-purple)">${esc(c.codigo)}</code></td><td>${esc(c.nombre)}</td>
                <td class="fw-bold">${c.tipo === 'PORCENTAJE' ? c.valor + '%' : clp(c.valor)}</td><td class="text-end">${c.montoMinimo ? clp(c.montoMinimo) : '—'}</td>
                <td class="text-center">${c.usosActuales} / ${c.limiteUsos ?? '∞'}</td><td class="small">${c.fechaExpiracion ? fechaCorta(c.fechaExpiracion + 'T12:00:00') : 'Sin fecha'}</td>
                <td>${cuponDisponible(c) ? '<span class="estado-pedido-badge ENTREGADO">DISPONIBLE</span>' : '<span class="estado-pedido-badge CANCELADO">NO DISPONIBLE</span>'}</td>
                <td><div class="form-check form-switch"><input class="form-check-input" type="checkbox" role="switch" data-action="toggle-cupon" data-id="${c.id}" ${c.activo ? 'checked' : ''} aria-label="Activar ${esc(c.codigo)}"></div></td></tr>`).join('')}
            </tbody></table></div></section>`;
    }

    // ---------------------- Formulario de producto ----------------------
    function abrirFormProducto(id) {
        const p = id ? producto(id) : null;
        const subActual = p ? subcat(p.sub) : null;
        const catSel = subActual ? subActual.cat : CATEGORIAS[0].id;
        const galeria = Array.from(new Set(PRODUCTOS_SEMILLA.map((x) => x[5]).concat(S.productos.map((x) => x.imagen)))).sort();
        $('#productoFormTitulo').textContent = p ? `Editar Tesoro #${p.id}` : 'Registrar Nuevo Tesoro Mágico';
        $('#productoFormBody').innerHTML = `<form id="formProducto" novalidate data-id="${p ? p.id : ''}">
            <div class="row g-3">
                <div class="col-md-8 d-flex flex-column gap-3">
                    <div><label class="label-magico" for="fpNombre">Nombre del producto <span class="req">*</span></label>
                        <input class="form-control" id="fpNombre" required maxlength="120" value="${esc(p ? p.nombre : '')}" placeholder="Ej: Báculo Lunar"><div class="invalid-feedback"></div></div>
                    <div><label class="label-magico" for="fpDesc">Descripción</label>
                        <textarea class="form-control" id="fpDesc" rows="3" maxlength="600" placeholder="Describe su magia...">${esc(p ? p.descripcion : '')}</textarea></div>
                    <div class="row g-3">
                        <div class="col-sm-6"><label class="label-magico" for="inputPrecioProducto">Precio (CLP) <span class="req">*</span></label>
                            <input class="form-control" id="inputPrecioProducto" type="number" min="0" step="1" required value="${p ? p.precio : ''}" placeholder="Ej: 19990"><div class="invalid-feedback"></div>
                            <div id="avisoPrecioBajo" class="text-warning small mt-1 fw-bold" ${p && p.precio < 5000 ? '' : 'hidden'}><i class="bi bi-exclamation-triangle me-1" aria-hidden="true"></i>Precio muy bajo — ¿es correcto?</div></div>
                        <div class="col-sm-6"><label class="label-magico" for="fpStock">Stock <span class="req">*</span></label>
                            <input class="form-control" id="fpStock" type="number" min="0" step="1" required value="${p ? p.stock : ''}" placeholder="Ej: 10"><div class="invalid-feedback"></div></div>
                        <div class="col-sm-6"><label class="label-magico" for="fpCat">Categoría</label>
                            <select class="form-select" id="fpCat">${CATEGORIAS.map((c) => `<option value="${c.id}" ${c.id === catSel ? 'selected' : ''}>${esc(c.nombre)}</option>`).join('')}</select></div>
                        <div class="col-sm-6"><label class="label-magico" for="fpSub">Subcategoría <span class="req">*</span></label>
                            <select class="form-select" id="fpSub" required>${opcionesSub(catSel, subActual ? subActual.id : null)}</select><div class="invalid-feedback"></div></div>
                    </div>
                </div>
                <div class="col-md-4 text-center">
                    <label class="label-magico d-block text-start" for="fpImg">Imagen (galería)</label>
                    <select class="form-select mb-2" id="fpImg">${galeria.map((g) => `<option value="${g}" ${p && p.imagen === g ? 'selected' : ''}>${g.replace(/^p-|\.webp$/g, '')}</option>`).join('')}</select>
                    <img id="fpPreview" class="preview-img" src="${img(p ? p.imagen : galeria[0])}" alt="Vista previa">
                    <p class="small text-muted mt-2 mb-0">En la app real también puedes subir un archivo (<code>UploadFileServiceImpl</code>).</p>
                </div>
            </div>
            <div class="d-flex justify-content-end gap-2 mt-4">
                <button type="button" class="btn btn-outline-magico" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-magico"><i class="bi bi-stars" aria-hidden="true"></i> ${p ? 'Guardar cambios' : 'Manifestar producto'}</button>
            </div></form>`;
        bootstrap.Modal.getOrCreateInstance($('#modalProducto')).show();
    }
    const opcionesSub = (cat, sel) => SUBCATEGORIAS.filter((s) => s.cat === Number(cat)).map((s) => `<option value="${s.id}" ${s.id === sel ? 'selected' : ''}>${esc(s.nombre)}</option>`).join('');
    function marcarInvalido(input, mensaje) {
        input.classList.toggle('is-invalid', !!mensaje);
        input.setAttribute('aria-invalid', mensaje ? 'true' : 'false');
        const fb = input.parentElement.querySelector('.invalid-feedback');
        if (fb) fb.textContent = mensaje || '';
        return !mensaje;
    }
    function guardarProducto(form) {
        const nombre = $('#fpNombre'); const precio = $('#inputPrecioProducto'); const stock = $('#fpStock'); const sub = $('#fpSub');
        let ok = true;
        ok = marcarInvalido(nombre, nombre.value.trim() ? '' : 'El nombre es obligatorio') && ok;
        const pv = Number(precio.value);
        ok = marcarInvalido(precio, precio.value === '' ? 'El precio es obligatorio' : pv < 0 ? 'El precio del producto no puede ser una cifra negativa.' : '') && ok;
        const sv = Number(stock.value);
        ok = marcarInvalido(stock, stock.value === '' ? 'El stock es obligatorio' : (!Number.isInteger(sv) || sv < 0) ? 'El stock debe ser un entero mayor o igual a 0' : '') && ok;
        ok = marcarInvalido(sub, sub.value ? '' : 'Elige una subcategoría') && ok;
        if (!ok) { const f = $('.is-invalid', form); if (f) f.focus(); return; }
        const datos = { nombre: nombre.value.trim(), descripcion: $('#fpDesc').value.trim(), precio: Math.round(pv), stock: sv, sub: Number(sub.value), imagen: $('#fpImg').value || 'p-vestido-matrimonio-magical.webp' };
        const id = Number(form.dataset.id);
        let p;
        if (id) { p = producto(id); Object.assign(p, datos); }
        else { p = { id: ++S.seq.producto, ...datos }; S.productos.push(p); }
        recalcularCuponSiAplica();
        guardar();
        bootstrap.Modal.getOrCreateInstance($('#modalProducto')).hide();
        toast(id ? `“${p.nombre}” actualizado` : `“${p.nombre}” se unió al catálogo`, 'ok', 'bi-stars', p.imagen);
        render();
        flashFila(p.id);
    }
    function flashFila(id) {
        const fila = $(`[data-fila="${id}"]`);
        if (fila) { fila.classList.remove('fila-flash'); void fila.offsetWidth; fila.classList.add('fila-flash'); }
    }
    function fijarStock(id, valor) {
        const p = producto(id); if (!p) return;
        const n = Math.max(0, Math.floor(Number(valor) || 0));
        if (n === p.stock) return;
        p.stock = n;
        // Si el caldero tiene más unidades que el nuevo stock, se ajusta (el checkout volvería a validar)
        const item = C().items.find((i) => i.productoId === p.id);
        if (item && item.cantidad > n) { if (n === 0) eliminarItem(p.id); else { item.cantidad = n; recalcularCuponSiAplica(); } }
        guardar();
        render();
        flashFila(p.id);
        toast(`Stock de “${p.nombre}”: ${n} ${n === 0 ? '(se oculta del catálogo público)' : ''}`, n <= STOCK_BAJO ? 'info' : 'ok', 'bi-box-seam');
        const input = $(`input[data-action="stock-input"][data-id="${p.id}"]`); if (input) input.focus({ preventScroll: true });
    }

    // ---------------------- Login / Registro ----------------------
    function abrirAuth(tab = 'login') {
        const maxFecha = sumarDias(0);
        $('#authBody').innerHTML = `
            <div class="auth-tabs" role="tablist">
                <button type="button" role="tab" id="tabLogin" aria-controls="panelLogin" aria-selected="${tab === 'login'}" data-action="auth-tab" data-tab="login">Iniciar sesión</button>
                <button type="button" role="tab" id="tabRegistro" aria-controls="panelRegistro" aria-selected="${tab === 'registro'}" data-action="auth-tab" data-tab="registro">Crear cuenta</button>
            </div>
            <p class="alerta-info"><i class="bi bi-shield-lock" aria-hidden="true"></i> Simulación: se acepta cualquier dato válido y <strong>nada sale de tu navegador</strong>. Las contraseñas no se guardan.</p>
            <div id="panelLogin" role="tabpanel" aria-labelledby="tabLogin" ${tab === 'login' ? '' : 'hidden'}>
                <div class="row g-2 mb-3">
                    <div class="col-sm-6"><button type="button" class="cuenta-demo" data-action="cuenta-demo" data-email="usuario@magical.cl" data-pass="Usuario.2026!"><strong>Cliente demo</strong><br><code>usuario@magical.cl</code><br><small>Usuario.2026!</small></button></div>
                    <div class="col-sm-6"><button type="button" class="cuenta-demo" data-action="cuenta-demo" data-email="admin@magical.cl" data-pass="Admin.2026!"><strong>Administración demo</strong><br><code>admin@magical.cl</code><br><small>Admin.2026!</small></button></div>
                </div>
                <form id="formLogin" novalidate>
                    <div class="mb-3"><label class="label-magico" for="loginEmail">Email</label><input class="form-control" id="loginEmail" type="email" autocomplete="username" required><div class="invalid-feedback"></div></div>
                    <div class="mb-3"><label class="label-magico" for="loginPass">Contraseña</label><input class="form-control" id="loginPass" type="password" autocomplete="current-password" required><div class="invalid-feedback"></div></div>
                    <button type="submit" class="btn btn-magico w-100 py-2"><i class="bi bi-box-arrow-in-right" aria-hidden="true"></i> Entrar a la Alianza</button>
                    <p class="small text-muted mt-2 mb-0">Cualquier otro email entra como cliente. En la app real lo valida Spring Security (BCrypt + JWT en cookie).</p>
                </form>
            </div>
            <div id="panelRegistro" role="tabpanel" aria-labelledby="tabRegistro" ${tab === 'registro' ? '' : 'hidden'}>
                <form id="formRegistro" novalidate class="row g-3">
                    <div class="col-md-6"><label class="label-magico" for="rEmail">Email de inicio de sesión <span class="req">*</span></label><input class="form-control" id="rEmail" type="email" autocomplete="email" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rPass">Contraseña sagrada <span class="req">*</span></label><input class="form-control" id="rPass" type="password" autocomplete="new-password" required aria-describedby="rPassAyuda"><div class="invalid-feedback"></div>
                        <small id="rPassAyuda" class="text-muted">Mínimo 8 caracteres, una mayúscula y un carácter especial.</small></div>
                    <div class="col-md-6"><label class="label-magico" for="rRut">RUT (sin puntos, con guion) <span class="req">*</span></label><input class="form-control" id="rRut" required placeholder="12345678-9"><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rTel">Teléfono</label><input class="form-control" id="rTel" type="tel" autocomplete="tel" placeholder="+569..."></div>
                    <div class="col-md-6"><label class="label-magico" for="rNombre">Nombre <span class="req">*</span></label><input class="form-control" id="rNombre" autocomplete="given-name" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rApellido">Apellido <span class="req">*</span></label><input class="form-control" id="rApellido" autocomplete="family-name" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rFecha">Fecha de nacimiento <span class="req">*</span></label><input class="form-control" id="rFecha" type="date" max="${maxFecha}" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rPais">País <span class="req">*</span></label><input class="form-control" id="rPais" value="Chile" autocomplete="country-name" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rRegion">Región / Estado <span class="req">*</span></label><input class="form-control" id="rRegion" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-6"><label class="label-magico" for="rCiudad">Ciudad <span class="req">*</span></label><input class="form-control" id="rCiudad" autocomplete="address-level2" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-8"><label class="label-magico" for="rDireccion">Dirección (calle y número) <span class="req">*</span></label><input class="form-control" id="rDireccion" autocomplete="street-address" required><div class="invalid-feedback"></div></div>
                    <div class="col-md-4"><label class="label-magico" for="rCP">Código postal</label><input class="form-control" id="rCP" autocomplete="postal-code"></div>
                    <div class="col-12"><div id="errorRegistro" class="alerta-magica" role="alert" hidden></div>
                        <button type="submit" class="btn btn-magico w-100 py-2"><i class="bi bi-stars" aria-hidden="true"></i> Unirme a la Alianza</button></div>
                </form>
            </div>`;
        bootstrap.Modal.getOrCreateInstance($('#modalAuth')).show();
    }
    function login(form) {
        const email = $('#loginEmail'); const pass = $('#loginPass');
        let ok = marcarInvalido(email, /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()) ? '' : 'El formato del email no es válido');
        ok = marcarInvalido(pass, pass.value ? '' : 'La contraseña es obligatoria') && ok;
        if (!ok) return;
        const e = email.value.trim().toLowerCase();
        bootstrap.Modal.getOrCreateInstance($('#modalAuth')).hide();
        if (e === ADMIN_DEMO.email) { iniciarSesion('ADMIN', { ...ADMIN_DEMO }); toast('Hola, Administrador · Panel de gestión mágica activo', 'ok', 'bi-shield-check'); }
        else {
            const usuario = e === USUARIO_DEMO.email ? { ...USUARIO_DEMO } : (S.ultimoCliente && S.ultimoCliente.email === e ? S.ultimoCliente : { nombre: e.split('@')[0].replace(/[._-]+/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()), apellido: '', email: e });
            S.ultimoCliente = usuario;
            iniciarSesion('CLIENT', usuario);
            toast(`Hola, ${usuario.nombre} · tu caldero se conserva (fusión de carrito invitado)`, 'ok', 'bi-person-heart');
        }
        render();
    }
    // UsuarioServiceImpl.registrarUsuario() + RegistroDTO
    function registrar(form) {
        const v = (id) => $('#' + id).value.trim();
        const reglas = [
            ['rEmail', !v('rEmail') ? 'El email de acceso es obligatorio' : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v('rEmail')) ? 'El formato del email no es válido' : ''],
            ['rPass', !$('#rPass').value ? 'La contraseña es obligatoria para registrarse.' : !REGEX_PASSWORD.test($('#rPass').value) ? 'La contraseña debe tener mínimo 8 caracteres, una mayúscula y un carácter especial.' : ''],
            ['rRut', !v('rRut') ? 'El RUT es obligatorio' : limpiarRut(v('rRut')).length < 7 ? 'Revisa el RUT ingresado' : ''],
            ['rNombre', v('rNombre') ? '' : 'El nombre es obligatorio'],
            ['rApellido', v('rApellido') ? '' : 'El apellido es obligatorio'],
            ['rFecha', !v('rFecha') ? 'La fecha de nacimiento es obligatoria' : edad(v('rFecha')) < 18 ? 'La guardiana debe ser mayor de 18 años para unirse a la Alianza.' : edad(v('rFecha')) > 105 ? 'La edad máxima permitida es 105 años.' : ''],
            ['rPais', v('rPais') ? '' : 'El país es obligatorio'],
            ['rRegion', v('rRegion') ? '' : 'La región o estado es obligatoria'],
            ['rCiudad', v('rCiudad') ? '' : 'La ciudad es obligatoria'],
            ['rDireccion', v('rDireccion') ? '' : 'La dirección de morada es obligatoria']
        ];
        let ok = true;
        reglas.forEach(([id, msg]) => { ok = marcarInvalido($('#' + id), msg) && ok; });
        const err = $('#errorRegistro');
        err.hidden = true;
        if (!ok) { const f = $('.is-invalid', form); if (f) f.focus(); return; }
        const email = v('rEmail').toLowerCase(); const rut = limpiarRut(v('rRut'));
        if ([ADMIN_DEMO.email, USUARIO_DEMO.email].includes(email)) { err.textContent = `El email ${email} ya tiene una cuenta activa.`; err.hidden = false; return; }
        if ([ADMIN_DEMO.rut, USUARIO_DEMO.rut].includes(rut)) { err.textContent = 'Este RUT ya tiene una cuenta de usuario vinculada.'; err.hidden = false; return; }
        const usuario = { nombre: v('rNombre'), apellido: v('rApellido'), email, rut, telefono: v('rTel'), calle: v('rDireccion'), ciudad: v('rCiudad'), estadoRegion: v('rRegion'), pais: v('rPais'), codigoPostal: v('rCP') };
        S.ultimoCliente = usuario;
        bootstrap.Modal.getOrCreateInstance($('#modalAuth')).hide();
        iniciarSesion('CLIENT', usuario);
        toast(`¡Bienvenida/o a la Alianza, ${usuario.nombre}! Cuenta creada con ROLE_CLIENT`, 'ok', 'bi-stars');
        render();
    }

    // ---------------------- Cómo está hecho ----------------------
    function abrirComo() {
        const link = (ruta) => `<a href="${REPO}${ruta}" target="_blank" rel="noopener">${ruta.split('/').pop()}</a>`;
        const J = 'java/com/magicalAliance/'; const T = 'resources/templates/';
        const filas = [
            ['Catálogo, filtros y paginación', 'Búsqueda por nombre, categoría/subcategoría, 5 órdenes, 12 por página con ventana ±2; sin stock oculto al público.',
                [J + 'controller/producto/ProductoController.java', J + 'service/producto/ProductoServiceImpl.java', J + 'repository/producto/ProductoRepository.java', J + 'util/PaginacionHelper.java', J + 'entity/producto/Producto.java', T + 'public/productos.html']],
            ['Detalle de producto', 'Badges de categoría, stock, selector de cantidad y relacionados por subcategoría/categoría.',
                [T + 'public/producto-detalle.html', J + 'entity/producto/Subcategoria.java', J + 'entity/producto/Categoria.java']],
            ['Caldero (carrito)', 'Agregar/sumar con tope de stock, precio con descuento congelado, cupón recalculado o retirado.',
                [J + 'controller/carrito/CarritoController.java', J + 'service/carrito/CarritoServiceImpl.java', J + 'entity/carrito/Carrito.java', J + 'entity/carrito/ItemCarrito.java', T + 'client/carrito/carrito.html']],
            ['Descuentos y cupones', 'Mejor descuento entre producto › subcategoría › categoría › global; cupón: existe, activo, vigente, límite y mínimo.',
                [J + 'service/descuento/DescuentoServiceImpl.java', J + 'entity/descuento/Descuento.java', J + 'entity/descuento/Cupon.java', J + 'controller/AdminDescuentoController.java', T + 'admin/descuentos/cupones-list.html']],
            ['Checkout y confirmación', 'Valida stock de todos los ítems, neto − cupón, IVA 19 %, snapshot de ítems, descuenta stock, suma uso del cupón.',
                [J + 'dto/carrito/CheckoutDTO.java', J + 'service/pedido/PedidoServiceImpl.java', J + 'entity/pedido/Pedido.java', J + 'entity/pedido/ItemPedido.java', T + 'client/carrito/checkout.html', T + 'client/carrito/confirmacion.html']],
            ['Mis pedidos', 'Historial del cliente; cancelar antes del envío (restaura stock) y devolución solo desde ENVIADO.',
                [J + 'controller/pedido/PedidoController.java', J + 'entity/pedido/EstadoPedido.java', T + 'client/pedidos/mis-pedidos.html', T + 'client/pedidos/detalle-pedido.html']],
            ['Panel: productos', 'CRUD de productos con galería de imágenes, aviso de precio bajo (< $5.000) y edición de stock.',
                [J + 'controller/producto/ProductoController.java', J + 'service/img/UploadFileServiceImpl.java', T + 'admin/inventario/producto-form.html']],
            ['Panel: pedidos', 'Filtro por estado y email, cambio de estado con aviso por correo al cliente.',
                [T + 'admin/pedidos/pedidos-list.html', J + 'service/EmailServiceImpl.java']],
            ['Login, registro y roles', 'Spring Security con BCrypt y JWT en cookie; registro con edad 18–105, contraseña robusta y RUT normalizado.',
                [J + 'config/SecurityConfig.java', J + 'filter/JwtFilter.java', J + 'controller/usuario/AuthController.java', J + 'service/usuario/UsuarioServiceImpl.java', J + 'dto/usuario/RegistroDTO.java', J + 'dto/usuario/MayorDeEdadValidator.java']]
        ];
        $('#comoBody').innerHTML = `
            <p>Esta vitrina es una <strong>re-creación front-end</strong> del eCommerce <strong>Magical Alliance</strong>, mi proyecto final del módulo 7 del bootcamp Full Stack Java. GitHub Pages no ejecuta Java, así que las mismas reglas de negocio se portaron a JavaScript y los datos viven en <code>localStorage</code>.</p>
            <div class="stack-chips mb-3"><span>Java 17</span><span>Spring Boot</span><span>Spring Security + JWT</span><span>Spring Data JPA</span><span>MySQL</span><span>Thymeleaf</span><span>Bootstrap 5</span><span>Lombok</span><span>JavaMail</span></div>
            <div class="table-responsive"><table class="tabla-como w-100"><caption class="visually-hidden">Pantallas y clases Java</caption>
                <thead><tr><th scope="col" style="min-width:150px">Pantalla</th><th scope="col" style="min-width:220px">Regla reproducida</th><th scope="col">Código original</th></tr></thead>
                <tbody>${filas.map(([p, r, ls]) => `<tr><th scope="row" class="fw-bold" style="background:none;color:var(--ma-ink);text-transform:none;font-size:.9rem;letter-spacing:0">${p}</th><td>${r}</td><td>${ls.map(link).join(' ')}</td></tr>`).join('')}</tbody>
            </table></div>
            <h3 class="h6 fw-bold mt-3" style="color:var(--ma-purple)">Qué se simplificó en la vitrina</h3>
            <ul class="small mb-3">
                <li>No hay servidor ni base de datos: no se envían correos, no se cifran contraseñas ni se emiten JWT.</li>
                <li>El catálogo combina los productos de las capturas reales con otros armados desde la galería de imágenes del proyecto, con precios y stock de ejemplo; descuentos y cupones también son de ejemplo.</li>
                <li>Usuarios, mensajes de contacto, suscriptores, categorías y subcategorías tienen CRUD en la app real; aquí solo se muestran productos, pedidos, descuentos y cupones.</li>
            </ul>
            <div class="d-flex flex-wrap gap-2">
                <a class="btn btn-magico" href="https://github.com/manecist/Ecommerce-Portafolio-Final-M7" target="_blank" rel="noopener"><i class="bi bi-github" aria-hidden="true"></i> Ver el repositorio</a>
                <a class="btn btn-outline-magico" href="https://manecist.github.io/" target="_blank" rel="noopener"><i class="bi bi-person-badge" aria-hidden="true"></i> Volver al portafolio</a>
            </div>`;
        bootstrap.Modal.getOrCreateInstance($('#modalComo')).show();
    }

    // ------------------------------------------------------------------
    // 9. Eventos (delegación)
    // ------------------------------------------------------------------
    function irCategoria(cat) {
        ui.catalogo = { cat: cat ? Number(cat) : null, sub: null, q: '', orden: ui.catalogo.orden, page: 0 };
        if (location.hash === '#/catalogo') render(); else location.hash = '#/catalogo';
    }
    function cantidadDe(btn) {
        const cont = btn.closest('[data-producto]');
        const input = cont ? $('.input-cant', cont) : null;
        return input ? Math.max(1, Number(input.value) || 1) : 1;
    }

    document.addEventListener('click', (ev) => {
        const t = ev.target.closest('[data-action]');
        if (!t) return;
        const a = t.dataset.action;
        const id = t.dataset.id ? Number(t.dataset.id) : null;
        try {
            switch (a) {
                case 'rol': ev.preventDefault(); bootstrap.Modal.getInstance($('#modalDetalle'))?.hide(); cambiarRol(t.dataset.rol); break;
                case 'como-hecho': abrirComo(); break;
                case 'reiniciar': confirmar('Reiniciar demo', '<p class="mb-0">Se borrarán tus pedidos, cambios de stock y productos creados en esta vitrina, y volverá al estado inicial.</p>', 'Reiniciar', reiniciar); break;
                case 'ir-categoria': ev.preventDefault(); irCategoria(t.dataset.cat); break;
                case 'filtro-cat': { const c = Number(t.dataset.cat); ui.catalogo.cat = ui.catalogo.cat === c ? null : c; ui.catalogo.sub = null; ui.catalogo.page = 0; render(); break; }
                case 'filtro-sub': ui.catalogo.sub = t.dataset.sub ? Number(t.dataset.sub) : null; ui.catalogo.page = 0; render(); break;
                case 'limpiar-filtros': ui.catalogo = { cat: null, sub: null, q: '', orden: 'recom', page: 0 }; render(); break;
                case 'pagina': ui.catalogo.page = Number(t.dataset.page); render(); break;
                case 'detalle': abrirDetalle(id); break;
                case 'cant-card': {
                    const cont = t.closest('[data-producto]'); const input = $('.input-cant', cont);
                    const max = Number(input.max) || 1; const nuevo = Math.min(max, Math.max(1, Number(input.value) + Number(t.dataset.delta)));
                    input.value = nuevo;
                    $$('.btn-cant', cont).forEach((b) => { b.disabled = (b.dataset.delta === '-1' && nuevo <= 1) || (b.dataset.delta === '1' && nuevo >= max); });
                    break;
                }
                case 'agregar': {
                    const cant = cantidadDe(t);
                    const p = producto(id);
                    agregarProducto(id, cant);
                    const cont = t.closest('[data-producto]');
                    volarAlCaldero(cont ? $('img', cont) : null);
                    t.classList.remove('listo'); void t.offsetWidth; t.classList.add('listo');
                    toast(`✨ ${cant > 1 ? cant + ' × ' : ''}${p.nombre} agregado al caldero`, 'ok', 'bi-stars', p.imagen);
                    setTimeout(() => {
                        renderHeader();
                        if ($('#modalDetalle').classList.contains('show')) abrirDetalle(id);
                        const r = ruta().vista; if (r === 'catalogo' || r === 'inicio') {
                            const card = $(`.grid-productos [data-producto="${id}"]`);
                            if (card) { const tmp = document.createElement('div'); tmp.innerHTML = tarjetaProducto(producto(id)); card.replaceWith(tmp.firstElementChild); }
                        }
                    }, 850);
                    break;
                }
                case 'cant-item': {
                    const item = C().items.find((i) => i.productoId === id);
                    const cupon = C().cuponAplicado;
                    actualizarCantidad(id, item.cantidad + Number(t.dataset.delta));
                    render(); avisarCuponRetirado(cupon); bumpCarrito(); break;
                }
                case 'quitar-item': { const p = producto(id); const cupon = C().cuponAplicado; eliminarItem(id); render(); toast(`${p.nombre} salió del caldero`, 'info', 'bi-trash3'); avisarCuponRetirado(cupon); break; }
                case 'vaciar': confirmar('Vaciar caldero', '<p class="mb-0">¿Quieres quitar todos los tesoros de tu caldero?</p>', 'Vaciar', () => { vaciarCarrito(); render(); toast('Tu caldero quedó vacío', 'info', 'bi-trash3'); }); break;
                case 'usar-cupon': { const inp = $('#inputCupon'); if (inp) { inp.value = t.dataset.codigo; $('#formCupon').requestSubmit(); } break; }
                case 'quitar-cupon': quitarCupon(); render(); toast('Cupón retirado', 'info', 'bi-ticket-perforated'); break;
                case 'ver-pedido': abrirPedido(id); break;
                case 'cancelar-pedido': confirmar(`Cancelar pedido #${id}`, '<p class="mb-0">El pedido pasará a <strong>CANCELADO</strong> y el stock de cada producto se restaurará (<code>cancelarPedido</code>).</p>', 'Cancelar pedido', () => {
                    try { cancelarPedido(id); render(); toast(`Pedido #${id} cancelado y stock restaurado`, 'info', 'bi-x-circle'); } catch (e) { toast(e.message, 'error', 'bi-exclamation-triangle'); }
                }); break;
                case 'devolucion': confirmar(`Devolución del pedido #${id}`, '<p class="mb-0">Se solicitará la devolución; la administradora la marcará como realizada.</p>', 'Solicitar', () => {
                    try { solicitarDevolucion(id); render(); toast(`Devolución solicitada para el pedido #${id}`, 'ok', 'bi-arrow-return-left'); } catch (e) { toast(e.message, 'error', 'bi-exclamation-triangle'); }
                }); break;
                case 'abrir-auth': ev.preventDefault(); abrirAuth(t.dataset.tab); break;
                case 'auth-tab': {
                    const login = t.dataset.tab === 'login';
                    $('#panelLogin').hidden = !login; $('#panelRegistro').hidden = login;
                    $('#tabLogin').setAttribute('aria-selected', String(login)); $('#tabRegistro').setAttribute('aria-selected', String(!login));
                    break;
                }
                case 'cuenta-demo': $('#loginEmail').value = t.dataset.email; $('#loginPass').value = t.dataset.pass; $('#formLogin').requestSubmit(); break;
                case 'nuevo-producto': abrirFormProducto(null); break;
                case 'editar-producto': bootstrap.Modal.getInstance($('#modalDetalle'))?.hide(); abrirFormProducto(id); break;
                case 'eliminar-producto': { const p = producto(id); confirmar('Eliminar producto', `<p class="mb-0">¿Eliminar <strong>${esc(p.nombre)}</strong> del catálogo? Los pedidos existentes conservan su snapshot.</p>`, 'Eliminar', () => {
                    S.productos = S.productos.filter((x) => x.id !== id); limpiarCarritoHuerfano(); guardar(); render(); toast(`“${p.nombre}” eliminado`, 'info', 'bi-trash3');
                }); break; }
                case 'stock-delta': fijarStock(id, producto(id).stock + Number(t.dataset.delta)); break;
                case 'filtro-estado': ui.adminPedidos.estado = t.dataset.estado; if (t.dataset.limpiar) ui.adminPedidos.email = ''; render(); break;
                default: break;
            }
        } catch (e) {
            if (e instanceof MagicalBusinessException) toast(e.message, 'error', 'bi-exclamation-triangle');
            else { console.error(e); toast('Algo salió mal en la vitrina', 'error', 'bi-bug'); }
        }
    });
    // CarritoServiceImpl.recalcularCuponSiAplica() quita el cupón si el total ya no llega al mínimo
    function avisarCuponRetirado(codigoPrevio) {
        if (codigoPrevio && !C().cuponAplicado && C().items.length) toast(`El cupón ${codigoPrevio} se retiró: tu caldero ya no cumple sus condiciones`, 'info', 'bi-ticket-perforated');
    }

    document.addEventListener('change', (ev) => {
        const t = ev.target;
        try {
            if (t.matches('[data-action="stock-input"]')) fijarStock(Number(t.dataset.id), t.value);
            else if (t.matches('[data-action="cambiar-estado"]')) {
                const p = pedidoPorId(t.dataset.id); const anterior = p.estado; p.estado = t.value; guardar(); render();
                toast(`Pedido #${p.id}: ${estadoLabel(anterior)} → ${estadoLabel(p.estado)} (en la app real se notifica por correo)`, 'ok', 'bi-envelope-check');
            } else if (t.matches('[data-action="toggle-descuento"]')) {
                const d = S.descuentos.find((x) => x.id === Number(t.dataset.id)); d.activo = t.checked; guardar(); render();
                toast(`Descuento “${d.nombre}” ${d.activo ? 'activado' : 'desactivado'}`, 'info', 'bi-percent');
            } else if (t.matches('[data-action="toggle-cupon"]')) {
                const c = S.cupones.find((x) => x.id === Number(t.dataset.id)); c.activo = t.checked; recalcularCuponSiAplica(); guardar(); render();
                toast(`Cupón ${c.codigo} ${c.activo ? 'activado' : 'desactivado'}`, 'info', 'bi-ticket-perforated');
            } else if (t.id === 'selOrden') { ui.catalogo.orden = t.value; ui.catalogo.page = 0; render(); }
            else if (t.id === 'fpCat') { $('#fpSub').innerHTML = opcionesSub(t.value, null); }
            else if (t.id === 'fpImg') { $('#fpPreview').src = img(t.value); }
        } catch (e) { toast(e.message, 'error', 'bi-exclamation-triangle'); }
    });

    let temporizadorBusqueda = null;
    document.addEventListener('input', (ev) => {
        const t = ev.target;
        if (t.id === 'txtBuscar') {
            clearTimeout(temporizadorBusqueda);
            temporizadorBusqueda = setTimeout(() => {
                ui.catalogo.q = t.value; ui.catalogo.page = 0;
                const pos = t.selectionStart; render();
                const nuevo = $('#txtBuscar'); if (nuevo) { nuevo.focus(); try { nuevo.setSelectionRange(pos, pos); } catch (e) { /* type=search */ } }
            }, 280);
        } else if (t.id === 'buscarAdmin') {
            clearTimeout(temporizadorBusqueda);
            temporizadorBusqueda = setTimeout(() => {
                ui.adminProductos.q = t.value; render();
                const nuevo = $('#buscarAdmin'); if (nuevo) { nuevo.focus(); const l = nuevo.value.length; try { nuevo.setSelectionRange(l, l); } catch (e) { /* nada */ } }
            }, 250);
        } else if (t.id === 'inputPrecioProducto') {
            const v = parseFloat(t.value); $('#avisoPrecioBajo').hidden = !(!isNaN(v) && v >= 0 && v < 5000);
        } else if (t.classList.contains('is-invalid')) {
            marcarInvalido(t, '');
        }
    });

    document.addEventListener('submit', (ev) => {
        const f = ev.target;
        ev.preventDefault();
        try {
            if (f.id === 'formFiltros') { ui.catalogo.q = $('#txtBuscar').value; ui.catalogo.orden = $('#selOrden').value; ui.catalogo.page = 0; render(); }
            else if (f.id === 'formCupon') {
                const codigo = $('#inputCupon').value.trim();
                if (!codigo) { toast('Escribe un código de cupón', 'error', 'bi-ticket-perforated'); return; }
                const c = aplicarCupon(codigo); render();
                toast(`Cupón ${c.codigo} aplicado: ${c.tipo === 'PORCENTAJE' ? c.valor + '% de descuento' : clp(c.valor) + ' de descuento'}`, 'ok', 'bi-ticket-perforated-fill');
            } else if (f.id === 'formCheckout') { enviarCheckout(f); }
            else if (f.id === 'formFiltroPedidos') { ui.adminPedidos.email = $('#emailBuscar').value.trim(); render(); }
            else if (f.id === 'formProducto') guardarProducto(f);
            else if (f.id === 'formLogin') login(f);
            else if (f.id === 'formRegistro') registrar(f);
            else if (f.id === 'formSuscripcion') {
                const inp = $('#emailSuscripcion'); const fb = $('#feedbackSuscripcion'); const e = inp.value.trim().toLowerCase();
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)) { fb.textContent = 'Ingresa un email válido'; return; }
                if (S.suscriptores.includes(e)) { fb.textContent = 'Este email ya está suscrito ✨'; return; }
                S.suscriptores.push(e); guardar(); fb.textContent = ''; inp.value = '';
                toast('¡Bienvenida/o a la Alianza! (suscripción simulada, no se envía ningún correo)', 'ok', 'bi-envelope-heart');
            }
        } catch (e) {
            if (e instanceof MagicalBusinessException) {
                if (f.id === 'formCupon') { $('#inputCupon').classList.add('is-invalid'); }
                toast(e.message, 'error', 'bi-exclamation-triangle');
            } else { console.error(e); toast('Algo salió mal en la vitrina', 'error', 'bi-bug'); }
        }
    });

    function enviarCheckout(f) {
        const v = (id) => $('#' + id).value.trim();
        const reglas = [
            ['nombreContacto', v('nombreContacto') ? '' : 'El nombre de contacto es obligatorio'],
            ['emailContacto', !v('emailContacto') ? 'El email es obligatorio' : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v('emailContacto')) ? 'El formato del email no es válido' : ''],
            ['calle', v('calle') ? '' : 'La calle y número son obligatorios'],
            ['ciudad', v('ciudad') ? '' : 'La ciudad es obligatoria'],
            ['estadoRegion', v('estadoRegion') ? '' : 'La región es obligatoria'],
            ['pais', v('pais') ? '' : 'El país es obligatorio']
        ];
        let ok = true;
        reglas.forEach(([id, msg]) => { ok = marcarInvalido($('#' + id), msg) && ok; });
        if (!ok) { const p = $('.is-invalid', f); if (p) p.focus(); return; }
        try {
            const pedido = crearPedido({
                nombreContacto: v('nombreContacto'), emailContacto: v('emailContacto'), telefonoContacto: v('telefonoContacto'),
                calle: v('calle'), ciudad: v('ciudad'), estadoRegion: v('estadoRegion'), pais: v('pais'), codigoPostal: v('codigoPostal'), notasPedido: v('notasPedido')
            });
            ui.ultimoPedido = pedido.id;
            location.hash = `#/confirmacion/${pedido.id}`;
        } catch (e) {
            const err = $('#errorCheckout'); err.textContent = e.message; err.hidden = false; err.focus?.();
            toast(e.message, 'error', 'bi-exclamation-triangle');
        }
    }

    // Botón "subir" con el báculo (footer.html)
    const btnSubir = $('#btnSubir');
    window.addEventListener('scroll', () => { btnSubir.classList.toggle('visible', window.scrollY > 500); }, { passive: true });
    btnSubir.addEventListener('click', () => { window.scrollTo({ top: 0, behavior: 'smooth' }); $('#titulo').focus({ preventScroll: true }); });

    // Cerrar el menú colapsado al navegar en móvil
    document.addEventListener('click', (ev) => {
        const enlace = ev.target.closest('#navbarNav a[href^="#/"], #navbarNav [data-action="rol"]');
        const menu = $('#navbarNav');
        if (enlace && menu.classList.contains('show') && !enlace.classList.contains('dropdown-toggle')) bootstrap.Collapse.getOrCreateInstance(menu).hide();
    });
    // Limpiar cupón inválido visualmente al escribir
    document.addEventListener('keydown', (ev) => { if (ev.target.id === 'inputCupon') ev.target.classList.remove('is-invalid'); });

    window.addEventListener('hashchange', render);
    // Sincroniza pestañas abiertas en paralelo
    window.addEventListener('storage', (ev) => { if (ev.key === STORAGE_KEY) { S = cargar(); render(); } });

    if (!location.hash) history.replaceState(null, '', '#/inicio');
    render();
})();
