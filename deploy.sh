set -ex

sh build-images.sh

REMOTE=domu
scp .env $REMOTE:~
scp docker-compose.yml $REMOTE:~
ssh $REMOTE "docker-compose pull && mkdir -p ~/data && chmod -R 777 ~/data && docker-compose down && docker-compose up -d"