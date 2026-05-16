package vision;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgenteVisualizador extends Agent {

  private static final String SERVICE_TYPE = "vision-safety";
  private static final String SERVICE_NAME = "Servicio-Vision-Seguridad";
  private static final Path PYTHON_SCRIPT = Path.of("visionModel", "predict_image.py");

  @Override
  protected void setup() {
    System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

    registerService(); // Registrar el servicio y darlo de alta

    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        // Espera mensajes, si no recibe ninguno se bloquea
        ACLMessage msg = receive();
        if (msg == null) {
          block(); // A la espera hasta recibir un mensaje.
          return; // JADE en el momento que recibe un mensaje desbloquea el agente, retorna y
                  // vuelve al bucle
        }

      }
    });
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
      System.out.println("Agente desregistrado del DF. Adiós!");
    } catch (FIPAException exception) {
      exception.printStackTrace();
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
      System.out.println("Agente registrado correctamente en el DF como '" + SERVICE_TYPE + "'.");
    } catch (FIPAException exception) {
      throw new IllegalStateException("No se pudo registrar el agente de visión", exception);
    }
  }

  private AnalysisRequest resolveRequest(ACLMessage msg) throws IOException {
// TODO
    return null;
  }

  private String analyzeWithVisionModel(AnalysisRequest request) throws IOException, InterruptedException {
//TODO
    return null;
  }

}
