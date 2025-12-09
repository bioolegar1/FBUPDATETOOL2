package com.fbupdatetool.service;

public interface SecurityCallback {


    /**
     * Solicita permissão administrativa para um comando crítico.
     *
     * @param command O comando SQL perigoso (ex: DROP DATABASE).
     * @return true se a senha estiver correta, false se negado.
     */
    boolean requestAdminPermission(String command);

}
