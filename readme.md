[![Build Status](https://travis-ci.org/nikit-cpp/blog.svg?branch=master)](https://travis-ci.org/nikit-cpp/blog)

# Requirements

* JDK 8
* Docker 1.12.3 +
* docker-compose 1.8.0 +

# Building with frontend (just turn on `frontend` profile)

There is highly recommends to shut down your application on 8080, although tests uses 8090, some of
them can fails, with websocket for example.
```
./mvnw -P frontend clean package
```

This will run tests on PhantomJS.
See `.travis.yml` for Firefox and Chrome test examples

# Building without frontend and without webdriver tests
```
./mvnw clean package
```


# Run
```
java -jar frontend/target/frontend-1.0-SNAPSHOT-exec.jar
```

# Changing version
```
./mvnw -DnewVersion=1.0.0 versions:set versions:commit
```

# Check for update maven dependency versions
```
./mvnw -DlogOutput=false -DprocessDependencyManagement=false versions:display-dependency-updates | less
./mvnw -DlogOutput=false versions:display-property-updates | less

./mvnw -DlogOutput=false versions:display-plugin-updates | less

```

# Frontend development

## Run webpack
```
cd frontend
npm run dev
```

## Run KarmaJS with PhantomJS browser (Chrome will be if not specified)
```
cd frontend
npm run unit
# or
npm run unit -- --browsers=Chrome
```

## Update js dependencies

https://www.npmjs.com/package/npm-check-updates

```
ncu -u
rm package-lock.json
npm install
```
# Request version info 

This will available after full pachage, e. g. after resources filtering of `git.template.json` and renaming result in `target/classes/ststic` dir to `git.json`

```
curl -i http://127.0.0.1:8080/git.json
```