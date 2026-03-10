set -ex

sh build-images.sh

scp .env domu:~
scp docker-compose.yml domu:~
ssh domu "docker-compose pull && mkdir -p ~/data && chmod -R 777 ~/data && docker-compose down && docker-compose up -d"