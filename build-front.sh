pushd ./frontend

npm run build

popd

cp -rf ./frontend/dist/ ./backend/src/main/resources/static/