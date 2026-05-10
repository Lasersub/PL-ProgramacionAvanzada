package clases;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.*;
import java.util.Comparator;

public class SimulacionRemota extends UnicastRemoteObject implements ISimulacionRemota {

    private SimulacionBackend backend;

    public SimulacionRemota(SimulacionBackend backend) throws RemoteException {
        super();
        this.backend = backend;
    }

    @Override
    public int getNumNinosEnHawkins() throws RemoteException {
        return backend.getHawkins().getCallePrincipal().getListaNinos().size()
             + backend.getHawkins().getSotanoByers().getListaNinos().size()
             + backend.getHawkins().getRadioWSQK().getListaNinos().size();
    }

    @Override
    public int getNumNinosPortalBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaNinos().size();
    }

    @Override
    public int getNumNinosCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaNinos().size();
    }

    @Override
    public int getNumNinosBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaNinos().size();
    }

    @Override
    public int getNumNinosAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaNinos().size();
    }

    @Override
    public int getNumNinosColmena() throws RemoteException {
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }

    @Override
    public int getNumDemogorgonesLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaDemogorgons().size();
    }

    @Override
    public List<String> getRankingTop3() throws RemoteException {
        List<Demogorgon> copia = new ArrayList<>(backend.getDemogorgons());
        copia.sort(Comparator.comparingInt(Demogorgon::getCapturas).reversed());
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < Math.min(3, copia.size()); i++) {
            Demogorgon d = copia.get(i);
            resultado.add(d.getId() + ": " + d.getCapturas() + " capturas");
        }
        return resultado;
    }

    @Override
    public String getEventoActivo() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null || gestor.getEventoActual() == GestorEventos.TipoEvento.NINGUNO) {
            return "Sin evento activo";
        }
        return gestor.getEventoActual().name().replace('_', ' ');
    }

    @Override
    public long getSegundosRestantesEvento() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null) return 0;
        return gestor.getSegundosRestantes();
    }

    @Override
    public void pausarReanudar() throws RemoteException {
        if (backend.isPausado()) {
            backend.reanudarSimulacion();
        } else {
            backend.pausarSimulacion();
        }
    }

    @Override
    public boolean isPausado() throws RemoteException {
        return backend.isPausado();
    }

    
    
    
    
    
    @Override
    public List<Nino> getNinosLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaNinos();
    }

    @Override
    public List<Nino> getNinosCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaNinos();
    }

    @Override
    public List<Nino> getNinosBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaNinos();
    }

    @Override
    public List<Nino> getNinosAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaNinos();
    }

    @Override
    public List<Nino> getNinosPortalIdaLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEnColaIda();
    }

    @Override
    public List<Nino> getNinosPortalIdaCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEnColaIda();
    }

    @Override
    public List<Nino> getNinosPortalIdaBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEnColaIda();
    }

    @Override
    public List<Nino> getNinosPortalIdaAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEnColaIda();
    }

    @Override
    public int getNinosPortalVueltaLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalVueltaCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalVueltaBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalVueltaAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEsperandoVuelta();
    }

    @Override
    public List<Demogorgon> getDemogorgonsLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaDemogorgons();
    }

    @Override
    public List<Demogorgon> getDemogorgonsCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaDemogorgons();
    }

    @Override
    public List<Demogorgon> getDemogorgonsBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaDemogorgons();
    }

    @Override
    public List<Demogorgon> getDemogorgonsAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaDemogorgons();
    }

    @Override
    public List<Nino> getNinosCallePrincipal() throws RemoteException {
        return backend.getHawkins().getCallePrincipal().getListaNinos();
    }

    @Override
    public List<Nino> getNinosSotanoByers() throws RemoteException {
        return backend.getHawkins().getSotanoByers().getListaNinos();
    }

    @Override
    public List<Nino> getNinosRadioWSQK() throws RemoteException {
        return backend.getHawkins().getRadioWSQK().getListaNinos();
    }
}
