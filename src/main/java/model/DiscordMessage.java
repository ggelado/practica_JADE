package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DiscordMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum Detecciones {
    GUN,
    BLOOD,
    VIOLENCE,
    FIGHT,
    NAZI, //TODO COMPLETAR TIPOS Y CATEGORÍAS CON LAS DE TEXTO
	 
    SPAM,
    SCAM,
    DISCRIMINATION,
    TOXIC,
    QUESTION,
    LINK,
    MENTION,
    HELP,
    POSITIVE,
    GREETING,
	
	DEPRESSION,
	ANXIETY,  
	SELF_HARM, 
	LONELINESS;

    public static Detecciones fromLabel(String value) {
      if (value == null) {
        return null;
      }

      String normalized = value.trim().toLowerCase().replace('_', '-');
      switch (normalized) {
        case "blood":
          return BLOOD;
        case "violence":
          return VIOLENCE;
        case "fight":
          return FIGHT;
        case "nazi":
        case "nazi-symbol":
          return NAZI;
        case "safe":
          return null;
        case "gun":
        case "ak47":
        case "m4a1-s":
        case "m4a1":
        case "galil":
        case "famas":
        case "tec-9":
        case "five-seven":
        case "glock-18":
        case "usp-s":
        case "eagle":
        case "berettas":
        case "p2000":
        case "mac10":
        case "mp5":
        case "mp9":
        case "p90":
        case "p250":
        case "ssg08":
        case "awp":
          return GUN;
        default:
          return null;
      }
    }
  }
  
  private final String mensaje;
  private final String id_mensaje;
  private final List<Detecciones> findings;
  
  public DiscordMessage(String msg, String id) {
    this.mensaje = Objects.requireNonNullElse(msg, "");
    this.id_mensaje = Objects.requireNonNullElse(id, "");
    this.findings = new ArrayList<>();
  }
  
  public String getMensaje() {
    return mensaje;
  }
  public String getId() {
    return id_mensaje;
  }
  
  public void agregarDetecciones(Detecciones deteccion) {
    if (deteccion != null && !findings.contains(deteccion)) {
      findings.add(deteccion);
    }
  }

  
  public Collection<Detecciones> getDetecciones() {
    return Collections.unmodifiableList(new ArrayList<>(findings));
  }

  
  
}
