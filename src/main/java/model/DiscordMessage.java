package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// JADE serializa los objetos que se pasan entre agentes con setContentObject(), así que la clase tiene que ser Serializable
public class DiscordMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum Detecciones {
    GUN, BLOOD, VIOLENCE, FIGHT, NAZI,

    SPAM, SCAM, DISCRIMINATION, TOXIC, QUESTION, LINK, MENTION, HELP, POSITIVE, GREETING,

    DEPRESSION, ANXIETY, SELF_HARM, LONELINESS;

    public static Detecciones fromLabel(String value) {
      if (value == null) {
        return null;
      }

      // Normalizamos para aceptar variantes: minúsculas y guión bajo tratado igual que guión (p.ej. self_harm == self-harm)
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
      case "toxic":
        return TOXIC;
      case "spam":
        return SPAM;
      case "scam":
        return SCAM;
      case "discrimination":
        return DISCRIMINATION;
      case "depression":
        return DEPRESSION;
      case "anxiety":
        return ANXIETY;
      case "self_harm":
      case "self-harm":
        return SELF_HARM;
      case "loneliness":
        return LONELINESS;
      case "help":
        return HELP;
      case "positive":
        return POSITIVE;
      case "greeting":
        return GREETING;
      case "question":
        return QUESTION;
      case "link":
        return LINK;
      case "mention":
        return MENTION;
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
  private final String channelId;
  private final List<Detecciones> findings;

  public DiscordMessage(String msg, String id, String chId) {
    this.mensaje = Objects.requireNonNullElse(msg, "");
    this.id_mensaje = Objects.requireNonNullElse(id, "");
    this.channelId = Objects.requireNonNullElse(chId, "");
    this.findings = new ArrayList<>();
  }

  public String getMensaje() {
    return mensaje;
  }

  public String getId() {
    return id_mensaje;
  }

  public String getChannelId() {
    return channelId;
  }

  public void agregarDetecciones(Detecciones deteccion) {
    if (deteccion != null && !findings.contains(deteccion)) {
      findings.add(deteccion);
    }
  }

  public Collection<Detecciones> getDetecciones() {
    // Devolvemos una copia inmutable para que nadie pueda modificar la lista interna por accidente
    return Collections.unmodifiableList(new ArrayList<>(findings));
  }

}
