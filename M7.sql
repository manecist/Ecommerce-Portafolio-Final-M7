-- 1. CREAR DATABASE
CREATE DATABASE ecommerce_crud_m6;

USE ecommerce_crud_m6;

-- 2. CREACIÓN DE TABLAS INDEPENDIENTES O MAESTRAS

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255)
);

CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rut VARCHAR(255) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    telefono VARCHAR(255),
    fecha_nacimiento DATE NOT NULL
);

CREATE TABLE categorias (
    id_categoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_categoria VARCHAR(255),
    imagen_banner VARCHAR(255)
);

CREATE TABLE cupones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    fecha_expiracion DATE,
    limite_usos INT,
    monto_minimo DOUBLE NOT NULL DEFAULT 0.0,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    usos_actuales INT NOT NULL DEFAULT 0,
    valor DOUBLE NOT NULL
);

-- 3. TABLAS CON DEPENDENCIAS SIMPLES

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fecha_registro DATE,
    rol_id BIGINT,
    cliente_rut VARCHAR(255),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES roles(id),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_rut) REFERENCES clientes(rut) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE direcciones_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    direccion VARCHAR(255) NOT NULL,
    ciudad VARCHAR(255) NOT NULL,
    estado_region VARCHAR(255),
    pais VARCHAR(255) NOT NULL,
    codigo_postal VARCHAR(255),
    es_principal BOOLEAN DEFAULT FALSE,
    cliente_rut VARCHAR(255),
    CONSTRAINT fk_direccion_cliente FOREIGN KEY (cliente_rut) REFERENCES clientes(rut) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE subcategorias (
    id_subcategoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_subcategoria VARCHAR(255),
    id_categoria BIGINT,
    CONSTRAINT fk_subcat_cat FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
);

CREATE TABLE productos (
    id_producto BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_producto VARCHAR(255) NOT NULL,
    descripcion_producto TEXT,
    precio_producto DOUBLE NOT NULL,
    stock_producto INT NOT NULL,
    imagen_producto VARCHAR(255),
    id_subcategoria BIGINT,
    CONSTRAINT fk_producto_subcat FOREIGN KEY (id_subcategoria) REFERENCES subcategorias(id_subcategoria)
);

-- 4. TABLAS DE PROCESOS (PEDIDOS Y CARRITOS)

CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    nombre_contacto VARCHAR(255),
    email_contacto VARCHAR(255),
    telefono_contacto VARCHAR(255),
    estado VARCHAR(50),
    direccion_entrega TEXT,
    subtotal DOUBLE,
    iva DOUBLE,
    total DOUBLE,
    monto_ahorro_productos DOUBLE,
    monto_descuento_cupon DOUBLE,
    cupon_aplicado VARCHAR(50),
    notas_pedido TEXT,
    fecha_pedido DATETIME,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE items_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT,
    nombre_producto VARCHAR(255) NOT NULL,
    imagen_producto VARCHAR(255),
    cantidad INT NOT NULL,
    precio_unitario DOUBLE NOT NULL,
    precio_original DOUBLE,
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_producto FOREIGN KEY (producto_id) REFERENCES productos(id_producto)
);

CREATE TABLE carritos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    estado VARCHAR(255),
    fecha_creacion DATETIME,
    fecha_actualizacion DATETIME,
    cupon_aplicado VARCHAR(50),
    monto_descuento_cupon DOUBLE,
    CONSTRAINT fk_carrito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE items_carrito (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrito_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DOUBLE NOT NULL,
    precio_original DOUBLE,
    CONSTRAINT fk_item_carrito FOREIGN KEY (carrito_id) REFERENCES carritos(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_cart_prod FOREIGN KEY (producto_id) REFERENCES productos(id_producto)
);

-- 5. TABLAS DE SOPORTE Y MARKETING

CREATE TABLE descuentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor DOUBLE NOT NULL,
    alcance VARCHAR(20) NOT NULL,
    categoria_id BIGINT,
    subcategoria_id BIGINT,
    producto_id BIGINT,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_desc_cat FOREIGN KEY (categoria_id) REFERENCES categorias(id_categoria),
    CONSTRAINT fk_desc_subcat FOREIGN KEY (subcategoria_id) REFERENCES subcategorias(id_subcategoria),
    CONSTRAINT fk_desc_prod FOREIGN KEY (producto_id) REFERENCES productos(id_producto)
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE suscriptores (
    id_suscriptor BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) UNIQUE NOT NULL,
    fecha_suscripcion DATETIME NOT NULL
);

CREATE TABLE contactos (
    id_contacto BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    fecha_envio DATETIME NOT NULL
);

-- Insertamos los roles base para que no te de error el sistema
INSERT INTO roles (nombre) VALUES ('ROLE_ADMIN'), ('ROLE_CLIENT');


-- 1. Ficha de Cliente
INSERT INTO clientes (nombre, apellido, rut, fecha_nacimiento, telefono, email) 
VALUES ('Administrador', 'Principal', '111111111', '1990-01-01', '+56900000000', 'admin@magical.cl'),
('Usuario', 'Pruebas', '222222222', '1990-01-01', '+56911111111', 'usuario@magical.cl');


-- 2. Cuenta de Usuario (Contraseña admin: Admin.2026! - Contraseña usuario: Usuario.2026! )
INSERT INTO usuarios (email, password, fecha_registro, rol_id, cliente_rut) 
VALUES (
    'admin@magical.cl', 
    '$2a$10$0byDOiHcAXn7/ZggMqasjedypJMFNMT5BNJy4GlU2U2VP1Nf7uZ.O', -- Hash real para Admin.2026!
    CURRENT_DATE, 
    1, 
    '111111111'
),
(
    'usuario@magical.cl', 
    '$2a$10$NaeHmHteIj47f8iQG8bA0OAoNCdB8nZSS1OjOL0w0tHN3xUZbaNAG', -- Hash real para Usuario.2026!
    CURRENT_DATE, 
    2, 
    '222222222'
);

-- Usuarios y sus roles
SELECT id, email, password, fecha_registro, rol_id, cliente_rut 
FROM usuarios;

-- Roles de sistema
SELECT id, nombre 
FROM roles;

-- Tokens de recuperación de contraseña
SELECT id, email, token, fecha_expiracion, usado 
FROM password_reset_tokens;

-- Datos maestros de clientes
SELECT id, rut, nombre, apellido, email, telefono, fecha_nacimiento 
FROM clientes;

-- Direcciones asociadas a clientes (Relación por RUT)
SELECT id, direccion, ciudad, estado_region, pais, codigo_postal, es_principal, cliente_rut 
FROM direcciones_cliente;

-- Categorías principales
SELECT id_categoria, nombre_categoria, imagen_banner 
FROM categorias;

-- Subcategorías dependientes
SELECT id_subcategoria, nombre_subcategoria, id_categoria 
FROM subcategorias;

-- Productos con su stock y precio actual
SELECT id_producto, nombre_producto, descripcion_producto, precio_producto, stock_producto, imagen_producto, id_subcategoria 
FROM productos;

-- Cabecera de Pedidos (Ventas finalizadas)
SELECT id, cliente_id, nombre_contacto, email_contacto, estado, fecha_pedido, total, subtotal, iva, cupon_aplicado 
FROM pedidos;

-- Detalle de productos en cada pedido (Snapshots)
SELECT id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario, precio_original 
FROM items_pedido;

-- Carritos activos o abandonados
SELECT id, cliente_id, estado, fecha_creacion, fecha_actualizacion, cupon_aplicado, monto_descuento_cupon 
FROM carritos;

-- Productos dentro de los carritos
SELECT id, carrito_id, producto_id, cantidad, precio_unitario, precio_original 
FROM items_carrito;

-- Cupones de descuento disponibles
SELECT id, codigo, nombre, tipo, valor, fecha_expiracion, limite_usos, usos_actuales, activo 
FROM cupones;

-- Reglas de descuentos automáticos (por producto, categoría o global)
SELECT id, nombre, tipo, valor, alcance, fecha_inicio, fecha_fin, activo 
FROM descuentos;

-- Suscriptores al Newsletter
SELECT id_suscriptor, email, fecha_suscripcion 
FROM suscriptores;

-- Mensajes del formulario de contacto
SELECT id_contacto, nombre, email, mensaje, fecha_envio 
FROM contactos;
