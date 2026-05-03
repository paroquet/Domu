pushd ./frontend

npm install
npm run build

popd

cp -rf ./frontend/dist/ ./backend/src/main/resources/static/