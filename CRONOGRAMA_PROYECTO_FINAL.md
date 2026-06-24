**==> picture [81 x 105] intentionally omitted <==**

**----- Start of picture text -----**<br>
ai<br>es<br>PUCE<br>**----- End of picture text -----**<br>


PONTIFICIA UNIVERSIDAD CATÓLICA DEL ECUADOR 

## PROGRAMACIÓN MÓVIL 

INGENIERÍA EN SISTEMAS DE INFORMACIÓN 

Cronograma Proyecto Final 

Integrantes Anahí Berrú, Jorge Jara Kelvin Piñero Ignacio Masaquiza Danny Yánez 

31/03/2026 

## Contents 

**Introducción:** ............................................................................................................ 3 **Público Objetivo** ........................................................................................................ 3 **Funcionalidades:** ....................................................................................................... 4 **Wireframes** ............................................................................................................... 5 **Pantalla de Asistencia** ............................................................................................. 5 **Pantalla de Calificaciones** ....................................................................................... 6 **Pantalla de Reporte de Incidentes** ........................................................................... 7 **Modelado de Estado Inicial** ........................................................................................ 7 **Cronograma de Integración Tecnológica** ..................................................................... 8 **Componente de IA** ..................................................................................................... 9 **Estrategia de Datos** .................................................................................................. 10 **Stack Tecnológico** .................................................................................................... 11 

## **Introducción:** 

La comunicación entre docentes, autoridades y representantes dentro de las instituciones educativas constituye un elemento fundamental para garantizar un adecuado seguimiento académico y disciplinario de los estudiantes. Sin embargo, en muchos casos los procesos de envío de reportes, registro de asistencia y notificaciones continúan realizándose de manera manual o mediante plataformas que son difíciles de entender, lo que puede ocasionar retrasos, pérdida de información y falta de confirmación en la recepción de mensajes importantes. 

En este contexto surge _AlegriApp_ , una aplicación móvil diseñada para optimizar la gestión y comunicación institucional dentro de las unidades educativas de Fe y Alegría. La propuesta busca centralizar procesos esenciales como el registro de asistencia, la gestión de calificaciones y el reporte de incidentes, incorporando mecanismos de confirmación de lectura y entrega mediante canales digitales efectivos como Telegram. De esta manera, se pretende mejorar la rapidez, confiabilidad y trazabilidad de la información compartida entre docentes y autoridades. 

El desarrollo del proyecto contempla el uso de tecnologías modernas orientadas al desarrollo móvil, como Kotlin y Jetpack Compose, implementando una arquitectura basada en MVVM para mantener una estructura organizada, escalable y mantenible. Además, se integrarán funcionalidades de inteligencia artificial enfocadas en la transcripción automática de hojas de asistencia a registros digitales, así como estrategias de persistencia de datos mediante almacenamiento local y sincronización con servicios en línea cuando exista conexión a internet. 

A través de esta solución tecnológica, se busca contribuir a la transformación digital de los procesos educativos, facilitando el trabajo administrativo y académico de los docentes, reduciendo errores en el manejo de información y fortaleciendo la comunicación institucional de manera eficiente y accesible. 

## **Público Objetivo** 

## 

El sistema AlegriApp está orientado principalmente a instituciones educativas pertenecientes a Fe y Alegría, buscando facilitar la comunicación y gestión académica entre docentes y autoridades. 

Público Objetivo Descripción 

|Docentes|Usuarios encargados de registrar asistencia, ingresar calificaciones,<br>reportar incidentes y enviar información académica mediante la<br>aplicación.|
|---|---|
|Autoridades<br>Institucionales|Responsables de supervisar la información académica y disciplinaria<br>generada dentro de la institución, además de dar seguimiento a reportes e<br>incidencias.|
|Instituciones Fe y<br>Alegría|Organizaciones educativas que requieren una herramienta digital para<br>optimizar procesos de comunicación, control académico y gestión de<br>información.|



## **Funcionalidades:** 

|**Funcionalidades:**||
|---|---|
|Funcionalidad|Descripción|
|Pantalla de Asistencia<br>(_AttendanceScreen_)|Permite a los docentes registrar la asistencia de los estudiantes de<br>manera rápida y organizada. La información puede almacenarse<br>localmente cuando no exista conexión a internet y sincronizarse<br>posteriormente con la base de datos mediante una API. Además, se<br>contempla la integración de inteligencia artificial para digitalizar<br>hojas de asistencia físicas.|
|Pantalla<br>de<br>Notas<br>(_GradesScreen_)|Facilita el registro y consulta de calificaciones académicas de los<br>estudiantes. Los docentes podrán ingresar, actualizar y visualizar<br>notas de manera estructurada, permitiendo mantener un mejor<br>control del rendimiento académico y agilizando la gestión de<br>información.|
|Pantalla de Reporte de<br>Incidentes<br>(_IncidentScreen_)|Permite registrar incidentes o novedades relacionadas con<br>estudiantes dentro de la institución. Los reportes podrán enviarse<br>mediante Telegram para garantizar la entrega y confirmación de|



lectura de la información por parte de las autoridades correspondientes. 

## **Wireframes** 

A continuación, se presentan los diseños iniciales de las principales interfaces de AlegriApp. Estas pantallas fueron diseñadas con el objetivo de visualizar un prototipo para el diseño final de la aplicación utilizando JetPackCompose 

## **Pantalla de Asistencia** 

Figura 1,2 Ventana de asistencia 

La pantalla de asistencia permite al docente registrar el estado de asistencia de cada estudiante mediante opciones rápidas como “Presente”, “Tardanza” y “Ausente”. Además, la interfaz muestra el progreso de registros realizados dentro del curso seleccionado y permite enviar el reporte correspondiente de manera inmediata. 

## **Pantalla de Calificaciones** 

Figura 3,4 Ventana Calificaciones 

La interfaz de calificaciones permite seleccionar la materia y período académico correspondiente para registrar las notas de los estudiantes. El sistema calcula automáticamente el promedio general de la sección y facilita el envío del boletín académico mediante integración con Telegram. 

## **Pantalla de Reporte de Incidentes** 

Figura 5,6 Ventana Reportes 

La pantalla de incidentes está diseñada para registrar novedades relacionadas con estudiantes dentro de la institución. El docente puede seleccionar el tipo de incidente, describir detalladamente lo ocurrido y enviar el reporte directamente a autoridades y representantes mediante Telegram, garantizando la entrega y trazabilidad de la información. 

## **Modelado de Estado Inicial** 

Para garantizar una correcta gestión de la información y de los eventos dentro de la aplicación, se implementará un modelo de estados basado en sealed classes en Kotlin. Este enfoque permitirá manejar de forma organizada los diferentes escenarios que pueden ocurrir durante la ejecución de la interfaz de usuario, mejorando la mantenibilidad y control de la aplicación. 

La primera sealed class estará orientada al manejo de estados de carga de información dentro de la interfaz. Su propósito será controlar procesos relacionados con la obtención y visualización de datos, como listas de estudiantes, asistencias o calificaciones. Esta clase contará con los siguientes estados: 

- Loading: representa el estado en el que la información se encuentra en proceso de carga. 

- Success: indica que los datos fueron obtenidos correctamente y pueden mostrarse en la interfaz. 

- Error: representa un fallo durante la carga de información, permitiendo mostrar mensajes de error al usuario. 

Adicionalmente, se implementará una segunda sealed class enfocada en controlar estados relacionados con acciones específicas dentro de la aplicación, especialmente aquellas asociadas al envío de información mediante la API de Telegram. Esta clase incluirá los siguientes estados: 

- Idle: estado inicial en el que no se está ejecutando ninguna acción. 

- Sending: indica que la información está siendo enviada al servidor o servicio externo. 

- Success: confirma que el envío de datos se realizó correctamente. 

- Error: representa un error ocurrido durante el proceso de envío de información. 

Este modelo permitirá mantener un flujo de interacción más claro y predecible dentro de la aplicación, facilitando el manejo de errores, la actualización dinámica de la interfaz y una mejor experiencia para el usuario. 

## **Cronograma de Integración Tecnológica** 

El desarrollo de AlegriApp se organizará mediante una planificación incremental basada en sprints, permitiendo implementar progresivamente cada componente tecnológico del sistema. Cada sprint estará enfocado en objetivos específicos como el desarrollo de interfaces, manejo de datos, integración de servicios externos, implementación de inteligencia artificial y validación final de la aplicación 

**Sprint Objetivo Principal Actividades Tecnológicas** 

|Sprint<br>1<br>–<br>Planificación y Base<br>del Proyecto|Preparar la estructura<br>inicial del sistema|Configuración del entorno de desarrollo en<br>Kotlin,<br>creación<br>de<br>repositorio,<br>implementación inicial de Jetpack Compose<br>y definición de arquitectura MVVM.|
|---|---|---|
|Sprint 2 – Desarrollo<br>de Interfaces|Construcción de las<br>pantallas principales|Desarrollo de las pantallas de asistencia,<br>calificaciones e incidentes utilizando Jetpack<br>Compose y manejo de estados mediante<br>MutableState y sealed classes.|
|Sprint 3 – Persistencia<br>y Gestión de Datos|Implementar<br>almacenamiento<br>y<br>sincronización|Integración<br>de<br>Room<br>Database<br>para<br>almacenamiento local, manejo offline y<br>conexión con API REST para sincronización<br>de datos.|
|Sprint 4 – Integración<br>de Servicios Externos|Implementar<br>comunicación<br>y<br>notificaciones|Integración de la API de Telegram para envío<br>de reportes, confirmación de mensajes y<br>comunicación institucional.|
|Sprint<br>5<br>–<br>Implementación<br>del<br>Componente IA|Incorporar<br>funcionalidades<br>de<br>inteligencia artificial|Integración de Google ML Kit para<br>reconocimiento<br>de<br>texto<br>(OCR)<br>y<br>digitalización automática<br>de<br>hojas<br>de<br>asistencia físicas.|
|Sprint 6 – Validación<br>y Optimización Final|Realizar<br>pruebas<br>y<br>ajustes finales|Corrección de errores, optimización de<br>interfaz,<br>pruebas<br>de<br>funcionamiento,<br>validación de experiencia de usuario y<br>preparación<br>de<br>presentación<br>final<br>del<br>proyecto.|



## **Componente de IA** 

El componente de inteligencia artificial de AlegriApp estará enfocado en la digitalización automática de hojas de asistencia físicas mediante tecnologías de reconocimiento óptico de 

caracteres (OCR). Para ello, se planea utilizar Google ML Kit, una herramienta de visión artificial desarrollada por Google e integrada para aplicaciones Android. 

El funcionamiento consistirá en capturar una fotografía de la hoja de asistencia desde la aplicación móvil. Posteriormente, el modelo de reconocimiento de texto procesará la imagen para identificar nombres, registros y datos escritos dentro del documento físico, permitiendo transformar automáticamente la información en registros digitales dentro del sistema. 

Esta funcionalidad busca reducir el tiempo invertido en el ingreso manual de información, minimizar errores humanos y facilitar la gestión académica de los docentes. Además, al integrarse con el sistema de almacenamiento local y sincronización en línea, la información podrá mantenerse disponible incluso en escenarios con conectividad limitada. 

## **Estrategia de Datos** 

La estrategia de manejo de datos de AlegriApp estará enfocada en garantizar la disponibilidad, integridad y sincronización de la información académica y administrativa, incluso en entornos con conectividad limitada. Para ello, se implementará un modelo híbrido de almacenamiento local y sincronización remota. 

Inicialmente, la aplicación permitirá registrar información de manera offline utilizando una base de datos local mediante Room, una librería de persistencia recomendada para aplicaciones Android. Esto permitirá que los docentes puedan continuar registrando asistencias, calificaciones e incidentes aun cuando no dispongan de conexión a internet. 

Una vez que el dispositivo recupere conectividad, el sistema realizará automáticamente la sincronización de los datos almacenados localmente hacia una base de datos remota mediante el consumo de una API REST. De esta manera, se garantizará la actualización y centralización de la información institucional sin afectar la experiencia del usuario. 

Además, dentro de la programación de la aplicación se implementará un manejo adecuado de estados de pantalla y persistencia temporal de información, con el objetivo de evitar la pérdida de datos durante cambios de interfaz, recargas de componentes o interrupciones inesperadas de la aplicación. Esto permitirá mantener una experiencia de usuario más estable, segura y confiable durante el uso del sistema. 

## **Stack Tecnológico** 

El desarrollo de AlegriApp se realizará utilizando tecnologías modernas orientadas al desarrollo de aplicaciones móviles Android, priorizando la escalabilidad, mantenibilidad y eficiencia del sistema. 

Para el desarrollo principal de la aplicación se utilizará Kotlin, lenguaje oficial recomendado por Google para aplicaciones Android, debido a su seguridad, compatibilidad y facilidad para implementar arquitecturas modernas. La interfaz gráfica será desarrollada mediante Jetpack Compose, permitiendo construir interfaces declarativas, dinámicas y adaptables de manera más eficiente. 

La arquitectura de la aplicación estará basada en el patrón MVVM (Model - View - ViewModel), con el objetivo de separar la lógica de negocio, la gestión de estados y la interfaz de usuario, facilitando el mantenimiento y escalabilidad del proyecto. 

Figura 7 Arquitectura MVVM 

En cuanto al manejo de datos locales, se implementará Room Database para permitir almacenamiento offline y sincronización posterior con el servidor cuando exista conexión a internet. La comunicación con servicios externos y backend se realizará mediante el consumo de una API REST. 

Para la gestión de estados de interfaz y reactividad de la aplicación se utilizarán herramientas como MutableState y manejo de estados mediante _sealed classes_ , permitiendo actualizar dinámicamente la interfaz de usuario según las acciones realizadas. 

Adicionalmente, el proyecto integrará Google ML Kit como componente de inteligencia artificial para realizar reconocimiento óptico de caracteres (OCR) en hojas de asistencia físicas, permitiendo su digitalización automática. 

Figura 8 Herramienta de Google ML Kit 

Finalmente, para el sistema de notificaciones y envío de reportes se utilizará la API de Telegram, garantizando la entrega y confirmación de mensajes hacia docentes, autoridades y representantes institucionales. 

