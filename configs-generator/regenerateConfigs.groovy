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

def FRONT_CONFIGURATION_SNIPPET =
"""custom.frontend:
    titleTemplate: "%s | owner's blog"
    header: "Owner's blog"
"""

def DATA_STORE_SNIPPET = {String contexts, boolean dropFirst, String ddlAuto ->
return """
spring.jpa:
  properties:
    hibernate.use_sql_comments: true
    hibernate.format_sql: true
    hibernate.generate_statistics: true
  hibernate.ddl-auto: ${ddlAuto}

spring.datasource:
    # https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
    url: jdbc:postgresql://172.22.0.2:5432/blog?connectTimeout=10&socketTimeout=40
    username: blog
    password: "blogPazZw0rd"
    driverClassName: org.postgresql.Driver
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

spring.liquibase:
  change-log: classpath:liquibase/migration.yml
  contexts: ${contexts}
  drop-first: ${dropFirst}

spring.redis.url: redis://172.22.0.3:6379/0
spring.data.redis.repositories.enabled: false
# Also see index in bootstrap.sql
custom.postgres.fulltext.reg-config: "'russian'::regconfig"
"""};

def MANAGEMENT_SNIPPET = { boolean test ->

"""
management:
  server:
    port: ${test?'3011':'3010'}
    ssl:
      enabled: false
    add-application-context-header: false
  security:
    enabled: false
"""
}

def WEBSERVER_SNIPPET =
"""
server.tomcat.basedir: \${java.io.tmpdir}/com.github.nkonev.tomcat
server.session.store-dir: \${server.tomcat.basedir}/sessions
""";

def TEST_USERS_SNIPPET=
"""custom.it.user: admin
custom.it.password: admin
""";

def common = { boolean test ->
"""
custom.base-url: "${ExportedConstants.SCHEME}://127.0.0.1:\${server.port}"

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
  max-bytes: 1048576 # 1 Mb. Must be < than tomcat file upload limit
  allowed-mime-types:
   - image/png
   - image/jpg
   - image/jpeg
  # value in seconds, passed in Cache-Control header
  max-age: 31536000
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
"""
    return str
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////// config files ///////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def BACKEND_MAIN_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO
logging.level.org.springframework.web.socket: WARN
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: WARN
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
server.session.persistent: true
${WEBSERVER_SNIPPET}

# this is URL
spring.mvc.static-path-pattern: /**
# You need to remove "file:..." element for production or you can to remove spring.resources.static-locations
# first element - for eliminate manual restart app in IntelliJ for copy compiled js to target/classes, last slash is important,, second element - for documentation
spring.resources.static-locations: file:backend/src/main/resources/static/, classpath:/static/

${DATA_STORE_SNIPPET('main', false, 'validate')}
${MANAGEMENT_SNIPPET(false)}
${FRONT_CONFIGURATION_SNIPPET}
""";
writeAndLog(BACKEND_MAIN_YML_FILE, BACKEND_MAIN_YML_CONTENT);


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def BACKEND_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: WARN
${common(true)}
${custom(true)}
server.port: ${ExportedConstants.TEST_PORT}
${WEBSERVER_SNIPPET}
${TEST_USERS_SNIPPET}
${DATA_STORE_SNIPPET('main, test', true, 'validate')}
${MANAGEMENT_SNIPPET(true)}
${FRONT_CONFIGURATION_SNIPPET}
""";
writeAndLog(BACKEND_TEST_YML_FILE, BACKEND_TEST_YML_CONTENT);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def WEBDRIVER_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: WARN
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
custom.selenium.browser: PHANTOM
custom.selenium.window-height: 900
custom.selenium.window-width: 1600
custom.selenium.selenide-condition-timeout: 10
custom.selenium.selenide-collections-timeout: 10

custom.it.url.prefix: ${ExportedConstants.SCHEME}://127.0.0.1:\${server.port}
custom.it.user.id: 1
${TEST_USERS_SNIPPET}
${DATA_STORE_SNIPPET('main, test', true, 'none')}
${MANAGEMENT_SNIPPET(true)}
${FRONT_CONFIGURATION_SNIPPET}
""";
writeAndLog(INTEGRATION_TEST_YML_FILE, WEBDRIVER_TEST_YML_CONTENT);
