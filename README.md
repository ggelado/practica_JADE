# Sistema multiagente para la moderación de mensajes en Discord

Este repositorio contiene la implementación de un sistema multiagente (SMA) desarrollado en Java utilizando el framework JADE. El proyecto consiste en un moderador inteligente para servidores de Discord que filtra contenido inapropiado en imágenes y analiza el texto de los mensajes para detectar conductas de riesgo, automatizando parte del trabajo de administración del servidor.

## Arquitectura del Sistema

La arquitectura se divide en seis agentes que se corresponden con los bloques temáticos de la asignatura. Los agentes se comunican entre sí mediante mensajes ACL y se localizan a través del Directory Facilitator (DF) de JADE.

Consultar UML.

### 1. Recuperación de Información — AgentePerceptor
Se conecta a Discord mediante JDA y escucha todos los mensajes del servidor. Por cada mensaje:
- Si contiene imágenes, las envía al **AgenteVisualizador**.
- Si contiene texto, lo envía al **AgenteAnalista**.

### 2. Percepción Computacional — AgenteVisualizador
Recibe la URL de una imagen y la analiza ejecutando un script Python con modelos YOLO preentrenados. Detecta: armas, violencia, sangre, peleas y simbología nazi. Envía las detecciones al **AgenteClasificador**.

Modelos utilizados (en `visionModel/models/`):
- Simbología nazi: [yolo11s-cls-nazi-symbols](https://huggingface.co/zhiwei2017/yolo11s-cls-nazi-symbols)
- Contenido NSFW: [nsfw_image_detection](https://huggingface.co/Falconsai/nsfw_image_detection)
- Violencia y peleas: [Fight-Violence-detection-yolov8](https://github.com/Musawer1214/Fight-Violence-detection-yolov8)

### 3. Descubrimiento de Conocimiento — AgenteAnalista
Recibe el texto de un mensaje y lo clasifica mediante una llamada a la API de Gemini. Obtiene etiquetas como `toxic`, `spam`, `depression`, `self_harm`, etc. Envía las detecciones al **AgenteClasificador**.

### 4. Ontologías y Representación del Conocimiento — AgenteClasificador
Carga la ontología `ontologia.rdf` (diseñada en Protégé, formato OWL/RDF) y usa el razonador Pellet (vía Apache Jena) para inferir el nivel de riesgo de cada mensaje a partir de sus detecciones. Según el nivel inferido:

| Nivel | Acción |
|---|---|
| `riesgoCritico` / `riesgoGrave` | Eliminar mensaje + alertar admin |
| `alertaSaludMental` | Alertar admin (sin borrar) |
| `riesgoModerado` | Eliminar mensaje |
| `riesgoLeve` | Solo log |

### 5. Sanción — AgenteSancionador
Recibe la orden de borrar un mensaje, lo elimina del canal de Discord y publica una imagen de reemplazo configurable.

### 6. Gestión de Incidencias — AgenteIncidencias
Notifica al administrador del servidor enviando un mensaje a un canal o por DM, según lo configurado en `token.env`.

---

## Requisitos previos

- **Java 17** o superior
- **Maven 3.6** o superior
- **Python 3.8** o superior
- Una cuenta de Discord con un bot creado en el [Portal de Desarrolladores](https://discord.com/developers/applications)
- Una clave de API de Gemini (obtenible en [Google AI Studio](https://aistudio.google.com/))

---

## Preparación del entorno

### 1. Configurar el entorno virtual de Python (modelo de visión)

El script Python que analiza imágenes necesita sus dependencias en un entorno virtual. Ejecutar desde la raíz del proyecto:

**Windows (PowerShell o cmd):**
```bash
cd visionModel
python -m venv ./venv
.\venv\Scripts\activate
pip install -r requirements.txt
```

**Linux / macOS:**
```bash
cd visionModel
python -m venv ./venv
source ./venv/bin/activate
pip install -r requirements.txt
```

### 2. Crear el archivo `token.env`

Crear un archivo llamado `token.env` en la **raíz del proyecto** (no se sube al repositorio). Contenido:

```env
DISCORD_TOKEN=el_token_de_tu_bot_de_discord
ADMIN_DISCORD_ID=id_del_canal_o_usuario_administrador
SAFE_IMAGE_URL=https://url-de-imagen-de-reemplazo.jpg
GEMINI_API_KEY=tu_clave_de_api_de_gemini

# Ruta al ejecutable de Python del entorno virtual creado en el paso anterior:
# Windows:
VISION_PYTHON_PATH=visionModel/venv/Scripts/python
# Linux / macOS:
# VISION_PYTHON_PATH=visionModel/venv/bin/python
```

> `ADMIN_DISCORD_ID` puede ser el ID de un canal de texto o el ID de un usuario. Si es un canal, el bot enviará la alerta allí; si es un usuario, la enviará por DM.

### 3. Compilar el proyecto

```bash
mvn compile
```

Esto también descarga las dependencias Maven y las copia en `target/lib/`.

---

## Ejecución

Usar la configuración de lanzamiento incluida en el repositorio: `runConfigurations/AgenteVisualizador.launch`. En VS Code, aparece en el panel **Run & Debug** como *AgenteVisualizador*. Hacer clic en el botón de play. En Eclipse, usar **Run As**.

Al arrancar, la GUI de JADE se abrirá y se verán los seis agentes registrados en el DF. En la consola aparecerán mensajes de confirmación de cada agente.

---

## Estructura del proyecto

```
practica_JADE/
├── src/main/java/
│   ├── agentes/
│   │   ├── percepcion/      AgentePerceptor
│   │   ├── vision/          AgenteVisualizador
│   │   ├── analista/        AgenteAnalista
│   │   ├── clasificador/    AgenteClasificador
│   │   ├── sancionador/     AgenteSancionador
│   │   └── incidencias/     AgenteIncidencias
│   └── model/
│       └── DiscordMessage.java
├── visionModel/
│   ├── predict_image.py     Script de inferencia YOLO
│   ├── models/              Pesos de los modelos (.pt)
│   └── requirements.txt
├── lib/                     JADE y commons-codec (locales)
├── runConfigurations/
│   └── AgenteVisualizador.launch    Configuración de lanzamiento para VS Code/Eclipse
├── ontologia.rdf            Ontología OWL del sistema
├── token.env                Credenciales (NO subir al repo)
└── pom.xml
```

---

## Identificación del Grupo

Grupo 15: Jiade Zheng , Gonzalo Gelado Rodríguez, Alejandra González Gila, Francisco Criado Lacosta, María Trinidad Hirmas Astorga y Fabio Francisco Rosquete Cuellar

## Declaración de uso de IA

*(Por redactar: describir para qué se ha usado la IA.)*
