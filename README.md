# SlideRangeBar
一个可拖动的范围选择器

root build.gradle：

	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}
module：  

	dependencies {
	        implementation 'com.github.smartzheng:SlideRangeBar:1.0.0'
	}
	
属性: 

    rangeCount:节点个数  
    filledColor:选择位置填充颜色  
    emptyColor:空心颜色  
    selectedTextColor:选中文字颜色  
    normalTextColor:未选中文字颜色  
    barHeightPercent:选择条高度占比  
    slotRadiusPercent:正常节点高度占比  
    sliderRadiusPercent:拖动块高度占比  
    
    
![image](https://github.com/smartzheng/SlideRangeBar/blob/master/sample/pics/slidebar.jpeg)
