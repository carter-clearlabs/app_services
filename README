build all projects
```
./gradlew clean build
```

Build just the service definitions for projects to consume

```
./gradlew service_definitions:build
```

Build just the gateway
```
./gradlew gateway:build
```

Build just the auth_service
```
./gradlew auth_service:build
```

Create Docker image for gateway
```
docker build . -t gcr.io/clearview-dev/gateway
```
Push to GCP Registry
```
gcloud docker -- push gcr.io/clearview-dev/gateway
```

Create Docker image for auth_service
```
docker build . -t gcr.io/clearview-dev/auth_service
```
Push to GCP Registry
```
gcloud docker -- push gcr.io/clearview-dev/auth_service
```

Then use UI in GCP KBE Console to update the containers to see new version.

