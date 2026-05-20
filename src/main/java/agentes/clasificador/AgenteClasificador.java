package agentes.clasificador;
import model.DiscordMessage;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
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
              public void action() {}
              ACLMessage msg = receive();
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