## Producto
Decidí invertir un poco del tiempo en pensar como se debería ver y sentir una app de este tipo, armando una paleta de colores acorde y siguiendo una estética bien definida, armando algo parecido a un design system. Siento que la app es lo primero que ve un usuario al elegir un producto, y la primera impresión tiene que ser excelente. Puede ser el producto más eficiente y rápido del mercado, pero si no se ve y siente bien, el usuario puede sentirse insatisfecho y elegir otro producto.

Al tener un catálogo de componentes definidos de antemano, es mucho más trivial el ida y vuelta con el LLM que se use para desarrollar. Nos ayuda a enfocarnos en el _qué_ en vez de en el _como debería verse_, ya que fue definido de antemano.

## Stack
Dado que el tiempo era bastante acotado y que el enunciado pedía no construir para iOS , elegí Android nativo como stack, que casualmente es mi punto fuerte. Creo que también servirá para contrastar con el prototipo que ya tienen construido para ver como se siente una app nativa en comparación a una solución híbrida (quizas ni siquiera se noten diferencias dada la pequeña cantidad de funcionalidades que se ofrecen en este demo). Se pierde el "code once, run everywhere", y quedaríamos atados a los tiempos de revisión del PlayStore para lanzar updates en lugar de poder distribuirse mediante OTA updates, pero se gana en fluidez y en facilidad para usar el hardware del teléfono, entre otros.

## AI Stack
* Claude Artifacts (Design) para definición de la paleta de colores/ font system, componentes básicos e iterar el diseño de las pantallas principales.
* Claude Code para el resto.

## STT/TTS
Estoy usando ElevenLabs tanto para STT como para TTS porque me pareció bastante superior en comparación a TextToSpeech nativo, tanto en cuanto a tono y variedad de voces, y en cuanto al output que generan. La única desventaja es que necesitaríamos conexión a internet para poder hacer las transcripciones. El código quedó bastante flexible para poder cambiar de providers en el futuro sin tener que dar velta la aplicación. A futuro se podría también switchear modelos en runtime dependiendo el estado de red/tipo de cuenta del cliente, por ejemplo usar modelos más caros solo en usuarios premium.

## LLM
Elegí usar las soluciones de OpenAI (Function Calling) por mi familiaridad con la tecnología y porque ofrece una solución robusta y bastante battle-tested + SDK nativo en Java.

## Costos
No se analizaron costos para el scope del entregable. Siento que para la escala de la aplicación (2/3 usuarios durante su primera y seguramente única semana de vida) no valía la pena analizar otros proveedores.

## Tradeoffs
Para agilizar los tiempos de desarrollo, le estamos pegando a las APIs de ElevenLabs y OpenAI directamente desde el cliente. Esto significa que las API keys se encuentran compiladas dentro del código, y es bastante trivial decompilar un APK y revisar los strings. Esto podría mitigarse parcialmente generando la versión en modo release, pero no deja de ser un riesgo grande. El cliente sin dudas debería pegarle a un backend nuestro que se encargue de elegir el provider adecuado, tenga sus guardrails, su observabilidad, alertas, etc. para no depender de un update de la aplicación para poder switchear en realtime.

## Escalabilidad
Si bien todo corre localmente y estamos consumiendo world-class APIs que no deberían tener problemas de escalabilidad, si se decidiera shippear esto al mundo exterior, habría que estar muy atentos al consumo de las API keys, ya que podría dispararse la factura si hay miles de usuarios (o se filtran las keys por lo dicho en el punto anterior).

## Out of scope
En un principio había pensado la app para que funcionara sin conexión y pudiera sincronizar los mensajes no enviados al momento de recuperar la conectividad, pero a la mitad de tiempo de empezar a desarrollar me dí cuenta que no iba a llegar con el tiempo dado. Decidí priorizar que funcionaran todas las features que pedía el enunciado y dejé afuera cosas como:
* Poder escribirle al Agente en vez de comunicarse únicamente por voz.
* Opción de deshacer un cambio hecho por el agent.
* Storear audios/transcripciones que hubieran fallado en algún punto (stt/pegada a openAI) para reintentar más adelante (ya sea manualmente o automáticamente al recuperar la conexión, si hubiera sido el caso), con exp. backoff y número máximo de retries.
* Datos de Temperatura/humedad/nombre de usuario en la home hardcodeados.
* Múltiples campos.
