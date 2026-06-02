#!/usr/bin/env bash
# 服务器侧（deepshape 国内机器）部署脚本：七牛中转方案的落地端。
#
# 配合 .github/workflows/deploy.yml：GHA 把镜像 tar.gz 上传七牛后，打印一条
# 调用本脚本的命令。本脚本在墙内服务器跑：
#   七牛下载（同区秒级）→ docker load → push TCR（同区）→ 触发 dokploy redeploy。
#
# 用法（通常由 GHA Summary 打印的命令调用，无需手敲）：
#   bash deploy-from-qiniu.sh <签名下载URL> <image:latest> <image:sha-xxx>
#
# 前置：
#   - docker 已登录 TCR（ccr.ccs.tencentyun.com）—— 服务器已 login，无需重复
#   - DOKPLOY_DEPLOY_WEBHOOK 配在同目录 .deploy-env 里（不进 git）：
#       echo 'DOKPLOY_DEPLOY_WEBHOOK=http://<dokploy>/api/deploy/<token>' > scripts/.deploy-env
#     dokploy 仍在本地 PoC 时，这个 webhook 指向本地 control plane；
#     上云后改成公网 URL。未配置则跳过触发，只打印提示。
set -euo pipefail

SIGNED_URL="${1:?用法: deploy-from-qiniu.sh <签名URL> <image:latest> <image:sha-xxx>}"
IMG_LATEST="${2:?缺 image:latest 参数}"
IMG_SHA="${3:?缺 image:sha-xxx 参数}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> [1/4] 从七牛下载镜像 tar.gz"
START=$(date +%s)
curl -fsSL -o "$WORK/image.tar.gz" "$SIGNED_URL"
echo "    下载完成，耗时 $(( $(date +%s) - START ))s，大小 $(du -h "$WORK/image.tar.gz" | cut -f1)"

echo "==> [2/4] docker load"
gunzip -c "$WORK/image.tar.gz" | docker load

echo "==> [3/4] push 到 TCR（同区，快）"
docker push "$IMG_LATEST"
docker push "$IMG_SHA"

echo "==> [4/4] 触发 dokploy redeploy"
# 读 webhook 配置（不进 git）
if [ -f "$SCRIPT_DIR/.deploy-env" ]; then
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.deploy-env"
fi
if [ -n "${DOKPLOY_DEPLOY_WEBHOOK:-}" ]; then
  curl -fsS -X POST "$DOKPLOY_DEPLOY_WEBHOOK" && echo "    已触发 dokploy 部署"
else
  echo "    ⚠ 未配置 DOKPLOY_DEPLOY_WEBHOOK（scripts/.deploy-env），跳过自动触发。"
  echo "    手动触发：在 Dokploy UI → Domu → Deploy，或 curl 其 webhook。"
  echo "    镜像 ${IMG_LATEST} 已就绪，dokploy redeploy 会 pull 到。"
fi

echo "✅ 完成。"
