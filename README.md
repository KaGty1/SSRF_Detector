# SSRF_Detector  ![Static Badge](https://img.shields.io/badge/SSRF%20Detector%20version1.0-blue?style=flat&logo=Burpsuite&logoColor=orange&logoSize=auto&label=Author%3A%20KaGty1&labelColor=white&color=blue)

SSRF_Detector是一款基于Burpsuite MontoyaAPI的黑盒SSRF漏洞自动化检测工具，用于检测无回显&amp;全回显SSRF漏洞，提供了多种功能供用户自定义，包括但不限于关键字配置，Payload配置以及检测字符串配置

### 插件功能配置

![image-20250403134800807](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403134800807.png)

#### 扫描流量配置

**(1) 被动扫描：**

不建议开启此功能，`Burpsuite`自带扫描器会触发企业SRC内部告警导致IP被封禁

**(2) HTTP history流量扫描：**

扫描`Proxy`中`HTTP history`模块中的流量，利用`RequestResponseFilter`过滤器筛选出存在关键参数的流量进行`SSRF`漏洞检测

**(3) Repeater流量扫描：**

扫描`Repeater`模块中发送过的数据包流量，利用`RequestResponseFilter`过滤器筛选出存在关键参数的流量进行`SSRF`漏洞检测



#### 缓存文件配置

用户可以自定义缓存文件位置，扫描过的流量信息会自动存储在缓存文件中，避免对某一流量包进行重复无效扫描



### 扫描参数配置

![image-20250403134859169](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403134859169.png)

#### 关键字配置

用户可以自定义添加/删除流量检测关键字，以关键字`url`为例，若某一流量包如下所示：

```http
GET /ssrf.php?url=/imgRes/user.png
Host: xxx.xx.xx.xxx
Cookie: PHPSESSID=xxxxxxxxxxxx
Referer: xxx.xx.xx.xxx
```

则会匹配到`url=/imgRes/user.png`中的`url`参数，并对此流量请求包进行`SSRF`漏洞检测



#### Payload 配置

插件默认提供两种无回显`SSRF`漏洞自动化检测方式

**(1) collaborator：**

利用`Burpsuite`内置的`Collaborator`模块进行无回显`SSRF`漏洞检测，但是很多厂商`WAF`会将`Collaborator`模块的`Payload`写进黑名单，导致漏检，于是提供了第二种检测方式 -> `DNSLog`

**(2) dnslog：**

利用广为人知`dnslog`平台(`http://dnslog.cn`)进行无回显`SSRF`漏洞检测，自动化获取`dnslog`的`Payload`和响应信息



#### 检测字符串配置

用于证明全回显`SSRF`漏洞存在

用户在挖掘企业SRC进行黑盒测试时，企业通常会提供全回显`SSRF`测试靶机，靶机的回显通常为一个标志性字符串，如`flag{tencent_ssrf_vuln}`

二者起到对应关系

```
http://ssrf.tencent.com/flag.html ---> flag{tencent_ssrf_vuln}
```

于是可以在`Payload`配置模块和检测字符串配置模块添加这种对应关系

![image-20250403140436720](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403140436720.png)



### 扫描流量概览

这里会显示所有包含检测关键字的流量请求包

![image-20250403140712482](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403140712482.png)



### 疑似存在漏洞

若配置的`Payloads`列表中有任意大于等于1个`Payload`探测存在`SSRF`漏洞成功，该流量数据包的状态(`Status`)就会自动更新为 "疑似漏洞"，并在 "疑似存在漏洞" 列表中显示

每一个检测存在`SSRF`漏洞的`Payload`都会单独显示，如下所示

**(1) Collaborator payload检测存在`SSRF`漏洞：**

![image-20250403141634651](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403141634651.png)



**(2) DNSLog payload检测存在`SSRF`漏洞：**

![image-20250403141726178](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403141726178.png)



**(3) 企业SRC 全回显`SSRF`靶机回显`flag`字符串：**

![image-20250403142045779](https://img-1325537595.cos.ap-beijing.myqcloud.com/undefinedimage-20250403142045779.png)
