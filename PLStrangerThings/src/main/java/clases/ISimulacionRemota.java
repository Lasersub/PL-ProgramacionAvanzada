package clases;

public interface ISimulacionRemota extends java.rmi.Remote {
    // Interfaz PanelControl.java
    int getNumNinosEnHawkins() throws java.rmi.RemoteException;
    
    int getNumNinosPortalBosque() throws java.rmi.RemoteException;
    int getNumNinosPortalLaboratorio() throws java.rmi.RemoteException;
    int getNumNinosPortalCentroComercial() throws java.rmi.RemoteException;
    int getNumNinosPortalAlcantarillado() throws java.rmi.RemoteException;
    
    int getNumNinosLaboratorio() throws java.rmi.RemoteException;
    int getNumNinosCentroComercial() throws java.rmi.RemoteException;
    int getNumNinosBosque() throws java.rmi.RemoteException;
    int getNumNinosAlcantarillado() throws java.rmi.RemoteException;
    int getNumNinosColmena() throws java.rmi.RemoteException;
    
    int getNumDemogorgonesLaboratorio() throws java.rmi.RemoteException;
    int getNumDemogorgonesCentroComercial() throws java.rmi.RemoteException;
    int getNumDemogorgonesBosque() throws java.rmi.RemoteException;
    int getNumDemogorgonesAlcantarillado() throws java.rmi.RemoteException;
    
    java.util.List<String> getRankingTop3() throws java.rmi.RemoteException;
    
    String getEventoActivo() throws java.rmi.RemoteException;
    long getSegundosRestantesEvento() throws java.rmi.RemoteException;
    
    void pausarReanudar() throws java.rmi.RemoteException;
    boolean isPausado() throws java.rmi.RemoteException;
    
    
    
    // Interfaz PanelInfo.java
    java.util.List<Nino> getNinosCallePrincipal() throws java.rmi.RemoteException;
    java.util.List<Nino> getNinosSotanoByers() throws java.rmi.RemoteException;
    java.util.List<Nino> getNinosRadioWSQK() throws java.rmi.RemoteException;
    
    java.util.List<Nino> getNinosLaboratorio() throws java.rmi.RemoteException;
    java.util.List<Nino> getNinosCentroComercial() throws java.rmi.RemoteException;
    java.util.List<Nino> getNinosBosque() throws java.rmi.RemoteException;
    java.util.List<Nino> getNinosAlcantarillado() throws java.rmi.RemoteException;
    
    Portal getPortalLaboratorio() throws java.rmi.RemoteException;
    Portal getPortalCentroComercial() throws java.rmi.RemoteException;
    Portal getPortalBosque() throws java.rmi.RemoteException;
    Portal getPortalAlcantarillado() throws java.rmi.RemoteException;
    
    java.util.List<Demogorgon> getDemogorgonsLaboratorio() throws java.rmi.RemoteException;
    java.util.List<Demogorgon> getDemogorgonsCentroComercial() throws java.rmi.RemoteException;
    java.util.List<Demogorgon> getDemogorgonsBosque() throws java.rmi.RemoteException;
    java.util.List<Demogorgon> getDemogorgonsAlcantarillado() throws java.rmi.RemoteException;
    
    int getNumGotasSangre() throws java.rmi.RemoteException;
    int getNumCapturasColmena() throws java.rmi.RemoteException;
    
}
