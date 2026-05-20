package agentes.clasificador;
import model.DiscordMessage;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

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
}