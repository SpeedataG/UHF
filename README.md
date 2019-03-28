# UHF
Speedata Android SDK

## 低电使用说明
* 芯联模块
  * 电池电量低于20% 禁止使用
* 非芯联模块，非SD60 SC60型号
  * 电池电量低于20% 禁止使用
  * 开始盘点前电压低于3.75v 禁止使用
* 非芯联模块，SD60／SC60型号
  * 电池电量低于20% 禁止使用
  * 开始盘点前电压低于3.75v 禁止使用
  * 盘点过程中电压低于3.4v禁止使用


## 文件说明

	“UHF_AS”文件夹中存放了超高频的android stuido版开发源码，包含了lib文件。
	
	“UHF_Eclipse”文件夹内分别存放了超高频dome的源码以及其需要导入的lib文件“SpeedataUhfLibs”和“LibDevice”
	安装包文件“uhfDome.apk”是超高频的demo文件。
	
	程序的Api链接：http://www.showdoc.cc/11073?page_id=90826
	
## 功能介绍

	本程序适用于思必拓手持终端设备中，装有超高频读卡模块的设备。
## 使用说明

	在任一支持高频读卡的思必拓手持终端设备中，安装”uhfDome.apk”文件后，会生成一个名为“uhf”的程序。点击进入后，在界面右上方有选择字样按钮，点击进行选卡
	然后再屏幕中心光标处填写需要写入的数据，点击“写”按钮进行写卡。
## 开发说明

	使用Android Studio的开发人员可直接在开发工具中打开相应的源码，参考源码进行开发。不使用源码则需要在build.gradle文件中添加：compile 'com.speedata:deivice:1.0'
    和 compile 'com.speedata:libuhf:+'
	使用Eclipse的开发人员，需要把”eclipse源码“文件夹中的三个源码都导入开发工具，其中“SpeedataUhfLibs”和“LibDevice”作为library，被Uhfdome程序源码调用。
	“LibDevice”作为library，被"SpeedataUhfLibs"调用。
## API文档

	详细的接口说明在showdoc，地址：http://www.showdoc.cc/11073?page_id=90826
