package agentes.clasificador;

import jade.core.Agent;

public class AgenteClasificador extends Agent {
	  private static final String SERVICE_TYPE = "clasificador";
	  private static final String SERVICE_NAME = "Servicio-Clasificador";
	  private static final String ONTOLOGY_PATH = "ontologia.rdf";
	  private static final String NS = "http://www.discord.monitor/ontologia#";

    @Override
    protected void setup() {
    	 System.out.println("[AgenteClasificador] Arrancando: " + getAID().getName());
    	 registerService();
    	 
    }
}