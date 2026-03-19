# Domu - 家肴

家肴是一个家庭餐饮应用，包含以下功能：创建家庭，菜谱功能，点菜功能，做菜记录。


## 名字来源

Domu 来源拉丁语 Domus，意为“家/屋檐”。跟日语 "どうも - Doumo" 谐音，"Domu~ 饭好啦，趁热吃吧！"

## quick start

```shell
# 本地开发模式
cd backend  
./gradlew bootRun --args='--spring.profiles.active=dev'

# 运行测试：
cd backend
./gradlew test                                          # 运行所有测试
./gradlew test --tests "com.domu.unit.service.AuthServiceTest"  # 运行指定测试类

# 构建 JAR：
./gradlew build

# 前端
cd frontend
npm run dev

```