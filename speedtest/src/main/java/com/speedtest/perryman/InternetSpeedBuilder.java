package com.speedtest.perryman;

import android.app.Activity;
import android.location.Location;

import androidx.annotation.NonNull;

import com.speedtest.perryman.util.DownloadSpeedTest;
import com.speedtest.perryman.util.PingTest;
import com.speedtest.perryman.util.UploadSpeedTest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class InternetSpeedBuilder extends Thread{
    SpeedTestHandler getSpeedTestHostsHandler = null;
    static int position = 0;
    static int lastPosition = 0;
    HashSet<String> tempBlackList;
    private OnInternetSpeedListener listener;
    Activity context;

    public InternetSpeedBuilder(Activity requireActivity) {
        this.context = requireActivity;
        tempBlackList = new HashSet<>();
        getSpeedTestHostsHandler = new SpeedTestHandler();
        getSpeedTestHostsHandler.start();
    }

    public void setOnInternetSpeedListener(@NonNull OnInternetSpeedListener listener) {
        this.listener = listener;
    }

    public void startTest() {
        final DecimalFormat dec = new DecimalFormat("#.##");
        if (getSpeedTestHostsHandler == null) {
            getSpeedTestHostsHandler = new SpeedTestHandler();
            getSpeedTestHostsHandler.start();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Get egcodes.speedtest hosts
                int timeCount = 600; //1min
                while (!getSpeedTestHostsHandler.isFinished()) {
                    timeCount--;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (timeCount <= 0) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onError("No Connection...");
                                }
                            }
                        });
                        getSpeedTestHostsHandler = null;
                        return;
                    }
                }

                //Find closest server
                HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();
                HashMap<Integer, List<String>> mapValue = getSpeedTestHostsHandler.getMapValue();
                double selfLat = getSpeedTestHostsHandler.getSelfLat();
                double selfLon = getSpeedTestHostsHandler.getSelfLon();
                double tmp = 19349458;
                double dist = 0.0;
                int findServerIndex = 0;
                for (int index : mapKey.keySet()) {
                    if (tempBlackList.contains(mapValue.get(index).get(5))) {
                        continue;
                    }
                    Location source = new Location("Source");
                    source.setLatitude(selfLat);
                    source.setLongitude(selfLon);

                    List<String> ls = mapValue.get(index);
                    Location dest = new Location("Dest");
                    dest.setLatitude(Double.parseDouble(ls.get(0)));
                    dest.setLongitude(Double.parseDouble(ls.get(1)));

                    double distance = source.distanceTo(dest);
                    if (tmp > distance) {
                        tmp = distance;
                        dist = distance;
                        findServerIndex = index;
                    }
                }
                String testAddr = mapKey.get(findServerIndex).replace("http://", "https://");
                final List<String> info = mapValue.get(findServerIndex);
                final double distance = dist;

                if (info == null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onError("There was a problem. Try again later.");
                            }
                        }
                    });
                    return;
                }

                final List<Double> pingRateList = new ArrayList<>();
                final List<Double> downloadRateList = new ArrayList<>();
                final List<Double> uploadRateList = new ArrayList<>();
                Boolean pingTestStarted = false;
                Boolean pingTestFinished = false;
                Boolean downloadTestStarted = false;
                Boolean downloadTestFinished = false;
                Boolean uploadTestStarted = false;
                Boolean uploadTestFinished = false;

                //Init Test
                final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 3);
                final DownloadSpeedTest downloadTest = new DownloadSpeedTest(testAddr.replace(testAddr.split("/")[testAddr.split("/").length - 1], ""));
                final UploadSpeedTest uploadTest = new UploadSpeedTest(testAddr);

                //Tests
                while (true) {
                    if (!pingTestStarted) {
                        pingTest.start();
                        pingTestStarted = true;
                    }
                    if (pingTestFinished && !downloadTestStarted) {
                        downloadTest.start();
                        downloadTestStarted = true;
                    }
                    if (downloadTestFinished && !uploadTestStarted) {
                        uploadTest.start();
                        uploadTestStarted = true;
                    }


//                    Ping Test
                    if (pingTestFinished) {
                        //Failure
                        if (pingTest.getAvgRtt() == 0) {
                            System.out.println("Ping error...");
                        } else {
                            //Success
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onProgressPing(dec.format(pingTest.getAvgRtt()) + " ms");
                                    }
                                }
                            });
                        }
                    } else {
                        pingRateList.add(pingTest.getInstantRtt());

                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onProgressPing(dec.format(pingTest.getInstantRtt()) + " ms");
                                }
                            }
                        });

                    }

                    //Download Test
                    if (pingTestFinished) {
                        if (downloadTestFinished) {
                            //Failure
                            if (downloadTest.getFinalDownloadRate() == 0) {
                                System.out.println("Download error...");
                            } else {
                                //Success
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (listener != null) {
                                            listener.onProgressDownloadSpeed(dec.format(downloadTest.getFinalDownloadRate()) + " Mbps", 0);
                                        }
                                    }
                                });
                            }
                        } else {
                            //Calc position
                            double downloadRate = downloadTest.getFinalDownloadRate();
                            downloadRateList.add(downloadRate);
                            position = getPositionByRate(downloadRate);

                            context.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onProgressDownloadSpeed(dec.format(downloadTest.getInstantDownloadRate()) + " Mbps", position);
                                    }
                                }

                            });
                            lastPosition = position;

                        }
                    }


                    //Upload Test
                    if (downloadTestFinished) {
                        if (uploadTestFinished) {
                            //Failure
                            if (uploadTest.getFinalUploadRate() == 0) {
                                System.out.println("Upload error...");
                            } else {
                                //Success
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (listener != null) {
                                            listener.onProgressUploadSpeed(dec.format(uploadTest.getFinalUploadRate()) + " Mbps", 0);
                                        }
                                    }
                                });
                            }
                        } else {
                            //Calc position
                            double uploadRate = uploadTest.getInstantUploadRate();
                            uploadRateList.add(uploadRate);
                            position = getPositionByRate(uploadRate);

                            context.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onProgressUploadSpeed(dec.format(uploadTest.getInstantUploadRate()) + " Mbps", position);
                                    }
                                }

                            });
                            lastPosition = position;

                        }
                    }

//                    Test bitti
                    if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) {
                        break;
                    }

                    if (pingTest.isFinished()) {
                        pingTestFinished = true;
                    }
                    if (downloadTest.isFinished()) {
                        downloadTestFinished = true;
                    }
                    if (uploadTest.isFinished()) {
                        uploadTestFinished = true;
                    }

                    if (pingTestStarted && !pingTestFinished) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (listener!=null){
                            listener.onCompleted();
                        }
                    }
                });

            }
        }).start();
    }

    public int getPositionByRate(double rate) {
        if (rate <= 1) {
            return (int) (rate * 30);

        } else if (rate <= 10) {
            return (int) (rate * 6) + 30;

        } else if (rate <= 30) {
            return (int) ((rate - 10) * 3) + 90;

        } else if (rate <= 50) {
            return (int) ((rate - 30) * 1.5) + 150;

        } else if (rate <= 100) {
            return (int) ((rate - 50) * 1.2) + 180;
        }

        return 0;
    }

    public interface OnInternetSpeedListener {
        void onProgressDownloadSpeed(String downloadSpeed, int position);

        void onProgressUploadSpeed(String uploadSpeed, int position);

        void onProgressPing(String ping);

        void onError(String error);

        void onCompleted();

    }

}
