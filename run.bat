call mvn clean
call mvn package
START /WAIT docker-compose build
START /WAIT docker-compose up