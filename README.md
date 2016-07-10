
### Step 1

    ./docker-run sbt \"runMain steps.Step1App\"

### Step 2

    ./docker-run sbt \"runMain steps.Step2App\"

### Step 3

    ./docker-run sbt \"runMain steps.Step3App\"

### Steps 4 to 7

   - Console for the temperature sensor:

        DOCKER_OPTS="--name temperature-sensor" ./docker-run sbt \"runMain sensors.TemperatureSensor\"

   - Console for the humidity sensor:

        DOCKER_OPTS="--name humidity-sensor" ./docker-run sbt \"runMain sensors.HumiditySensor\"

   - Console for the Play server:

        DOCKER_OPTS="--link temperature-sensor --link humidity-sensor -p 9000:9000" ./docker-run sbt run

Open a web browser to [http://localhost:9000](http://localhost:9000)

Modify the superclass of [AppController](app/controllers/AppController.scala) to switch steps.
