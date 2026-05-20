#!/bin/bash

# Script lanzador de JADE
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Construir classpath con todas las dependencias
CLASSPATH="target/classes:lib/jade.jar:lib/commons-codec-1.3.jar"

for jar in target/lib/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Lanzar JADE
java -cp "$CLASSPATH" jade.Boot -gui \
    AgenteVisualizador:agentes.vision.AgenteVisualizador \
    AgentePerceptor:agentes.percepcion.AgentePerceptor \
    AgenteClasificador:agentes.clasificador.AgenteClasificador \
    AgenteIncidencias:agentes.incidencias.AgenteIncidencias
