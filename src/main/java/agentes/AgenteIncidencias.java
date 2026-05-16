package agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class AgenteIncidencias extends Agent {

    protected void setup() {
        System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID()); 
        
        ServiceDescription sd = new ServiceDescription();
        sd.setType("gestion-incidencias"); 
        sd.setName("Servicio-Alertas-Seguridad");
        
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            System.out.println("Agente registrado correctamente en el DF como 'gestion-incidencias'.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("\n**************************************");
                    System.out.println("¡ALERTA RECIBIDA!");
                    System.out.println("Contenido: " + msg.getContent());
                    System.out.println("**************************************\n");
                } else {
                    block(); 
                }
            }
        });
    }

    
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Agente desregistrado del DF. Adiós!");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}