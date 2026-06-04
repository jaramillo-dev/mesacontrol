-- 1. Agregar restricción de integridad para el Enum de Roles
ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_rol
        CHECK (rol IN ('ADMIN', 'VENTAS', 'TECNICO'));

-- 2. Insertar usuarios de prueba
-- NOTA: El password para todos es '123456'.
-- El hash generado es de BCrypt (costo 10), compatible con tu BCryptPasswordEncoder.
INSERT INTO usuario (nombre, email, password, rol, activo) VALUES
                                                               ('Admin Principal', 'admin@mesacontrol.imss.mx', '$2a$10$IDzRrqrA8Xfk1dPrwucOp.FtvWrzdaN2NPpjkH8EDyBSe/FxVYMpq', 'ADMIN', true),
                                                               ('Agente de Ventas', 'ventas@mesacontrol.imss.mx', '$2a$10$IDzRrqrA8Xfk1dPrwucOp.FtvWrzdaN2NPpjkH8EDyBSe/FxVYMpq', 'VENTAS', true),
                                                               ('Técnico de Soporte', 'tecnico@mesacontrol.imss.mx', '$2a$10$IDzRrqrA8Xfk1dPrwucOp.FtvWrzdaN2NPpjkH8EDyBSe/FxVYMpq', 'TECNICO', true);