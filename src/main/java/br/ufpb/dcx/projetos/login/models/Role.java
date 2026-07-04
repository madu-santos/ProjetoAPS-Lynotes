package br.ufpb.dcx.projetos.login.models;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    ANYONE,
    USER,
    ADMIN
}
