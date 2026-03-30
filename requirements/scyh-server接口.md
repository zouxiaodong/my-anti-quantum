# API测试文档


**简介**:API测试文档


**HOST**:localhost:8101


**联系人**:村雨遥


**Version**:v1.1.0


**接口路径**:/v2/api-docs


[TOC]






# demo


## 随机数生成


**接口地址**:`/scyh-server/v101/genRandom`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|length|length|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"msg": ""
}
```


## 对称算法加密运算


**接口地址**:`/scyh-server/v101/symAlgEnc`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "data": "",
  "iv": "",
  "keyData": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|对称算法加密运算接口入参|body|true|SymAlgEncParam|SymAlgEncParam|
|&emsp;&emsp;algorithm|算法: SM4/ECB/NoPadding 或者 SM4/CBC/NoPadding||true|string||
|&emsp;&emsp;data|数据明文报文||true|string||
|&emsp;&emsp;iv|iv值,CBC模式必填, hex格式字符串||false|string||
|&emsp;&emsp;keyData|密钥值, hex格式字符串||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## 对称算法解密运算


**接口地址**:`/scyh-server/v101/symAlgDec`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "data": "",
  "iv": "",
  "keyData": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|对称算法解密运算接口入参|body|true|SymAlgDecParam|SymAlgDecParam|
|&emsp;&emsp;algorithm|算法: SM4/ECB/NoPadding 或者 SM4/CBC/NoPadding||true|string||
|&emsp;&emsp;data|数据密文报文||true|string||
|&emsp;&emsp;iv|iv值,CBC模式必填||false|string||
|&emsp;&emsp;keyData|密钥值, hex格式字符串||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## 摘要运算


**接口地址**:`/scyh-server/v101/hash`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "data": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|摘要运算接口入参|body|true|HashParam|HashParam|
|&emsp;&emsp;algorithm|算法: SM3、SHA1、SHA256||true|string||
|&emsp;&emsp;data|数据报文||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## hmac计算


**接口地址**:`/scyh-server/v101/hmac`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "data": "",
  "key": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|hmac运算接口入参|body|true|HMacParam|HMacParam|
|&emsp;&emsp;data|数据报文||true|string||
|&emsp;&emsp;key|密钥, hex格式字符串||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## ecc密钥对生成


**接口地址**:`/scyh-server/v101/genEccKeyPair`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"msg": ""
}
```


## sm2加密


**接口地址**:`/scyh-server/v101/sm2Enc`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "data": "",
  "privateKey": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|Sm2加密运算接口入参|body|true|SM2EncParam|SM2EncParam|
|&emsp;&emsp;data|数据报文||true|string||
|&emsp;&emsp;privateKey|私钥：hex格式||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## sm2解密


**接口地址**:`/scyh-server/v101/sm2Dec`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "data": "",
  "privateKey": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|Sm2加密运算接口入参|body|true|SM2EncParam|SM2EncParam|
|&emsp;&emsp;data|数据报文||true|string||
|&emsp;&emsp;privateKey|私钥：hex格式||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```


## 生成pqc密钥对


**接口地址**:`/scyh-server/v101/genPqcKeyPair`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|生成pqc密钥对接口入参|body|true|GenPqcKeyPairParam|GenPqcKeyPairParam|
|&emsp;&emsp;algorithm|pqc算法：kyber512、kyber768、kyber1024、kyber512_gm、kyber768_gm、kyber1024_gm、dilithium2、dilithium3、dilithium5、dilithium2_gm、dilithium3_gm、dilithium5_gm||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«object»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||object||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": {},
	"msg": ""
}
```


## pqc公钥封装


**接口地址**:`/scyh-server/v101/pqcKeyWrapper`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "pqcPubkey": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|pqc密钥封装接口入参|body|true|PqcKeyWrapperParam|PqcKeyWrapperParam|
|&emsp;&emsp;algorithm|pqc算法：kyber512、kyber768、kyber1024、kyber512_gm、kyber768_gm、kyber1024_gm||true|string||
|&emsp;&emsp;pqcPubkey|pqc公钥：hex格式||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«PqcKeyWrapperDto»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||PqcKeyWrapperDto|PqcKeyWrapperDto|
|&emsp;&emsp;keyCipher|pqc公钥加密的对称密钥密文|string||
|&emsp;&emsp;keyId|密钥索引|string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": {
		"keyCipher": "",
		"keyId": ""
	},
	"msg": ""
}
```


## pqc私钥解封


**接口地址**:`/scyh-server/v101/pqcKeyUnWrapper`


**请求方式**:`POST`


**请求数据类型**:`application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "algorithm": "",
  "cipherText": "",
  "pqcPrikey": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|param|pqc私钥解封接口入参|body|true|PqcKeyUnWrapperParam|PqcKeyUnWrapperParam|
|&emsp;&emsp;algorithm|pqc算法：kyber512、kyber768、kyber1024、kyber512_gm、kyber768_gm、kyber1024_gm||true|string||
|&emsp;&emsp;cipherText|数据密文：hex格式||true|string||
|&emsp;&emsp;pqcPrikey|pqc私钥：hex格式||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result«string»|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|data||string||
|msg||string||


**响应示例**:
```javascript
{
	"code": 0,
	"data": "",
	"msg": ""
}
```