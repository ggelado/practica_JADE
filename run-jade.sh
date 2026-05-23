#!/bin/bash

cd "$(dirname "$0")"
AGENTS="AgenteVisualizador:agentes.vision.AgenteVisualizador;AgentePerceptor:agentes.percepcion.AgentePerceptor;AgenteClasificador:agentes.clasificador.AgenteClasificador;AgenteIncidencias:agentes.incidencias.AgenteIncidencias;AgenteSancionador:agentes.sancionador.AgenteSancionador;AgenteAnalista:agentes.analista.AgenteAnalista"

mvn package || true
mvn dependency:copy-dependencies || true

CP="target/classes:lib/*.jar"
[ -d target/dependency ] && CP="$CP:target/dependency/*"

exec java -cp "$CP" jade.Boot -gui "$AGENTS" "$@"
