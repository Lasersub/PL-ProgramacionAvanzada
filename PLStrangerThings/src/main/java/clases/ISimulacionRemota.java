package clases;

public interface ISimulacionRemota extends java.rmi.Remote {

    int getNinosEnHawkins() throws java.rmi.RemoteException;
    int getNinosPortalBosque() throws java.rmi.RemoteException;
    int getNinosPortalLaboratorio() throws java.rmi.RemoteException;
    int getNinosPortalCentroComercial() throws java.rmi.RemoteException;
    int getNinosPortalAlcantarillado() throws java.rmi.RemoteException;
    int getNinosLaboratorio() throws java.rmi.RemoteException;
    int getNinosCentroComercial() throws java.rmi.RemoteException;
    int getNinosBosque() throws java.rmi.RemoteException;
    int getNinosAlcantarillado() throws java.rmi.RemoteException;
    int getNinosColmena() throws java.rmi.RemoteException;
    int getDemogorgonesLaboratorio() throws java.rmi.RemoteException;
    int getDemogorgonesCentroComercial() throws java.rmi.RemoteException;
    int getDemogorgonesBosque() throws java.rmi.RemoteException;
    int getDemogorgonesAlcantarillado() throws java.rmi.RemoteException;
    java.util.List<String> getRankingTop3() throws java.rmi.RemoteException;
    String getEventoActivo() throws java.rmi.RemoteException;
    long getSegundosRestantesEvento() throws java.rmi.RemoteException;
    void pausarReanudar() throws java.rmi.RemoteException;
    boolean isPausado() throws java.rmi.RemoteException;
}
