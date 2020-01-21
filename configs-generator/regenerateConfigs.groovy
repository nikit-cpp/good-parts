def string = """
This is Groovy config generator
"""

println string


// usage:
// ./mvnw -pl configs-generator generate-resources
// after usage you should do Build -> Rebuild project
// documentation https://github.com/groovy/GMavenPlus/wiki


def BACKEND_MAIN_YML_FILE = "${project.basedir}/../backend/src/main/resources/config/application.yml";
def BACKEND_TEST_YML_FILE = "${project.basedir}/../backend/src/test/resources/config/application.yml";
def INTEGRATION_TEST_YML_FILE = "${project.basedir}/../webdriver-test/src/test/resources/config/application.yml";

class ExportedConstants {
    public static final def PROD_PORT = 8080
    public static final def TEST_PORT = 9080
    public static final def TEST_SMTP_PORT = 3025 // this is greenmail requirement
    public static final def TEST_IMAP_PORT = 3143
    public static final def TEST_EMAIL_USERNAME = "testEmailUsername"
    public static final def TEST_EMAIL_PASSWORD = "testEmailPassword"
    public static final def SCHEME = 'http'
}

def AUTOGENERATE_SNIPPET =
"""# This file was autogenerated via configs-generator
# Please do not edit it manually.
""";

def writeAndLog(filePath, content) {
    def file = new File(filePath);
    file.withWriter('UTF-8') { writer ->
        writer.write(content)
    }
    println("""File ${file.canonicalPath} was successfully saved!""");
};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////// common snippets //////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def DATA_STORE_SNIPPET = {boolean dropFirst ->
return """
spring.datasource:
    name: blog_ds
    type: org.apache.tomcat.jdbc.pool.DataSource
    # https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
    url: jdbc:postgresql://172.22.0.2:5432/blog?connectTimeout=10&socketTimeout=40
    username: blog
    password: "blogPazZw0rd"
    driverClassName: org.postgresql.Driver
    # https://docs.spring.io/spring-boot/docs/2.0.0.M7/reference/htmlsingle/#boot-features-connect-to-production-database
    # https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html#Common_Attributes
    # https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-connect-to-production-database
    tomcat:
      minIdle: 4
      maxIdle: 8
      maxActive: 10
      maxWait: 60000
      testOnBorrow: true
      testOnConnect: true
      testWhileIdle: true
      timeBetweenEvictionRunsMillis: 5000
      validationQuery: SELECT 1;
      validationQueryTimeout: 4
      logValidationErrors: true

# https://docs.spring.io/spring-boot/docs/2.0.0.M7/reference/htmlsingle/#howto-execute-flyway-database-migrations-on-startup
# https://flywaydb.org/documentation/configfiles
spring.flyway:
  locations: ${dropFirst ? 'classpath:/db/migration, classpath:/db/demo': 'classpath:/db/migration'}
  drop-first: ${dropFirst}
  schemas: migrations, auth, posts, images, settings
  out-of-order: true

spring.redis.url: redis://172.22.0.3:6379/0
spring.data.redis.repositories.enabled: false

spring:
  elasticsearch:
    rest:
      uris: 172.22.0.5:9200
      read-timeout: 30s
      connection-timeout: 5s
spring.data.elasticsearch.repositories.enabled: false
"""};

def MANAGEMENT_SNIPPET = { boolean test ->

"""
management.endpoints.web.exposure.include: '*'
management.endpoint.health.show-details: always
management:
  server:
    port: ${test?'3011':'3010'}
    ssl:
      enabled: false
    add-application-context-header: false
"""
}

def WEBSERVER_SNIPPET =
"""
server.tomcat.basedir: \${java.io.tmpdir}/com.github.nkonev.tomcat
server.servlet.session.store-dir: \${server.tomcat.basedir}/sessions
# For pretty output in tests
spring.http.encoding.force-response: true
spring.servlet.multipart.max-file-size: 6MB
spring.servlet.multipart.max-request-size: 8MB
""";

def TEST_USERS_SNIPPET=
"""custom.it.user: admin
custom.it.password: admin
""";

def common = { boolean test ->
"""
custom.base-url: "${ExportedConstants.SCHEME}://localhost:\${server.port}"

# https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-email.html
# https://yandex.ru/support/mail-new/mail-clients.html
# https://stackoverflow.com/questions/411331/using-javamail-with-tls
spring.mail:
  testConnection: false
  host: ${ !test ? "smtp.yandex.ru" : "127.0.0.1"} 
  port: ${ !test ? 465 : ExportedConstants.TEST_SMTP_PORT}
  username: ${ !test ? "username" : ExportedConstants.TEST_EMAIL_USERNAME} 
  password: ${ !test ? "password" : ExportedConstants.TEST_EMAIL_PASSWORD} 
  properties:
    # mail.smtp.starttls.enable: "true"
    ${ (!test ? '' : '# ') + 'mail.smtp.ssl.enable: "true"'}
    mail.smtp.connectiontimeout: 5000
    mail.smtp.timeout: 3000
    mail.smtp.writetimeout: 5000

custom.stomp.broker:
  host: "172.22.0.4"
  port: 61613
  virtual-host: /
  client-login: blog
  client-password: blogPazZw0rd
  system-login: blog
  system-password: blogPazZw0rd

# Postgres image store configuration
custom.image:
  max-bytes: 4194304 # 4 Mb. Must be < than tomcat file upload limit. see also spring.servlet.multipart.*
  allowed-mime-types:
   - image/png
   - image/jpg
   - image/jpeg
  # value in seconds, passed in Cache-Control header
  max-age: 31536000
# If post title image is empty - it set first image from content to title automatically
custom.set.first.image.as.title: true
spring.session.timeout: 2d
"""
}

def custom(boolean test) {
    def str = """
custom:
  email:
    from: ${!test ? '"username@yandex.ru"' : ExportedConstants.TEST_EMAIL_USERNAME+'@test.example.com'} 
  registration:
    email:  
      subject: "Registration confirmation"
      text-template: "Please open __REGISTRATION_LINK_PLACEHOLDER__ for complete registration __LOGIN__."
  confirmation:
    registration:
      token:
        ttl-minutes: 5
  password-reset:
    email:
      subject: "Password reset"
      text-template: "Link __PASSWORD_RESET_LINK_PLACEHOLDER__ for reset your password for account __LOGIN__. If you didn't issue password reset -- you can ignore this mail."
    token:
      ttl-minutes: 5
  tasks:
    enable: ${!test}
    poolSize: 10
    defaultLockAtMostForSec: 20
    defaultLockAtLeastForSec: 20
    images.clean:
      cron: "0 * * * * *"
    rendertron.cache.refresh:
      cron: "0 */30 * * * *"
    elasticsearch.refresh:
      cron: "0 0 */2 * * *"
  rendertron:
    serviceUrl: http://rendertron.example.com:3000/
  xss:
    iframe:
      allow:
        src:
          pattern: '^(https://www\\.youtube\\.com.*)|(https://coub\\.com/.*)|(https://player\\.vimeo\\.com.*)|(https://asciinema\\.org.*)\$'

"""
    return str
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////// config files ///////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def BACKEND_MAIN_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.root: INFO
#logging.pattern.console: '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(\${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-50.50(%logger{49}:%line)){cyan} %clr(:){faint} %m%n%wEx'
#logging.level.org.springframework.core.env.PropertySourcesPropertyResolver: DEBUG
#logging.level.org.springframework.security.oauth2: DEBUG
logging.level.org.elasticsearch.client.RestClient: ERROR
#logging.level.org.apache.tomcat.jdbc.pool: TRACE
#logging.level.org.springframework.security: DEBUG
#logging.level.org.springframework.session: DEBUG
#logging.level.org.springframework.security.web: DEBUG
#logging.level.org.apache.catalina: TRACE
#logging.level.org.springframework.web: DEBUG
#logging.level.org.hibernate.SQL: DEBUG
#logging.level.org.hibernate.type: TRACE
${common(false)}
${custom(false)}
server.tomcat.accesslog.enabled: false
server.tomcat.accesslog.pattern: '%t %a "%r" %s (%D ms)'
server.port: ${ExportedConstants.PROD_PORT}
server.servlet.session.persistent: true
${WEBSERVER_SNIPPET}

# this is URL
spring.mvc.static-path-pattern: /**
# You need to remove "file:..." element for production or you can to remove spring.resources.static-locations
# first element - for eliminate manual restart app in IntelliJ for copy compiled js to target/classes, last slash is important,, second element - for documentation
spring.resources.static-locations: file:backend/src/main/resources/static/, classpath:/static/

${DATA_STORE_SNIPPET(false)}
${MANAGEMENT_SNIPPET(false)}

spring.security:
    oauth2:
      client:
        registration:
          vkontakte:
            client-id: 6805077
            client-secret: your-app-client-secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/api/login/oauth2/code/{registrationId}"
            client-authentication-method: post
          facebook:
            client-name: "facebook" # use in BlogOAuth2UserService
            client-id: 1684113965162824
            client-secret: your-app-client-secret
            redirect-uri: "{baseUrl}/api/login/oauth2/code/{registrationId}"
        provider:
          vkontakte:
            authorization-uri: https://oauth.vk.com/authorize
            token-uri: https://oauth.vk.com/access_token
            user-info-uri: https://api.vk.com/method/users.get?v=5.92
            user-info-authentication-method: form
            user-name-attribute: response
          facebook:
            user-info-uri: "https://graph.facebook.com/me?fields=id,name,picture"
""";
writeAndLog(BACKEND_MAIN_YML_FILE, BACKEND_MAIN_YML_CONTENT);

def BACKEND_TEST_SECURITY_SNIPPET =
"""
spring.security:
    oauth2:
      client:
        registration:
          vkontakte:
            client-id: 6805077
            client-secret: your-app-client-secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/api/login/oauth2/code/{registrationId}"
            client-authentication-method: post
          facebook:
            client-name: "facebook" # use in BlogOAuth2UserService
            client-id: 1684113965162824
            client-secret: your-app-client-secret
            redirect-uri: "{baseUrl}/api/login/oauth2/code/{registrationId}"
        provider:
          vkontakte:
            authorization-uri: http://127.0.0.1:10081/mock/vkontakte/authorize
            token-uri: http://127.0.0.1:10081/mock/vkontakte/access_token
            user-info-uri: http://127.0.0.1:10081/mock/vkontakte/method/users.get?v=5.92
            user-info-authentication-method: form
            user-name-attribute: response
          facebook:
            authorization-uri: http://127.0.0.1:10080/mock/facebook/dialog/oauth
            token-uri: http://127.0.0.1:10080/mock/facebook/oauth/access_token
            user-info-uri: http://127.0.0.1:10080/mock/facebook/me?fields=id,name,picture
"""

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def BACKEND_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.root: INFO
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: WARN
logging.level.org.apache.tomcat.util.modeler.Registry: ERROR
${common(true)}
${custom(true)}
server.port: ${ExportedConstants.TEST_PORT}
${WEBSERVER_SNIPPET}
${TEST_USERS_SNIPPET}
${DATA_STORE_SNIPPET(true)}
${MANAGEMENT_SNIPPET(true)}
custom.rendertron.enable: true
custom.rendertron.enable.async.cache.refresh: false
${BACKEND_TEST_SECURITY_SNIPPET}
""";
writeAndLog(BACKEND_TEST_YML_FILE, BACKEND_TEST_YML_CONTENT);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def WEBDRIVER_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.root: INFO
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: WARN
logging.level.org.apache.tomcat.util.modeler.Registry: ERROR
${common(true)}
${custom(true)}
server.port: ${ExportedConstants.TEST_PORT}
${WEBSERVER_SNIPPET}
# this is URL
spring.mvc.static-path-pattern: /**
# You need to remove "file:..." element for production or you can to remove spring.resources.static-locations
# first element - for eliminate manual restart app in IntelliJ for copy compiled js to target/classes, last slash is important,, second element - for documentation
spring.resources.static-locations: file:../backend/src/main/resources/static/, classpath:/static/

custom.selenium.implicitly-wait-timeout: 10
custom.selenium.browser: CHROME
custom.selenium.window-height: 900
custom.selenium.window-width: 1600
custom.selenium.selenide-condition-timeout: 10
custom.selenium.selenide-collections-timeout: 10

custom.it.url.prefix: ${ExportedConstants.SCHEME}://localhost:\${server.port}
custom.it.user.id: 1
${TEST_USERS_SNIPPET}
${DATA_STORE_SNIPPET(true)}
${MANAGEMENT_SNIPPET(true)}
custom.rendertron.enable.async.cache.refresh: false
${BACKEND_TEST_SECURITY_SNIPPET}
""";
writeAndLog(INTEGRATION_TEST_YML_FILE, WEBDRIVER_TEST_YML_CONTENT);
