
````markdown
# Integración – API REST con servicios externos (JSONPlaceholder)

**Objetivo:** Exponer una API REST que consuma y procese datos desde **JSONPlaceholder**.

- **Java:** 17  
- **Spring Boot:** 3.4.x  
- **Dependencias principales:** Web, Validation, Cache, Springdoc OpenAPI  
- **Build:** Maven

---

## Requisitos previos

- **Java 17** (`java -version`)
- **Maven 3.9+** (`mvn -v`)
- Conexión a Internet (para consumir JSONPlaceholder y descargar dependencias)

---

## Instrucciones de compilación y ejecución

### 1) Compilar

```bash
mvn clean package
````

Esto genera el JAR en `target/integracion-0.0.1-SNAPSHOT.jar`.

### 2) Ejecutar (dos opciones)

**Opción A:** con Maven

```bash
mvn spring-boot:run
```

**Opción B:** con Java (JAR)

```bash
java -jar target/integracion-0.0.1-SNAPSHOT.jar
```

La app levanta en `http://localhost:8080`.

### 3) Swagger / OpenAPI

* UI: `http://localhost:8080/swagger-ui.html`
* JSON: `http://localhost:8080/v3/api-docs`

> Si no abre `swagger-ui.html`, probar `http://localhost:8080/swagger-ui/index.html`.

---

## Configuración (externalizada)

Archivo: `src/main/resources/application.properties`

```properties
# URL base de la API externa
external.jsonplaceholder.base-url=https://jsonplaceholder.typicode.com

# Timeouts (ms)
http.connectTimeout=3000
http.readTimeout=8000

# Cache en memoria simple
spring.cache.type=simple

# Logging
logging.level.root=INFO
logging.level.com.challenge.integracion=DEBUG

# Swagger
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

```



---

## Descripción de la arquitectura

```
com.challenge.integracion
├─ config/
│  └─ RestTemplateConfig.java
├─ controller/
│  └─ PostController.java
├─ dto/
│  └─ MergedPostResponse.java
├─ exception/
│  ├─ ApiError.java
│  └─ ApiExceptionHandler.java
├─ model/placeholder/
│  ├─ Post.java
│  ├─ User.java
│  └─ Comment.java
├─ service/
│  ├─ ExternalPlaceholderClient.java
│  └─ PostService.java
└─ IntegracionApplication.java
```

### Capas y responsabilidades

* **Controller (PostController)**
  Expone endpoints REST (`/posts` y `/posts/{id}`), valida entradas (`@Validated`, `@Min(1)`), y delega la lógica.

* **Service (PostService)**
  Orquesta llamadas a la API externa (a través del client), mergea datos (`posts` + `users` + `comments`) y construye la respuesta `MergedPostResponse`.

* **Client (ExternalPlaceholderClient)**
  Encapsula las llamadas HTTP a JSONPlaceholder usando `RestTemplate`.
  Implementa **caché** para `users` y `comments` con `@Cacheable`.

* **DTOs / Modelos (records)**

    * `model.placeholder.*` mapea la respuesta externa.
    * `dto.MergedPostResponse` es el modelo de salida ya mergeado para el cliente.

* **Configuración (RestTemplateConfig)**
  Define el `RestTemplate` con **timeouts configurables** y `ClientHttpRequestFactory`.

* **Manejo de errores (ApiExceptionHandler)**
  `@ControllerAdvice` centraliza errores (HTTP externos, timeouts, validaciones, genéricos) y devuelve `ApiError` consistente.

---

## Endpoints y ejemplos de uso

### 1) GET `/posts`  *(endpoint principal)*

**Funcionalidad:**

* Llama a JSONPlaceholder:

    * `/posts`
    * `/users` *(1 sola vez; cacheado)*
    * `/comments` *(1 sola vez; cacheado)*
* Mergea:

    * `post.userId` ↔ `user.id`
    * `comment.postId` ↔ `post.id`

**Ejemplo con curl:**

```bash
curl -s http://localhost:8080/posts | jq '.[0]'
```

**Respuesta (ejemplo resumido):**

```json
{
  "id": 1,
  "title": "sunt aut facere repellat provident occaecati",
  "body": "quia et suscipit ...",
  "user": {
    "id": 1,
    "name": "Leanne Graham",
    "username": "Bret",
    "email": "Sincere@april.biz"
  },
  "comments": [
    {
      "id": 1,
      "name": "id labore ex et quam laborum",
      "email": "Eliseo@gardner.biz",
      "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos..."
    }
  ]
}
```

> Nota: El merge devuelve **todos los posts**, cada uno con su **autor** y su **lista de comentarios**.

---

### 2) DELETE `/posts/{id}`  *(endpoint secundario)*

**Funcionalidad:**

* Reenvía DELETE a `https://jsonplaceholder.typicode.com/posts/{id}`.
* **Validación**: `id >= 1`.
* **Nota**: JSONPlaceholder no persiste cambios; simula la operación.

**Ejemplos con curl:**

```bash
# OK
curl -i -X DELETE http://localhost:8080/posts/1

# Error de validación (id inválido)
curl -i -X DELETE http://localhost:8080/posts/0
```

**Respuesta error 400 (ejemplo):**

```json
{
  "timestamp": "2025-09-29T19:39:52.85Z",
  "status": 400,
  "error": "Bad Request",
  "message": "deletePost.id: must be greater than or equal to 1",
  "path": "/posts/0"
}
```

---

## Manejo de errores

`ApiExceptionHandler` traduce excepciones a una estructura común `ApiError`:

* **Errores HTTP remotos** (`HttpStatusCodeException`) → status original de la API externa.
* **Timeouts / IO** (`ResourceAccessException`) → `504 Gateway Timeout`.
* **Validaciones** (`MethodArgumentNotValidException`, `ConstraintViolationException`) → `400 Bad Request`.
* **Errores REST genéricos** (`RestClientException`) → `502 Bad Gateway`.
* **Otros no controlados** (`Exception`) → `500 Internal Server Error`.

**Formato `ApiError`:**

```json
{
  "timestamp": "2025-09-29T19:39:52.85Z",
  "status": 400,
  "error": "Bad Request",
  "message": "detalle...",
  "path": "/ruta"
}
```

---

## Logging

* Nivel por defecto: `INFO` global, `DEBUG` para el paquete del proyecto.
* Se loguea entrada a endpoints, llamadas HTTP externas y errores.

Para ver más del motor de caché:

```properties
logging.level.org.springframework.cache=DEBUG
```

---

## Caché

* Implementado con `spring.cache.type=simple` (ConcurrentMap en memoria).
* **Entradas cacheadas**: `users`, `comments`.
* **No** tiene TTL ni persistencia: se limpia al reiniciar la app.

**Para Verificar que funciona:**

1. Hacer `GET /posts`: ver logs de `posts`, `users`, `comments`.
2. Hacer de nuevo `GET /posts`: ver `posts`, **pero no `users`/`comments`** (se obtienen desde caché).

---

## Decisiones técnicas tomadas

1. **Spring Boot + RestTemplate**

    * Elegido por **familiarización y conocimiento**.
    * Timeouts configurados vía `ClientHttpRequestFactory`.
    * Alternativa moderna: `RestClient`/`WebClient`.

2. **Bulk fetch y merge en memoria**

    * El enunciado sugiere “para cada post obtener…”, pero hacer una llamada por post no escala.
    * **Decisión**: llamar una sola vez a `/users` y a `/comments`, agrupar por `id`/`postId` y **mergear**.
    * Funcionalmente **equivalente** y **más eficiente** (menos requests).

3. **Caché simple**

    * `@Cacheable` en `users` y `comments` para evitar llamadas repetidas.
    * Con `spring.cache.type=simple` se mantiene la implementación básica y confiable.

4. **Records** (`record`) para modelos**

    * Inmutables, concisos, con `equals/hashCode/toString` generados automáticamente.
    * Reducción de boilerplate y más legibilidad.

5. **Manejo de errores centralizado**

    * `@ControllerAdvice` para respuestas y seguimiento en logs.
    * Mapeo explícito de validaciones y errores.

6. **Validaciones**

    * `@Min(1)` en path variable de DELETE + `@Validated` a nivel controller.
    * Evita pegar a la API con entradas inválidas.

7. **Swagger/OpenAPI**

    * Auto-documentación lista para usar desde `/swagger-ui.html`.

8. **Performance**

    * Se optimiza con bulk + caché.
    * **No** se agrega concurrencia.
    * Se deja documentado como **mejora futura**.

---

##  Ideas de Mejoras futuras (no requeridas)

* **Paralelizar** las 3 llamadas externas (`posts`, `users`, `comments`) con `@Async`.
* **Cache con TTL**  o distribuido.
* **Tests**:

    * `@RestClientTest` para `PostService` con `MockRestServiceServer`.
    * `@WebMvcTest` para `PostController`.
* **Paginación / filtros** en `GET /posts` (por usuario o por título).

---

## Comandos útiles

```bash
# Run
mvn spring-boot:run

# Probar GET
curl -s http://localhost:8080/posts | jq '.[0]'

# Probar DELETE OK
curl -i -X DELETE http://localhost:8080/posts/1

# Probar DELETE inválido
curl -i -X DELETE http://localhost:8080/posts/0

# Swagger
# Windows
start http://localhost:8080/swagger-ui.html
# macOS
# open http://localhost:8080/swagger-ui.html
```

---

