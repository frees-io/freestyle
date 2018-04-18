# TodoList HTTP service using http4s

## Running the Application

```scala
sbt todolist-http-http4s/run
```

By default, it'll bootstrap in port `8081`.

## Playing with the App

There are several ways to interact with the `TodoList` RESTful API.

Here some of examples:

* (Re-)Creating tables (H2 database):

```bash
curl -X "POST" "http://localhost:8081/reset"
```

* Inserting items:

```bash
curl -X "POST" "http://localhost:8081/insert" \
     -H "Content-Type: application/json" \
     -d $'{
  "tag": {
    "name": "Tag Name",
    "id": 1
  },
  "items": [
    {
      "todoListId": 1,
      "id": 1,
      "item": "item 1",
      "completed": false
    }
  ],
  "list": {
    "title": "List title",
    "tagId": 1,
    "id": 1
  }
}'
```

* Listing items:

```bash
curl "http://localhost:8081/list"
```