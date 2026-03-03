set -ex

BACKEND_TAG=ccr.ccs.tencentyun.com/o-pq/domu:$(date +%Y%m%d)
HEAD_BACKEND_TAG=ccr.ccs.tencentyun.com/o-pq/domu:latest
docker build -t $BACKEND_TAG -f Dockerfile .
docker tag $BACKEND_TAG $HEAD_BACKEND_TAG