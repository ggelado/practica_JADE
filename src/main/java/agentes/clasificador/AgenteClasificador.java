package agentes.clasificador;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import model.DiscordMessage;
import openllet.jena.PelletReasonerFactory;

public class AgenteClasificador extends Agent {

  private static final String SERVICE_TYPE = "clasificador";
  private static final String SERVICE_NAME = "Servicio-Clasificador";
  private static final String ONTOLOGY_PATH = "ontologia.rdf";
  private static final String NS = "http://www.discord.monitor/ontologia#";

  private OntModel modeloBase;

  @Override
  protected void setup() {
    System.out.println("[AgenteClasificador] Arrancando: " + getAID().getName());

    try (InputStream in = new FileInputStream(ONTOLOGY_PATH)) {
      modeloBase = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
      modeloBase.read(in, null, "RDF/XML");
      System.out.println("[AgenteClasificador] Ontología cargada correctamente.");
    } catch (Exception e) {
      System.err.println("[AgenteClasificador] Error cargando ontología: " + e.getMessage());
      return;
    }

    registerService();

    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        ACLMessage msg = receive();
        if (msg != null) {
          try {
            DiscordMessage discordMsg = (DiscordMessage) msg.getContentObject();

            System.out.println("[AgenteClasificador] Mensaje recibido -> id: " + discordMsg.getId() + " | detecciones: "
                + discordMsg.getDetecciones());

            String nivel = clasificarMensaje(discordMsg);

            System.out.println("[AgenteClasificador] Nivel inferido: " + nivel);

            reenviarSegunNivel(discordMsg, nivel);

          } catch (UnreadableException e) {
            System.err.println("[AgenteClasificador] Error leyendo mensaje: " + e.getMessage());
          }
        } else {
          block();
        }
      }
    });
  }

  private String clasificarMensaje(DiscordMessage msg) {
    OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, modeloBase);

    OntClass clsMensaje = model.getOntClass(NS + "Mensaje");
    if (clsMensaje == null) {
      System.err.println("[AgenteClasificador] Clase Mensaje no encontrada en la ontología");
      return "SinClasificar";
    }

    Individual individuo = model.createIndividual(NS + "msg_" + msg.getId(), clsMensaje);

    ObjectProperty tieneDeteccion = model.getObjectProperty(NS + "tieneDeteccion");
    if (tieneDeteccion == null) {
      System.err.println("[AgenteClasificador] Propiedad tieneDeteccion no encontrada");
      return "SinClasificar";
    }

    for (DiscordMessage.Detecciones d : msg.getDetecciones()) {
      OntClass clsDeteccion = getClaseDeteccion(model, d);
      if (clsDeteccion != null) {
        Individual indDet = model.createIndividual(NS + "det_" + msg.getId() + "_" + d.name().toLowerCase(),
            clsDeteccion);
        individuo.addProperty(tieneDeteccion, indDet);
        System.out.println("[AgenteClasificador] Detección añadida: " + d.name());
      } else {
        System.err.println("[AgenteClasificador] Clase no encontrada para: " + d);
      }
    }

    ObjectProperty tieneNivel = model.getObjectProperty(NS + "tieneNivel");
    if (tieneNivel == null) {
      System.err.println("[AgenteClasificador] Propiedad tieneNivel no encontrada");
      return "SinClasificar";
    }

    // Recoger todos los niveles inferidos y quedarse con el más grave
    org.apache.jena.rdf.model.NodeIterator nit = individuo.listPropertyValues(tieneNivel);
    String nivelMasGrave = "SinClasificar";
    while (nit.hasNext()) {
      String nivelUri = nit.next().toString();
      String nivel = nivelUri.contains("#") ? nivelUri.split("#")[1] : nivelUri;
      System.out.println("[AgenteClasificador] Nivel inferido parcial: " + nivel);
      nivelMasGrave = elegirMasGrave(nivelMasGrave, nivel);
    }

    return nivelMasGrave;
  }

  private String elegirMasGrave(String actual, String nuevo) {
    int pesoActual = getPeso(actual);
    int pesoNuevo = getPeso(nuevo);
    return pesoActual >= pesoNuevo ? actual : nuevo;
  }

  private int getPeso(String nivel) {
    switch (nivel) {
    case "riesgoCritico":
      return 5;
    case "riesgoGrave":
      return 4;
    case "alertaSaludMental":
      return 3;
    case "riesgoModerado":
      return 2;
    case "riesgoLeve":
      return 1;
    case "sinRiesgo":
      return 0;
    default:
      return -1;
    }
  }

  private OntClass getClaseDeteccion(OntModel model, DiscordMessage.Detecciones d) {
    switch (d) {
    case GUN:
      return model.getOntClass(NS + "Deteccion_Arma");
    case BLOOD:
    case FIGHT:
      return model.getOntClass(NS + "DeteccionViolenciaLeve");
    case VIOLENCE:
      return model.getOntClass(NS + "DeteccionViolenciaGrave");
    case NAZI:
    case DISCRIMINATION:
      return model.getOntClass(NS + "Deteccion_Odio");
    case TOXIC:
      return model.getOntClass(NS + "Deteccion_Toxica");
    case SPAM:
      return model.getOntClass(NS + "DeteccionMolestaLeve");
    case SCAM:
      return model.getOntClass(NS + "DeteccionMolestaGrave");
    case DEPRESSION:
    case ANXIETY:
    case LONELINESS:
      return model.getOntClass(NS + "DeteccionSaludMentalAlerta");
    case SELF_HARM:
      return model.getOntClass(NS + "DeteccionSaludMentalCritica");
    case QUESTION:
    case LINK:
    case MENTION:
      return model.getOntClass(NS + "Deteccion_Neutra");
    case HELP:
    case POSITIVE:
    case GREETING:
      return model.getOntClass(NS + "Deteccion_Positiva");
    default:
      return null;
    }
  }

  private void reenviarSegunNivel(DiscordMessage msg, String nivel) {
    System.out.println("[AgenteClasificador] reenviarSegunNivel -> id: " + msg.getId() + " | nivel: " + nivel);
    switch (nivel) {
    case "riesgoCritico":
      enviarASancionador(msg);
      enviarAIncidencias(msg);
      break;
    case "riesgoGrave":
      enviarASancionador(msg);
      enviarAIncidencias(msg);
      break;
    case "alertaSaludMental":
      System.out.println("[AgenteClasificador] Alerta de salud mental -> id: " + msg.getId());
      enviarAIncidencias(msg);
      break;
    case "riesgoModerado":
      enviarASancionador(msg);
      break;
    case "riesgoLeve":
      System.out.println("[AgenteClasificador] Riesgo leve -> advertencia -> id: " + msg.getId());
      break;
    default:
      System.out.println("[AgenteClasificador] Mensaje sin riesgo -> id: " + msg.getId());
    }
  }

  private void enviarAIncidencias(DiscordMessage msg) {
    try {
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("gestion-incidencias");
      template.addServices(sd);

      DFAgentDescription[] agents = DFService.search(this, template);
      if (agents == null || agents.length == 0) {
        System.err.println("[AgenteClasificador] No se encontró AgenteIncidencias.");
        return;
      }

      ACLMessage alerta = new ACLMessage(ACLMessage.INFORM);
      alerta.addReceiver(agents[0].getName());
      alerta.setContentObject(msg);
      send(alerta);

      System.out.println("[AgenteClasificador] Alerta enviada a AgenteIncidencias -> id: " + msg.getId());

    } catch (Exception e) {
      System.err.println("[AgenteClasificador] Error enviando a AgenteIncidencias: " + e.getMessage());
    }
  }

  private void enviarASancionador(DiscordMessage msg) {
    try {
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("sancionador");
      template.addServices(sd);

      DFAgentDescription[] agents = DFService.search(this, template);
      if (agents == null || agents.length == 0) {
        System.err.println("[AgenteClasificador] No se encontró AgenteSancionador.");
        return;
      }

      ACLMessage alerta = new ACLMessage(ACLMessage.INFORM);
      alerta.addReceiver(agents[0].getName());
      alerta.setContentObject(msg);
      send(alerta);

      System.out.println("[AgenteClasificador] Mensaje enviado al Sancionador -> id: " + msg.getId());

    } catch (Exception e) {
      System.err.println("[AgenteClasificador] Error enviando al Sancionador: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void registerService() {
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());

    ServiceDescription sd = new ServiceDescription();
    sd.setType(SERVICE_TYPE);
    sd.setName(SERVICE_NAME);
    dfd.addServices(sd);

    try {
      DFService.register(this, dfd);
      System.out.println("[AgenteClasificador] Registrado en el DF como '" + SERVICE_TYPE + "'.");
    } catch (FIPAException e) {
      throw new IllegalStateException("No se pudo registrar el AgenteClasificador", e);
    }
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
      System.out.println("[AgenteClasificador] Desregistrado del DF.");
    } catch (FIPAException e) {
      e.printStackTrace();
    }
  }
}