Pruebas del Agente de Visión:

Modelos utilizados:

- https://huggingface.co/zhiwei2017/yolo11s-cls-nazi-symbols/tree/main/yolo11s-cls-nazi-symbol-detection/weights
- https://huggingface.co/Falconsai/nsfw_image_detection/tree/main
- https://github.com/Musawer1214/Fight-Violence-detection-yolov8

Este modelo se encarga de la detección de simbología nazi. Se trata de un modelo preentrenado (no lo hemos entrenado
nosotros).

El directorio con contenido de ejemplo (testDir) no es subido al repositorio para cumplir con las Comunity Guidelines
de GitHub.

---

# ¿Cómo ejecutar? (instrucciones para bash, bte similares en otros SO)


WIP: Imagen docker

1. Configurar un entorno virtual de python

```bash
python -m venv ./venv
```

2. Acceder al entorno virtual

```bash
source ./venv/Scripts/activate
```

3. Instalar dependencias


```bash
pip install -r requirements.txt
```

4. Editar variable de entorno poniendo la ruta del *python virtual*

---

# ¿Cómo ejecutar? (instrucciones para Docker)

```bash
# Construir la imagen
docker build -t vision-model .

# Ejecutar el contenedor
docker run vision-model

# O con volúmenes (sintáxis windows)
docker run -v %cd%\output:/app/output vision-model
```