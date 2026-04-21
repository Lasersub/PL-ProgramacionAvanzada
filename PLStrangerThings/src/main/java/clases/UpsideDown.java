/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

/**
 *
 * @author User
 */
public class UpsideDown {
    
    private Zona bosque;
    private Zona laboratorio;
    private Zona centroComercial;
    private Zona alcantarillado;
    private Zona colmena;

    public UpsideDown(Zona bosque, Zona laboratorio, Zona centroComercial, Zona alcantarillado, Zona colmena) {
        this.bosque = bosque;
        this.laboratorio = laboratorio;
        this.centroComercial = centroComercial;
        this.alcantarillado = alcantarillado;
        this.colmena = colmena;
    }

    public Zona getBosque() {
        return bosque;
    }

    public Zona getLaboratorio() {
        return laboratorio;
    }

    public Zona getCentroComercial() {
        return centroComercial;
    }

    public Zona getAlcantarillado() {
        return alcantarillado;
    }

    public Zona getColmena() {
        return colmena;
    }
    
    
}
