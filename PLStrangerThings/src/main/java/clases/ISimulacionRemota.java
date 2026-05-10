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
    java.util.List<String> getNinosCallePrincipal() throws java.rmi.RemoteException;
    java.util.List<String> getNinosSotanoByers() throws java.rmi.RemoteException;
    java.util.List<String> getNinosRadioWSQK() throws java.rmi.RemoteException;

    java.util.List<String> getNinosLaboratorio() throws java.rmi.RemoteException;
    java.util.List<String> getNinosCentroComercial() throws java.rmi.RemoteException;
    java.util.List<String> getNinosBosque() throws java.rmi.RemoteException;
    java.util.List<String> getNinosAlcantarillado() throws java.rmi.RemoteException;

    int getNinosEnColaIdaPortalBosque() throws java.rmi.RemoteException;
    int getNinosEnColaIdaPortalLaboratorio() throws java.rmi.RemoteException;
    int getNinosEnColaIdaPortalCentroComercial() throws java.rmi.RemoteException;
    int getNinosEnColaIdaPortalAlcantarillado() throws java.rmi.RemoteException;

    int getNinosEsperandoVueltaPortalBosque() throws java.rmi.RemoteException;
    int getNinosEsperandoVueltaPortalLaboratorio() throws java.rmi.RemoteException;
    int getNinosEsperandoVueltaPortalCentroComercial() throws java.rmi.RemoteException;
    int getNinosEsperandoVueltaPortalAlcantarillado() throws java.rmi.RemoteException;

    int getCapacidadPortalBosque() throws java.rmi.RemoteException;
    int getCapacidadPortalLaboratorio() throws java.rmi.RemoteException;
    int getCapacidadPortalCentroComercial() throws java.rmi.RemoteException;
    int getCapacidadPortalAlcantarillado() throws java.rmi.RemoteException;

    java.util.List<String> getDemogorgonesLaboratorio() throws java.rmi.RemoteException;
    java.util.List<String> getDemogorgonesCentroComercial() throws java.rmi.RemoteException;
    java.util.List<String> getDemogorgonesBosque() throws java.rmi.RemoteException;
    java.util.List<String> getDemogorgonesAlcantarillado() throws java.rmi.RemoteException;

    int getNumGotasSangre() throws java.rmi.RemoteException;
    int getNumCapturasColmena() throws java.rmi.RemoteException;

}
