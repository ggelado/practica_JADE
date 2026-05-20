package agentes.clasificador;
import model.DiscordMessage;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.domain.FIPAException;


public class AgenteClasificador extends Agent {
	  private static final String SERVICE_TYPE = "clasificador";
	  private static final String SERVICE_NAME = "Servicio-Clasificador";
	  private static final String ONTOLOGY_PATH = "ontologia.rdf";
	  private static final String NS = "http://www.discord.monitor/ontologia#";

	  @Override
	    protected void setup() {
	    	 System.out.println("[AgenteClasificador] Arrancando: " + getAID().getName());
	    	 registerService();
	    	 addBehaviour(new CyclicBehaviour() {
	              @Override
	              public void action() {
	              ACLMessage msg = receive();
	              
	              if (msg != null) {
	                  try {
	                      DiscordMessage discordMsg = (DiscordMessage) msg.getContentObject();

	                      System.out.println("[AgenteClasificador] Mensaje recibido -> id: "
	                              + discordMsg.getId()
	                              + " | detecciones: " + discordMsg.getDetecciones());

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

				private String clasificarMensaje(DiscordMessage discordMsg) {
					 // Cargar ontología
			        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
			        try (InputStream in = new FileInputStream(ONTOLOGY_PATH)) {
			            model.read(in, null, "RDF/XML");
			        } catch (Exception e) {
			            System.err.println("[AgenteClasificador] Error cargando ontología: " + e.getMessage());
			            return "SinClasificar";
			        }
			        // Crear individuo Mensaje en la ontología
			        OntClass clsMensaje = model.getOntClass(NS + "Mensaje");
			        if (clsMensaje == null) {
			            System.err.println("[AgenteClasificador] Clase Mensaje no encontrada en la ontología");
			            return "SinClasificar";
			        }
			        Individual individuo = model.createIndividual(NS + "msg_" + msg.getId(), clsMensaje);
			     // Añadir cada detección como individuo vinculado
			        ObjectProperty tieneDeteccion = model.getObjectProperty(NS + "tieneDeteccion");
			        
			        for (DiscordMessage.Detecciones d : msg.getDetecciones()) {
			            OntClass clsDeteccion = getClaseDeteccion(model, d);
			            if (clsDeteccion != null) {
			                Individual indDet = model.createIndividual(NS + "det_" + d.name().toLowerCase(), clsDeteccion);
			                individuo.addProperty(tieneDeteccion, indDet);
			                System.out.println("[AgenteClasificador] Añadida detección: " + d.name() + " -> " + clsDeteccion.getLocalName());
			            }
			        }
			        // Consultar el nivel inferido por el razonador
			        ObjectProperty tieneNivel = model.getObjectProperty(NS + "tieneNivel");
			        Statement stmt = individuo.getProperty(tieneNivel);

			        if (stmt != null) {
			            String nivelUri = stmt.getObject().toString();
			            // Devolver solo el nombre local (sin la URI completa)
			            return nivelUri.contains("#") ? nivelUri.split("#")[1] : nivelUri;
			        }

			        return "SinClasificar";
				}

				private void reenviarSegunNivel(DiscordMessage discordMsg, String nivel) {
					// TODO Auto-generated method stub
					
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
    
    private OntClass getClaseDeteccion(OntModel model, DiscordMessage.Detecciones d) {
        switch (d) {
            case GUN:
                return model.getOntClass(NS + "DeteccionArma");
            case BLOOD:
            case FIGHT:
                return model.getOntClass(NS + "DeteccionViolenciaLeve");
            case VIOLENCE:
                return model.getOntClass(NS + "DeteccionViolenciaGrave");
            case NAZI:
            case DISCRIMINATION:
                return model.getOntClass(NS + "DeteccionOdio");
            case TOXIC:
                return model.getOntClass(NS + "DeteccionToxica");
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
                return model.getOntClass(NS + "DeteccionNeutra");
            case HELP:
            case POSITIVE:
            case GREETING:
                return model.getOntClass(NS + "DeteccionPositiva");
            default:
                return null;
        }
    }
}