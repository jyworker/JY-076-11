# iyzipay-java

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.iyzipay/iyzipay-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.iyzipay/iyzipay-java/)

iyzico Java SDK，用于在 Java 项目中调用 iyzico API。

## 项目简介

这个仓库提供了：

- `Options` 配置对象，用于设置 `apiKey`、`secretKey`、`baseUrl` 和代理信息
- 各类支付、退款、取消、卡管理、分期、BKM、APM、订阅等接口模型
- 可直接参考的 sample 测试代码

如果你想快速接入，先看下面的“快速开始”。

## 环境要求

- Java 8+
- Maven 3+（或仓库自带的 Maven Wrapper）

## 安装

### Maven

```xml
<dependency>
  <groupId>com.iyzipay</groupId>
  <artifactId>iyzipay-java</artifactId>
  <version>2.0.141</version>
</dependency>
```

### Gradle

```groovy
implementation "com.iyzipay:iyzipay-java:2.0.141"
```

## 快速开始

先创建 `Options`：

```java
Options options = new Options();
options.setApiKey("your api key");
options.setSecretKey("your secret key");
options.setBaseUrl("https://sandbox-api.iyzipay.com");
```

如果你需要走代理，也可以配置：

```java
options.setProxyHost("127.0.0.1");
options.setProxyPort(8080);
```

发起一笔支付请求：

```java
CreatePaymentRequest request = new CreatePaymentRequest();
request.setLocale(Locale.TR.getValue());
request.setConversationId("123456789");
request.setPrice(new BigDecimal("1"));
request.setPaidPrice(new BigDecimal("1.2"));
request.setCurrency(Currency.TRY.name());
request.setInstallment(1);
request.setBasketId("B67832");
request.setPaymentChannel(PaymentChannel.WEB.name());
request.setPaymentGroup(PaymentGroup.PRODUCT.name());

PaymentCard paymentCard = new PaymentCard();
paymentCard.setCardHolderName("John Doe");
paymentCard.setCardNumber("5528790000000008");
paymentCard.setExpireMonth("12");
paymentCard.setExpireYear("2030");
paymentCard.setCvc("123");
paymentCard.setRegisterCard(0);
request.setPaymentCard(paymentCard);

Payment payment = Payment.create(request, options);
```

更多完整示例可以参考 `src/test/java/com/iyzipay/sample`。

## 常用接入流程

1. 创建 `Options`
2. 按业务构建请求对象
3. 调用对应的静态接口，例如 `Payment.create(...)`
4. 根据返回对象判断 `status`、`errorCode`、`errorMessage`

## 支持能力

- 支付、退款、取消、审批/拒绝
- 3DS 支付与 checkout form
- 卡存储与卡管理
- 分期与 BIN 查询
- BKM Express
- APM 支付
- Sub merchant
- iyziLink 与 iyziUP
- 订阅相关流程
- 报表接口

## 构建

```bash
./mvnw clean install
```

## 测试

示例测试依赖系统参数：

- `baseUrl`
- `apiKey`
- `secretKey`

运行单个 sample：

```bash
mvn test -Dtest=PaymentSample -DbaseUrl=https://sandbox-api.iyzipay.com -DapiKey=yourApiKey -DsecretKey=yourSecretKey
mvn test -Dtest=PaymentSample#should_create_payment -DbaseUrl=https://sandbox-api.iyzipay.com -DapiKey=yourApiKey -DsecretKey=yourSecretKey
```

使用 Maven Wrapper：

```bash
./mvnw test -Dtest=PaymentSample -DbaseUrl=https://sandbox-api.iyzipay.com -DapiKey=yourApiKey -DsecretKey=yourSecretKey
./mvnw test -Dtest=PaymentSample#should_create_payment -DbaseUrl=https://sandbox-api.iyzipay.com -DapiKey=yourApiKey -DsecretKey=yourSecretKey
```

## Mock 测试卡

### 成功支付卡

| Card Number | Bank | Card Type |
| --- | --- | --- |
| 5890040000000016 | Akbank | Master Card (Debit) |
| 5526080000000006 | Akbank | Master Card (Credit) |
| 4766620000000001 | Denizbank | Visa (Debit) |
| 4603450000000000 | Denizbank | Visa (Credit) |
| 4729150000000005 | Denizbank Bonus | Visa (Credit) |
| 4987490000000002 | Finansbank | Visa (Debit) |
| 5311570000000005 | Finansbank | Master Card (Credit) |
| 9792020000000001 | Finansbank | Troy (Debit) |
| 9792030000000000 | Finansbank | Troy (Credit) |
| 5170410000000004 | Garanti Bankası | Master Card (Debit) |
| 5400360000000003 | Garanti Bankası | Master Card (Credit) |
| 374427000000003 | Garanti Bankası | American Express |
| 4475050000000003 | Halkbank | Visa (Debit) |
| 5528790000000008 | Halkbank | Master Card (Credit) |
| 4059030000000009 | HSBC Bank | Visa (Debit) |
| 5504720000000003 | HSBC Bank | Master Card (Credit) |
| 5892830000000000 | Türkiye İş Bankası | Master Card (Debit) |
| 4543590000000006 | Türkiye İş Bankası | Visa (Credit) |
| 4910050000000006 | Vakıfbank | Visa (Debit) |
| 4157920000000002 | Vakıfbank | Visa (Credit) |
| 5168880000000002 | Yapı ve Kredi Bankası | Master Card (Debit) |
| 5451030000000000 | Yapı ve Kredi Bankası | Master Card (Credit) |

### Cross border

| Card Number | Country |
| --- | --- |
| 4054180000000007 | Non-Turkish (Debit) |
| 5400010000000004 | Non-Turkish (Credit) |

### Loyalty

| Card Number | Card Family |
| --- | --- |
| 5549607159333771 | Bonus |
| 5451030000000000 | World |

### 特定错误码

| Card Number | Description |
| --- | --- |
| 5406670000000009 | Success but cannot be cancelled, refund or post auth |
| 4111111111111129 | Not sufficient funds |
| 4129111111111111 | Do not honour |
| 4128111111111112 | Invalid transaction |
| 4127111111111113 | Lost card |
| 4126111111111114 | Stolen card |
| 4125111111111115 | Expired card |
| 4124111111111116 | Invalid cvc2 |
| 4123111111111117 | Not permitted to card holder |
| 4122111111111118 | Not permitted to terminal |
| 4121111111111119 | Fraud suspect |
| 4130111111111118 | General error |
| 4131111111111117 | Success but mdStatus is 0 |
| 4141111111111115 | Success but mdStatus is 4 |
| 4151111111111112 | 3dsecure initialize failed |
| 4151111111111393 | Restricted for online transactions |

## Mock APM 账号

| Account Holder Name | Description |
| --- | --- |
| success | Succeeded payment after succeeded initialize |
| fail-after-init | Failed payment after succeeded initialize |
| error | Failed initialize |

