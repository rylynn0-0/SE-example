# ============================================================
# 后端镜像：多阶段构建（Multi-stage build）
#   阶段 1（build）：用带 Maven + JDK17 的镜像把源码编译成 jar
#   阶段 2（运行）：只保留 JDK17 运行时 + jar 包
# 好处：最终镜像里没有 Maven 和源码，体积从 ~800MB 降到 ~250MB
# ============================================================

# ---------- 阶段 1：编译 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝依赖清单 + Maven 镜像配置，再下载依赖。
# 这样只要 pom.xml / settings.xml 没变，Docker 就会复用这一层缓存，
# 后续改 Java 代码不会重新下载依赖。
COPY pom.xml .
COPY settings.xml .
RUN mvn -B -q -s settings.xml dependency:go-offline

# 再拷贝源码并打包（-DskipTests 跳过测试，加快首次构建）
# -s settings.xml 表示使用阿里云镜像，否则会去 Maven Central 下载，国内极慢
COPY src ./src
RUN mvn -B -q -s settings.xml clean package -DskipTests

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 从编译阶段把产物 jar 复制过来
COPY --from=build /app/target/*.jar app.jar

# 容器内监听 8080（由 docker-compose 映射到宿主机）
EXPOSE 8080

# 时区设为上海，避免日志和数据库时间相差 8 小时
ENV TZ=Asia/Shanghai

# 启动命令：java -jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
