/*
 * Copyright 2012 Indigo Rose Software Design Corporation
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airbop.library.simple;

//import static com.airbop.library.simple.CommonUtilities.GOOGLE_PROJECT_NUMBER;
import static com.airbop.library.simple.CommonUtilities.displayMessage;
import static com.airbop.library.simple.CommonUtilities.onGCMMessage;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
//import android.util.Log;

//import com.airbop.gcm.demo.R;
//import com.airbop.client.DemoActivity;
import com.airbop.library.simple.CommonUtilities.AirBopManifestSettings;
import com.airbop.library.simple.CommonUtilities.AirBopStrings;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;


/**
 * IntentService responsible for handling GCM messages.\
 * Docs: http://developer.android.com/guide/google/gcm/client-javadoc/com/google/android/gcm/GCMBaseIntentService.html
 */
public class AirBopGCMIntentService extends GCMBaseIntentService {

    //@SuppressWarnings("hiding")
    private static final String TAG = "AirBopGCMIntentService";
    /*
    protected AirBopGCMIntentService(String... senderIds) {
	    super("773896660824");
	}*/
    
    @Override
    protected String[] getSenderIds (Context context) {
    	AirBopManifestSettings airBop_settings = CommonUtilities.loadDataFromManifest(context);
		String[] ids = {airBop_settings.mGoogleProjectNumber};
    	return ids;
    }
    
    /**
	 * Called after the app has been registered on the GCM service
	 * We now have the regID that we can use to register with the
	 * AirBop servers.
	 */
    @Override
    protected void onRegistered(Context context, String registrationId) {
    	
        //Log.i(TAG, "Device registered: regId = " + registrationId);
        displayMessage(context, AirBopStrings.airbop_gcm_registered);
        //Get our data for the server
        AirBopServerUtilities server_data = AirBopServerUtilities.fillDefaults(registrationId);
        //server_data.loadCurrentLocation(context);
        server_data.loadDataFromPrefs(context);
        // Get rid of the location from the prefs so we requery next time
        AirBopServerUtilities.clearLocationPrefs(context);
        AirBopServerUtilities.register(getApplicationContext()
    			, server_data);
    }

    /**
	 * Called after the device has been unregisterd from the GCM server.
	 * We we are registered on the AirBop servers we should unregister
	 * from there as well.
	 */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
    	
        //Log.i(TAG, "Device unregistered");
        displayMessage(context, AirBopStrings.airbop_gcm_unregistered);
        //If we are still registered with AirBop it is time to unregister
        if (GCMRegistrar.isRegisteredOnServer(context)) {
        	AirBopServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
        	//Log.i(TAG, "Ignoring unregister callback");
        	
        }
    }
    /**
	 * We have received a push notification from GCM, analyze
	 * the intents bundle for the payload.
	 */
	@Override
	protected void onMessage(Context context, Intent intent) {
		
	    //Log.i(TAG, "Received message");
	    displayMessage(context, "Message Received" );
	    String message = null;
	    String title = null;
	    String url = null;
	    String image_url = null;
	    String large_icon = null;
	    
	    if (intent != null) {      	
	    	//Check the bundle for the pay load body and title
	        Bundle bundle = intent.getExtras();
	        AirBopManifestSettings airBop_settings = CommonUtilities.loadDataFromManifest(context);
			if (airBop_settings.mDefaultNotificationHandling) {
		 	   	if (bundle != null) {
		 	   		displayMessage(context, "Message bundle: " +  bundle);
		 	   		//Log.i(TAG, "Message bundle: " +  bundle);
		 	   		message = bundle.getString("message");   			 	   		
		 	   		title = bundle.getString("title");	
		 	   		url = bundle.getString("url");	 	   		
		 	   		image_url = bundle.getString("image_url");
		 	   		large_icon = bundle.getString("large_icon");
		 	   		
		 	   		// If there was no body just use a standard message
		 		   	if (message == null) {
		 		   		message = AirBopStrings.airbop_message;
		 			}
		 		   	
		 		   	if (image_url != null) {
		 		   		generateImageNotification(context, title, message, url, image_url, large_icon); 	
		 		   	} else {
		 		   		generateNotification(context, title, message, url, large_icon);
		 		   	}
		 	   	} 
			} else {
				onGCMMessage(context, bundle);
			}
	    }
	   	
	}

	@Override
    protected void onDeletedMessages(Context context, int total) {
		/*
		 * Called when the GCM servers tells that app that 
		 * pending messages have been deleted because the
		 * device was idle.
		 */
        //Log.i(TAG, "Received deleted messages notification");
        String message = String.format(AirBopStrings.airbop_gcm_deleted, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, "", message);
    }

	/**
	 * Called on registration or unregistration error. 
	 * Whatever this error is, it is not recoverable
	 */
    @Override
    public void onError(Context context, String errorId) {
    	
        //Log.i(TAG, "Received error: " + errorId);
        displayMessage(context, String.format(AirBopStrings.airbop_gcm_error, errorId));
    }

    /**
     * Called on a registration error that could be retried.
	 * By default, it does nothing and returns true, but could be
	 * overridden to change that behavior and/or display the error. 
	 */
    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
    	// log message
        //Log.i(TAG, "Received recoverable error: " + errorId);
        displayMessage(context, String.format(AirBopStrings.airbop_gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }
    
    /**
     * Decode a base64 string into a Bitmap
     */
    private static Bitmap decodeImage(String image_data) {
    	// Decode the encoded string into largeIcon
        Bitmap largeIcon = null;
        if ((image_data != null) && (!image_data.equals(""))) {
        	byte[] decodedImage = Base64.decode(image_data, Base64.DEFAULT);
        	if (decodedImage != null) {
        		largeIcon = BitmapFactory.decodeByteArray(decodedImage
        				, 0
        				, decodedImage.length);
        	}
        }
        return largeIcon;
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context
    		, String title
    		, String message) {
        
    	AirBopManifestSettings airBop_settings = CommonUtilities.loadDataFromManifest(context);
        //int icon = R.drawable.ic_stat_gcm;
    	int icon = 0;
        Resources res = context.getResources();
        if (res != null) {
        	//icon = res.getIdentifier(airBop_settings.mDefaultNotificationIcon, null, null);
        	icon = airBop_settings.mNotificationIcon;
        }
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        //if ((title == null) || (title.equals(""))) {
        if (title == null) {
        	title = airBop_settings.mDefaultNotificationTitle;
        }
        Class intent_class = null;
        if (context != null) {
        	ClassLoader class_loader = context.getClassLoader();
        	if (class_loader != null) {
        		try {
        			if (airBop_settings.mDefaultNotificationClass != null) {
        				intent_class = Class.forName(airBop_settings.mDefaultNotificationClass);
        			}
        		} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		//Log.i(TAG, "intent_class: " + intent_class);
        	}
        }
        Intent notificationIntent = null;
        if (intent_class != null){
        	notificationIntent = new Intent(context, intent_class);
        } else {
        	notificationIntent = new Intent(Intent.ACTION_VIEW);
        }
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
                
        Notification notification = new NotificationCompat.Builder(context)
	        .setContentTitle(title)
	        .setContentText(message)
	        .setContentIntent(intent)
	        .setSmallIcon(icon)
	        .setWhen(when)
	        .setStyle(new NotificationCompat.BigTextStyle()
	        	.bigText(message))
	    .build();
        
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
    
    private static void generateNotification(Context context
    		, String title
    		, String message
    		, String url
    		, String large_icon) {
    	
        //int icon = R.drawable.ic_stat_gcm;
    	AirBopManifestSettings airBop_settings = CommonUtilities.loadDataFromManifest(context);
         
    	int icon = airBop_settings.mNotificationIcon;
        
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
                
        //if ((title == null) || (title.equals(""))) {
        if (title == null) {
           title = airBop_settings.mDefaultNotificationTitle;
         }
        
        Intent notificationIntent = null;
        if ((url == null) || (url.equals(""))) {
        	//just bring up the app
        	if (context != null) {
            	ClassLoader class_loader = context.getClassLoader();
            	if (class_loader != null) {
            		try {
            			if (airBop_settings.mDefaultNotificationClass != null) {
            				notificationIntent = new Intent(context
    							, Class.forName(airBop_settings.mDefaultNotificationClass));
            			}
            		} catch (ClassNotFoundException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    					//notificationIntent = new Intent(Intent.ACTION_VIEW);
    				}
            	}
            }
        	
        } else {
        	//Launch the URL
        	notificationIntent = new Intent(Intent.ACTION_VIEW);
            notificationIntent.setData(Uri.parse(url));
            notificationIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        }
        PendingIntent intent = null;
        // set intent so it does not start a new activity
        if (notificationIntent != null) {
        	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        }
        
        
        Builder notificationBuilder = new NotificationCompat.Builder(context)
		        .setContentTitle(title)
		        .setContentText(message)
		        .setLargeIcon(decodeImage(large_icon))
		        .setWhen(when)
		        .setStyle(new NotificationCompat.BigTextStyle()
				    	.bigText(message));
        if (intent != null) {
        	notificationBuilder.setContentIntent(intent);
        }
        if (icon != 0) {
        	notificationBuilder.setSmallIcon(icon);
        }
        Notification notification = notificationBuilder.build();
        
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
       
    }
    
    private static void generateImageNotification(Context context
    		, String title
    		, String message
    		, String url
    		, String image_url
    		, String large_icon) {
    	
    	// The bitmap to download
    	Bitmap message_bitmap = null; 
    	// Should we download the image?
    	if ((image_url != null) && (!image_url.equals(""))) {
    		message_bitmap = AirBopImageDownloader.downloadBitmap(image_url, context);
    	}
    	// If we didn't get the image, we're out of here
    	if (message_bitmap == null) {
    		generateNotification(context
    	    		, title
    	    		, message
    	    		, url
    	    		, large_icon);
    		return;
    	}
    	AirBopManifestSettings airBop_settings = CommonUtilities.loadDataFromManifest(context);
           	
        int icon = airBop_settings.mNotificationIcon;
        
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        //if ((title == null) || (title.equals(""))) {
        if (title == null) {
            title = airBop_settings.mDefaultNotificationTitle;
        }
        
        Intent notificationIntent = null;
        if ((url == null) || (url.equals(""))) {
        	//just bring up the app
        	if (context != null) {
            	ClassLoader class_loader = context.getClassLoader();
            	if (class_loader != null) {
            		try {
            			if (airBop_settings.mDefaultNotificationClass != null) {
            				notificationIntent = new Intent(context
    							, Class.forName(airBop_settings.mDefaultNotificationClass));
            			}
            		} catch (ClassNotFoundException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
            	}
            }
        } else {
        	//Launch the URL
        	notificationIntent = new Intent(Intent.ACTION_VIEW);
            notificationIntent.setData(Uri.parse(url));
            notificationIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        }
        PendingIntent intent = null;
        // set intent so it does not start a new activity
        if (notificationIntent != null) {
        	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        }
        
        Builder notificationBuilder = new NotificationCompat.Builder(context)
		        .setContentTitle(title)
		        .setContentText(message)
		        .setLargeIcon(decodeImage(large_icon))
		        .setWhen(when)
		        .setStyle(new NotificationCompat.BigPictureStyle()
			        	.bigPicture(message_bitmap));
		if (intent != null) {
			notificationBuilder.setContentIntent(intent);
		}
		if (icon != 0) {
			notificationBuilder.setSmallIcon(icon);
		}
		Notification notification = notificationBuilder.build();
	                
	       
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
   
}
