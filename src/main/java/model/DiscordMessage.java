package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiscordMessage implements Serializable {

  public enum Detecciones {
    GUN, BLOOD, VIOLENCE, FIGHT, NAZI //TODO COMPLETAR TIPOS Y CATEGORÍAS
  }
  
  private String mensaje;
  private String id_mensaje;
  private List<Detecciones> findings;
  
  public DiscordMessage(String msg, String id) {
    this.mensaje=msg;
    this.id_mensaje=id;
    this.findings = new ArrayList<>();
  }
  
  public String getMensaje() {
    return mensaje;
  }
  public String getId() {
    return id_mensaje;
  }
  
  public void agregarDetecciones(Detecciones deteccion) {
    findings.add(deteccion);
  }
  
  public Collection<Detecciones> getDetecciones() {
    return new ArrayList<>(findings);
  }
  
  
}
