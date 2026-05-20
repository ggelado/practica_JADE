# Sistema multiagente para la censura de mensajes.

Este repositorio contiene la implementación de un sistema multiagente (SMA) desarrollado en Java utilizando el framework JADE. El proyecto consiste en un moderador inteligente para servidores de Discord. Su objetivo principal es filtrar contenido visual inapropiado y detectar conductas de riesgo analizando los mensajes, automatizando parte del trabajo de administración del servidor.

## Arquitectura del Sistema

La arquitectura se divide en cuatro módulos principales que se corresponden con los bloques temáticos de la asignatura. Se ha optado por un diseño pragmático que asegura la viabilidad técnica y el cumplimiento de los requisitos de comunicación entre agentes (DF, mensajería ACL y filtros bloqueantes).

### 1. Recuperación de Información (Agente Perceptor)
Este módulo se encarga de la captura de datos desde la plataforma externa.

* **Implementación:** El agente perceptor (`DiscordListener`) utiliza la librería JDA (Java Discord API) para interactuar con Discord.
* **Optimización:** Para respetar las cuotas de la API, el agente restringe su escucha a los eventos de creación de mensajes (`on_message`), extrayendo el contenido de texto y las URLs de los archivos adjuntos en tiempo real.

### 2. Ontologías y Representación del Conocimiento (Agente Clasificador)
Proporciona la base semántica para que el sistema categorice correctamente los eventos.

* **Implementación:** La jerarquía ontológica se ha diseñado mediante Protégé, exportando el modelo a formato OWL. El agente clasificador utiliza la librería Apache Jena para cargar la ontología y realizar consultas sobre las clases definidas (ej. `Conducta_Hostil`, `Riesgo_Depresivo`, `Contenido_Explicito`).
* **Lógica de evaluación:** Los reportes de los agentes de análisis se mapean contra la ontología para deducir la gravedad de la infracción y determinar el protocolo de actuación.

### 3. Descubrimiento de Conocimiento (Agente Analista)
Realiza el procesamiento del texto para identificar comportamientos anómalos o tóxicos.

* **Implementación:** Ante la inviabilidad de entrenar un modelo de Deep Learning nativo en Java para esta práctica, el agente implementa un enfoque de análisis de sentimientos basado en lexicón (diccionarios). Evalúa la polaridad y el contenido de los mensajes buscando patrones clave asociados a discursos de odio o indicadores de vulnerabilidad emocional.
* **Generación de alertas:** Si el umbral de criticidad supera lo establecido, el agente genera un reporte de contexto y notifica a los administradores humanos mediante mensaje directo, priorizando la supervisión humana en casos de salud mental.

### 4. Percepción Computacional (Agente de Visión)
Analiza las imágenes adjuntas para evitar la distribución de contenido prohibido.

* **Implementación:** Para garantizar precisión y rendimiento, el agente `VisionAnalyzer` delega el procesamiento pesado consumiendo una API de visión externa (como Google Cloud Vision / SafeSearch). El agente extrae la URL de la imagen de Discord y realiza la petición HTTP.
* **Acción:** El agente procesa el JSON de respuesta. Si los índices de probabilidad de contenido explícito o simbología de odio son altos, se coordina mediante mensajes ACL con el `DiscordListener` para eliminar el mensaje original y notifica al Agente Clasificador para que registre la infracción.

* **Respuesta visual:** Cuando se detecta contenido inapropiado, el sistema puede publicar en el canal original una imagen bonita configurable mediante `SAFE_IMAGE_URL` en `token.env` para suavizar la intervención del bot.

## Requisitos e Instalación

*(Por redactar)*

## Instrucciones de Ejecución

*(Por redactar)*

## Identificación del Grupo
*(Grupo ns sabe todavía.)*

## Declaración de uso de IA
*(Por redactar, describir para qué se ha usado la IA.)*