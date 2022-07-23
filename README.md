# Internert_speed_meter

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
Step 2. Add the dependency

dependencies {
	        implementation 'com.github.saifullah-nurani:Internert_speed_meter:1.2'
	}
  
Step 3. Intialize 

InternetSpeedBuilder speedBuilder = new InternetSpeedBuilder(this);
        speedBuilder.setOnInternetSpeedListener(new InternetSpeedBuilder.OnInternetSpeedListener() {
            @Override
            public void onProgressDownloadSpeed(String downloadSpeed, int position) {
                
            }

            @Override
            public void onProgressUploadSpeed(String uploadSpeed, int position) {

            }

            @Override
            public void onProgressPing(String ping) {

            }

            @Override
            public void onError(String error) {

            }

            @Override
            public void onCompleted() {

            }
        });
        speedBuilder.startTest();
        
        
        
      
