/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;


/**
 *
 * @author User
 */
public class Hawkins {
    
    
    private Zona radioWSQK; 
    private Zona callePrincipal;
    private Zona sotanoByers;
    
    private Portal portalBosque;
    private Portal portalLaboratorio;
    private Portal portalCentroComercial;
    private Portal portalAlcantarillado;

    public Hawkins(Zona RadioWSQK, Zona CallePrincipal, Zona SotanoByers, Portal portalBosque, Portal portalLaboratorio, Portal portalCentroComercial, Portal portalAlcantarillado) {
        this.radioWSQK = RadioWSQK;
        this.callePrincipal = CallePrincipal;
        this.sotanoByers = SotanoByers;
        this.portalBosque = portalBosque;
        this.portalLaboratorio = portalLaboratorio;
        this.portalCentroComercial = portalCentroComercial;
        this.portalAlcantarillado = portalAlcantarillado;
    }

    public Zona getRadioWSQK() {
        return radioWSQK;
    }

    public Zona getCallePrincipal() {
        return callePrincipal;
    }

    public Zona getSotanoByers() {
        return sotanoByers;
    }

    public Portal getPortalBosque() {
        return portalBosque;
    }

    public Portal getPortalLaboratorio() {
        return portalLaboratorio;
    }

    public Portal getPortalCentroComercial() {
        return portalCentroComercial;
    }

    public Portal getPortalAlcantarillado() {
        return portalAlcantarillado;
    }
    
    
}
