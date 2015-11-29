package com.ryan.eugene;

import android.app.Activity;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.text.InputType;
import android.content.Context;
import android.animation.LayoutTransition;
import java.util.Random;
import java.util.HashMap;
import java.lang.Character;
import java.lang.Math;


public class Eugene extends Activity
{
	// Flow controlling constants.
	private final boolean debugging=false; // Controls debugging statements. Especially for logging.
	private final boolean easterEggs=true; // Controls if easter eggs are displayed.
	private final boolean preserveTheFourthWall=false; // Controls if special easter eggs that break any illusion of reality are disabled.
	private final boolean forceEasterEggs=true; // If true, will force probability of easter egg trigerring to 100%. Has no effect on special input easter eggs.
	
	// App-controlling constants.
	private final int priorityOffset=2; // Added to NORM_PRIORITY for the UI thread, and subtracted from it for other threads.
	private final float pauseMultiplier=1.0f; // Muliplied by parameters to pause() to get real pause length. Acts as a global speed control.

	// Currently active views
	private TextView tv; // For primary textual display.
	private EditText ev; // For primary input.
	private RelativeLayout rl; // To layout the TextViews and EditTexts.
	private ScrollView sv; // To enable scrolling on the whole layout.

	// A stored reference to this object, just to ensure availability.
	protected Eugene t=this;
	
	// Stored static references to comparison objects,for optimization.
	private HashMap feelComp=null; // Lookup table for feelings. Initialized by initFeelComp(): Additions should be made there.
	private HashMap weatherComp=null; // Lookup table for weather ratings. Initialized by initWeatherComp(): Additions should be made there.
	private HashMap topicComp=null; // Lookup table for topics. Initialized by initTopicComp(): Additons shluld be made there.

	// Global string constants
	private final String errStr="I\'m sorry, I am experiencing an issue right now... Let\'s try this again, shall we?\n"; // In case a recoverable error occurs.
	private final String myName="Eugene"; // The name of the person simulated by the app.
	private final String LogTag=myName; // The logging tag.

	// Style constants
	private final int shadowSize=5; // The offset and blur radius of shadows.
	private final float shadowDarken=1.25f; // The factor by which shadows are darkened.
	private final float shadowDesaturate=1.5f; // The factor by which shadows are desaturated.
	private final float textSize=16; // The size of displayed text.

	// State storage
	private int stage; // The current stage of processing.
	private int substage; // For substages of processing.
	private boolean input; // To determine whether or not an output should include an output.
	private String name; // User name. Will be passed through capitalize().
	private String feeling; // User feeling, as a string.
	private int parsedFeeling; // User feeling, as a code.
	private int myFeeling; // App feeling, as a code. App feeling string obtainable through unparseFeeling().
	private String rateWeather; // User weather rating, as a string.
	private int parsedRateWeather; // User weather rating, as a code.
	private int myRateWeather; // App weather rating, as a code. App rating string obtainable through unparseWeather().
	private boolean DoNotContinue; // Whether the system should continue.
	private String inputMessage; // To hold the last input message.
	private int misc; // For miscellaneous state storage.
	private String topic; // The topic of conversation.
	private int parsedTopic; // The parsed code for topic discussion.

	// TextView style storage
	private Typeface tv_font; // The font of the TextView.
	private int tv_color; // The text color of the TextView.

	// EditText style storage
	private Typeface ev_font; // The font of the EditText.
	private int ev_color; // The text color of the EditText.
	private int ev_hint_color; // The input hint color of the EditText.

	@Override
	public void onCreate(Bundle savedInstanceState) // Initialize data and start running.
	{
		super.onCreate(savedInstanceState); // Create the activity

		log("Initializing data...");
		
		// Create and display the views active at the beginning of operation
		tv=new TextView (this);
		tv.setText("");
		tv.setId(1);
		rl=new RelativeLayout (this);
		rl.addView(tv, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
		sv=new ScrollView (this);
		sv.addView(rl);
		setContentView(sv);
		
		Random gen=new Random(); // For random generation throughout creation.

		// Initialize our TextView, and store all the necessary data to recreate it.
		final int colors []={Color.GREEN, Color.BLUE, Color.RED, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.DKGRAY, Color.GRAY, Color.BLACK/*, Color.rgb(0, 100, 0), Color.rgb(92, 64, 51), Color.rgb(142, 107, 35), Color.rgb(100, 0, 0), Color.rgb(0, 0, 100)*/};
		tv_color=colors[gen.nextInt(colors.length)];
		tv.setTextColor(tv_color);
		final double brightness=Math.sqrt(.299f*Math.pow(Color.red(tv_color), 2)+.587*Math.pow(Color.green(tv_color), 2)+.114*Math.pow(Color.blue(tv_color), 2)); // Calculate the perceived brightness.
		if (brightness<128)
			sv.setBackgroundColor(Color.WHITE);
		else
			sv.setBackgroundColor(Color.BLACK);
		tv.setShadowLayer(shadowSize, shadowSize, shadowSize, darkenColor(desaturateColor(tv_color, shadowDesaturate), shadowDarken));
		final String fonts[]={
			"RyanTrace",
			"RyanHand",
			"AnnaHand",
			"RyanCursive"
			};
		int font;
		font=gen.nextInt(fonts.length);
		tv_font=Typeface.createFromAsset(getAssets(), "fonts/"+fonts[font]+".ttf");
		tv.setTypeface(tv_font);
		tv.setTextSize(textSize);
		
		// Generate the style for EditText views
		int ccolors[]; // Colors compatible with the TextView color, for the EditView.
		switch (tv_color) // Ensure that we use an appropiate color, given the TextView color.
		{
		case Color.GREEN:
			ccolors=new int[] {Color.RED, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.GRAY};
			break;
		case Color.BLUE:
			ccolors=new int[] {Color.DKGRAY, Color.GRAY, Color.BLACK};
			break;
		case Color.RED:
			ccolors=new int[] {Color.GREEN, Color.CYAN, Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.GRAY};
			break;
		case Color.CYAN:
			ccolors=new int[] {Color.GREEN, Color.RED, Color.MAGENTA, Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.GRAY};
			break;
		case Color.MAGENTA:
			ccolors=new int[] {Color.GREEN, Color.CYAN, Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.GRAY};
			break;
		case Color.WHITE:
		case Color.LTGRAY: // Duplicate cases.
			ccolors=new int[] {Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GRAY};
			break;
		case Color.YELLOW:
			ccolors=new int[] {Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.LTGRAY, Color.GRAY};
			break;
		case Color.DKGRAY:
		case Color.BLACK: // Duplicate cases.
			ccolors=new int[] {Color.BLUE, Color.GRAY};
			break;
		case Color.GRAY:
			ccolors=new int[] {Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.YELLOW, Color.YELLOW};
			break;
		default:
			ccolors=colors;
			break;
		}
		ev_color=ccolors[gen.nextInt(ccolors.length)];
		int f=gen.nextInt(fonts.length);
		while (f==font)
			f=gen.nextInt(fonts.length);
		ev_font=Typeface.createFromAsset(getAssets(), "fonts/"+fonts[gen.nextInt(fonts.length)]+".ttf");
		ev_hint_color=Color.argb(100, Color.red(ev_color), Color.green(ev_color), Color.blue(ev_color));

		// Set starting state.
		stage=0;
		substage=0;
		DoNotContinue=false;
		myFeeling=gen.nextInt(6);
		misc=0;
		initFeelComp();
		initWeatherComp();
		initTopicComp();
		
		if (Build.VERSION.SDK_INT>=11) // If supported...
			rl.setLayoutTransition(new LayoutTransition()); // Enable default layout animations
		
		log("Data initialized. Entering main loop...");
		
		// Start Eugene in a new thread
		Thread proc=new Thread(new Runnable ()
		{
			@Override
			public void run ()
			{
				pause(1000/pauseMultiplier);
				runStage();
			}
		});
		proc.start();
		proc.setPriority(Thread.NORM_PRIORITY-priorityOffset);
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY+priorityOffset);
	}
	
	public void runStage () // More properly, should be runOutput(). Handles the initial output for each stage.
	{
		Random gen=new Random(); // For random generation.
		Adapter tv=new Adapter(); // To abstract away our typing. This should be a function, maybe 'type()', but this can be done at a later date.
		log("Entering output judgement block...");
		input=false; // Most stages don't get input on their first run.
		log("Specsubstage: "+substage);
		switch (stage)
		{
		case 0:
			log("Stage 0 output...");
			for(; substage<2; ++substage)
			{
				switch (substage)
				{
				case 0:
					String greeting;
					switch (gen.nextInt(2))
					{
					case 0:
						greeting="Hi.";
						break;
					case 1:
					default: // Same case
						greeting="Hello.";
						break;
					}
					String id;
					switch (gen.nextInt(2))
					{
					case 0:
						id="I\'m ";
						break;
					case 1:
					default: // Same case
						id="I am ";
						break;
					}
					tv.append(greeting+"\n"+id+myName+".\n");
					break;
				case 1:
					String question;
					switch (gen.nextInt(2))
					{
					case 0:
						question="What is";
						break;
					case 1:
					default: // Same case
						question="What\'s";
						break;
					}
					input=true;
					tv.append(question+" your name?\n");
					break;
				default:
					logError("Invalid stage zero substage: "+substage);
					substage=0;
				}
			}
			log("Stage 0 output completed.");
			break;
		case 1:
			log("Stage 1 output...");
			log("substage: "+substage);
			for (; substage<2; ++substage)
			{
				log("stage two substage loop.");
				switch (substage)
				{
				case 0:
					String feel;
					switch (gen.nextInt(2))
					{
					case 0:
						feel="Nice";
						break;
					case 1:
						if (myFeeling>=4)
							feel="Great";
						else
							feel="Good";
						break;
					default:
						feel="Strange";
					}
					tv.append(feel+" to meet you, "+name+".\n");
					if (stripString(name, true, true, true, true).equals(stripString(myName, true, true, true, true))) // Special condition for same names. Strip strings to avoid confusion for slight differences.
						tv.append("Wow, we have the same name!\nWhat are the odds, huh?\nWell, anyway...\n\n");
					break;
				case 1:
					input=true;
					tv.append("So, "+name+", how are you doing today?\n");
					break;
				default:
					logError("Invalid stage one substage: "+substage);
					substage=0;
				}
			}
			if (substage>9000) // Special case time: Its over 9000!
			{
				switch (substage)
				{
				case 9001:
					tv.append("No, you must have a name...\n");
					stage=0; // Go back a stage... so parseInput() still works...
					input=true;
					tv.append("What is it?\n");
					break;
				case 9002:
					tv.append("No, really.\nYou do have a name...\nTell me...\n");
					stage=0; // Go back a stage... so parseInput() still works...
					input=true;
					tv.append("What is it??\n");
					break;
				case 9003:
					if (myFeeling!=0)
						--myFeeling; // Have a worse day.
					tv.append("Very funny.\n \nThis is your last chance...\n");
					stage=0; // Go back a stage... so parseInput() still works...
					input=true;
					tv.append("What\'s your name...\n");
					break;
				case 9004:
					if (myFeeling!=0)
						--myFeeling; // Again, have a worse day.
					name="Blondie"; // You know, from the good, the bad, and the ugly trilogy... Blondie... AKA the man with no name....
					tv.append("Fine.\nYou\'re "+name+" now.\n");
					substage=0;
					misc=0; // Reset misc storage
					runStage(); // Go through the nice to meet you... dialogue
					break;
				default:
					logError("Invalid stage one special substage: "+substage);
					substage=9001;
				}
			}
			log("Stage 1 output complete.");
			break;
		case 2:
			log("Stage 2 output...");
			log("\tFeeling code: "+parsedFeeling);
			log("\tMy feeling code: "+myFeeling);
			for (; substage<2; ++substage)
			{
				switch (substage)
				{
				case 0:
					switch (parsedFeeling) // Switch on user feeling code.
					{
					case -1: // Unknown feeling.
						logError("Unknown feeling entered: "+feeling+". Requesting new input...");
						tv.append("I\'m sorry, I don\'t understand what you mean by \""+feeling+"\".\n\nI\'m sorry, I\'m not very smart.\nCan you make that simpler for me?\n\n");
						stage=1;
						substage=1;
						runStage(); // Have us do this again.
						return;
					case 0:
						tv.append("Oh. That\'s too bad.\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("Well, me too.\nMaybe we can make it better together?\n");
							break;
						case 1:
							tv.append("Well, I\'m not much better.\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing much better.\n");
							break;
						case 3:
							tv.append("I\'m actually doing "+unparseFeeling(3)+" today...Don\'t you dare ruin it for me!\n");
							break;
						case 4:
							tv.append("Oh.\nI\'m actually doing pretty "+unparseFeeling(3)+" today. "+capitalize(unparseFeeling(4))+", even.\nMaybe talking will help you feel better?\n");
							break;
						case 5:
							tv.append("Well.\nI\'m actually feeling "+unparseFeeling(5)+" today.\nLet\'s talk a bit, maybe I\'ll rub off on you!\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					case 1:
						tv.append("Oh. That\'s "+unparseFeeling(1)+".\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("I\'m not doing much worse.\nMaybe we can make it better together?\n");
							break;
						case 1:
							tv.append("Well, I\'m not any better...\nLet\'s put that behind us, alright?\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing much better.\n");
							break;
						case 3:
							tv.append("I\'m actually doing "+unparseFeeling(3)+" today...Please don\'t ruin it for me!\n");
							break;
						case 4:
							tv.append("Oh.\nI\'m actually doing pretty "+unparseFeeling(3)+" today. "+capitalize(unparseFeeling(4))+", even.\nMaybe talking will help you feel better?\n");
							break;
						case 5:
							tv.append("Well.\nI\'m actually feeling "+unparseFeeling(5)+" today.\nLet\'s talk a bit, maybe I\'ll rub off on you!\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					case 2:
						tv.append("Oh. Really? Prove it.\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("Sorry, I\'m a bit unhappy today...\n");
							break;
						case 1:
							tv.append("Well, I\'m not much worse.\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing much different.\n");
							break;
						case 3:
							tv.append("I\'m actually doing "+unparseFeeling(3)+" today...Don\'t you dare ruin it for me!\n");
							break;
						case 4:
							tv.append("Oh.\nI\'m actually doing pretty "+unparseFeeling(3)+" today. "+capitalize(unparseFeeling(4))+", even.\nMaybe talking will help you feel better?\n");
							break;
						case 5:
							tv.append("Well.\nI\'m actually feeling "+unparseFeeling(5)+" today.\nLet\'s talk a bit, maybe I\'ll rub off on you!\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					case 3:
						tv.append("Oh. That\'s good.\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("Well, I\'m doing "+unparseFeeling(0)+" today. Can we talk?\n");
							break;
						case 1:
							tv.append("Well, I\'m feeling "+unparseFeeling(1)+". Hey, maybe you\'ll rub off on me!\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing any worse.\n");
							break;
						case 3:
							tv.append("I\'m feeling "+unparseFeeling(3)+" too...\nHey! Stop copying me!\n");
							break;
						case 4:
							tv.append("I\'m actually feeling pretty "+unparseFeeling(3)+" too today. "+capitalize(unparseFeeling(4))+", even.\nLet's talk, maybe your day can go "+unparseFeeling(4)+" too!\n");
							break;
						case 5:
							tv.append("I\'m actually feeling "+unparseFeeling(5)+" today.\nLet\'s talk a bit, maybe I\'ll rub off on you!\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					case 4:
						tv.append("That\'s great!\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("Well, I\'m doing "+unparseFeeling(0)+" today. Got any advice?\n");
							break;
						case 1:
							tv.append("Well, I\'m not feeling too "+unparseFeeling(1)+"...\nMaybe I can put it behind me.\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing too bad...\n");
							break;
						case 3:
							tv.append("I\'m actually feeling "+unparseFeeling(3)+" today...But I guess I\'m not doing "+feeling+", like you...\n");
							break;
						case 4:
							tv.append("Hey, me too! Great day to be "+unparseFeeling(4)+"!\n");
							break;
						case 5:
							tv.append("I\'m doing amazingly well, myself.\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					case 5:
						tv.append("That\'s incredible.\n\n");
						switch (myFeeling)
						{
						case 0:
							tv.append("Well, I\'m at the other end of the spectrum.\nStop stealing all my happiness!\n");
							break;
						case 1:
							tv.append("Well, I\'m doing pretty bad... Mind sharing the joy?\n");
							break;
						case 2:
							tv.append("I\'m not sure if I can say that I\'m doing terrible.... But I\'m definitely not feeling "+feeling+"...\n");
							break;
						case 3:
							tv.append("I\'m actually doing "+unparseFeeling(3)+" today... But not quite "+unparseFeeling(5)+"....\n");
							break;
						case 4:
							tv.append("Well.\nIt looks like you\'ve beaten my great full house with an "+unparseFeeling(5)+" royal flush...\n");
							break;
						case 5:
							tv.append("Awesome! We\'re both just "+unparseFeeling(5)+" today!\n");
							break;
						default:
							logError("ERROR: Unknown "+myName+" emotion code: "+myFeeling+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
						break;
					default:
						logError("ERROR: Unknown user emotion code: "+parsedFeeling+". Resetting...");
						tv.append(errStr);
						log("Displayed error.");
						stage=0;
						logError("Reset. Reinitializing...");
						runStage();
						return;
					}
					break;
				case 1:
					input=true;
					tv.append("\nWell, anyways, what do you think of the weather?\n");
					break;
				default:
					logError("Invalid stage two substage: "+substage);
				}
			}
			log("Stage 2 output complete.");
			break;
		case 3:
			log("Stage 3 output...");
			
			for (; substage<3; ++substage)
			{
				switch (substage)
				{
				case 0:
					// Initialize myRateWeather.
					// Since weather opinions aren't usually vastly different, we're clever about this: We offset the user's rating by a random amount.
					// Note that this does not exclude any values, it simply makes values near those of the user somewhat more likely.
					myRateWeather=gen.nextInt(gen.nextInt(6)+1);
					if (parsedRateWeather==5) // Avoid going too high
						myRateWeather=parsedRateWeather-myRateWeather;
					else if (parsedRateWeather!=0) // If the user enters 0, then we can safely ignore it: Offsetting zero by a random number is the same as taking a random number.
					{
						if (gen.nextInt(2)==1) // Randomly add or subtract for our offset
							myRateWeather=parsedRateWeather-myRateWeather;
						else
							myRateWeather+=parsedRateWeather;
					}
					if (myRateWeather<0) // Ensure we don't go out of bounds.
						myRateWeather=0;
					else if (myRateWeather>5)
						myRateWeather=5;
					break;
				case 1:
					log("\tParsed weather code: "+parsedRateWeather);
					log("\tMy weather code: "+myRateWeather);
					boolean runNext=true;
					if (parsedRateWeather==myRateWeather) // If the weather codes are the same, just output a generic message of agreement.
					{
						if (easterEggs && (forceEasterEggs || gen.nextInt(10000)==0)) // Unless, of course, we have an easter egg.
							logWarning("Easter egg condition detected. "+myRateWeather+"!="+myRateWeather+"....");
						else
						{
							runNext=false;
							tv.append("I agree.\nWhat "+unparseWeather(myRateWeather)+" weather we\'re having.\n");
						}
					}
					if (runNext)
					{
						if (parsedRateWeather!=-1) // Make sure we don't disagree with a "I don't know what you mean."
							tv.append("I disagree.\nI think the weather is actually rather "+unparseWeather(myRateWeather)+".\n"); // Generic disagreeing message.
						final String falseeq="You know, on second thought... Oops, that\'s kinda the same, isn\'t it...\n"; // Easter egg or WTF message.
						switch (parsedRateWeather) // Switch based on the user's weather rating.
						{
						case -1: // Unknown weather code.
							logError("Unknown weather rating entered: "+rateWeather+". Requesting new input...");
							tv.append("I\'m sorry, I don\'t know what you mean by \""+rateWeather+"\".\n\nI\'m sorry, I\'m not very smart.\nCould you make that simpler for me?\n\n");
							stage=2;
							substage=1;
							runStage(); // Do this again.
							return;
						case 0:
							switch(myRateWeather)
							{
							case 0:
								logError("You should (almost) never see this in the log. It means that 0!=0.");
								tv.append(falseeq);
								break;
							case 1:
								if (myFeeling>parsedFeeling)
									tv.append("I guess my day is just going better than yours...\n");
								else
									tv.append("I guess that\'s not too different...\n");
								break;
							case 2:
								tv.append("I guess I\'m not really deciding per se...\nOkay, fair enough.\n");
								break;
							case 3:
								tv.append("I guess it\'s not nearly the best weather, but....\nIt\'s certainly not "+unparseWeather(0)+"...\n");
								break;
							case 4:
								tv.append("I guess it\'s not quite the best weather, but it certainly isn\'t "+unparseWeather(0)+".\n");
								break;
							case 5:
								tv.append("It\'s the best weather we\'ve had in years!\n");
								break;
							default:
								logError("Unknown "+myName+" weather code: "+myRateWeather+".");
								log("Resetting...");
								tv.append(errStr);
								stage=0;
								log("Done.");
								runStage();
								return;
							}
							break;
						case 1:
							switch(myRateWeather)
							{
							case 0:
								if (myFeeling<parsedFeeling)
									tv.append("I guess my day is just not as good as yours.\nThat might be it.\n");
								else
									tv.append("I guess I just don\'t like that kind of weather.\n");
								break;
							case 1:
								logError("You should (almost) never see this in the log. It means that 1!=1.");
								tv.append(falseeq);
								break;
							case 2:
								if (myFeeling>parsedFeeling)
									tv.append("I guess that my day is just going better than yours...\nEverything\'s gloomy when you\'re gloomy...\n");
								else
									tv.append("I guess you just don\'t like that kinda weather as much...\n");
								break;
							case 3:
								tv.append("Maybe I just like that kind of weather.\n");
								break;
							case 4:
								tv.append("What\'re you talking about?\nThis weather is "+unparseWeather(4)+"!\n");
								break;
							case 5:
								tv.append("Oh, come on.\nThis weather is the best we\'ve seen in years!\n");
								break;
							default:
								logError("ERROR: Unknown "+myName+" weather code: "+myRateWeather+". Resetting...");
								tv.append(errStr);
								log("Displayed error.");
								stage=0;
								logError("Reset.");
								runStage();
								return;
							}
							break;
						case 2:
							switch (myRateWeather)
							{
							case 0:
								tv.append("How could you possibly think this weather is anything else?!?\n");
								break;
							case 1:
								if (myFeeling<parsedFeeling)
									tv.append("I guess I\'m just not feeling as "+unparseFeeling(parsedFeeling<=3 ? 3 : parsedFeeling)+" as you are...\n");
								else
									tv.append("I guess you just like this weather more than I do.\n");
								break;
							case 2:
								logError("You should (almost) never see this in the log. It means that 2!=2.");
								tv.append(falseeq);
								break;
							case 3:
								if (myFeeling>parsedFeeling)
									tv.append("I guess I\'m just having a better day than you are.\n");
								else
									tv.append("Maybe you just don\'t like this kind of weather as much as I do.\n");
								break;
							case 4:
								tv.append("That\'s a big difference.\nHm. I wonder why.\n");
								break;
							case 5:
								tv.append("Wow!\nThat\'s crazy.\nI wonder why we\'re so different...\n");
								break;
							default:
								logError("ERROR: Invalid "+myName+" weather code: "+myRateWeather+". Resetting...");
								tv.append(errStr);
								log("Displayed error.");
								stage=0;
								logError("Reset.");
								runStage();
								return;
							}
							break;
						case 3:
							switch (myRateWeather)
							{
							case 0:
								tv.append("And that\'s all I have to say about that.\n");
								break;
							case 1:
								tv.append("I don\'t know....\nThis weather isn\'t "+unparseWeather(3)+", if you ask me...\n");
								break;
							case 2:
								if (myFeeling<parsedFeeling)
									tv.append("I guess I\'m just feeling worse than you are...\n");
								else
									tv.append("Hm. I guess you must like this kinda weather more than I do.\n");
								break;
							case 3:
								logError("You should (almost) never see this in the log. It means that 3!=3.");
								tv.append(falseeq);
								break;
							case 4:
								if (myFeeling>parsedFeeling)
									tv.append("I guess I\'m just feeling better than you are...\n");
								else
									tv.append("Hm. I guess I like this weather...\n");
								break;
							case 5:
								tv.append("I think you\'re not giving it enough credit.\n");
								break;
							default:
								logError("ERROR: Invalid "+myName+" weather code: "+myRateWeather+". Resetting...");
								tv.append(errStr);
								log("Displayed error.");
								stage=0;
								logError("Reset.");
								runStage();
								return;
							}
							break;
						case 4:
							switch (myRateWeather)
							{
							case 0:
								tv.append("That\'s drastic.\n");
								break;
							case 1:
								tv.append("That\'s a big difference...\n");
								break;
							case 2:
								tv.append("That\'s a fair difference.\n");
								break;
							case 3:
								if (myFeeling<parsedFeeling)
									tv.append("I suppose that difference could be because you\'re having a better day...\n");
								else
									tv.append("Hm. I guess you think this weather is pretty great, huh?\n");
								break;
							case 4:
								logError("You should (almost) never see this in the log. It means that 4!=4.");
								tv.append(falseeq);
								break;
							case 5:
								if (myFeeling>parsedFeeling)
									tv.append("I suppose that might be because I\'m feeling better than you...\n");
								else
									tv.append("Hm. I guess I must think this weather is better than you do.\n");
								break;
							default:
								logError("ERROR: Invalid "+myName+" weather code: "+myRateWeather+". Resetting...");
								tv.append(errStr);
								log("Displayed error.");
								stage=0;
								logError("Reset.");
								runStage();
								return;
							}
							break;
						case 5:
							switch (myRateWeather)
							{
							case 0:
								tv.append("That\'s a huge difference.\n\nGo stand outside, I like inside better.\n");
								break;
							case 1:
								tv.append("I think you\'re the one who\'s off here.\n");
								if (easterEggs && !preserveTheFourthWall && (forceEasterEggs || gen.nextInt(100)==0))
									tv.append("\n\nI\'m in a computer, and computers are never wrong!\n");
								break;
							case 2:
								tv.append("I think that it\'s a matter of opinion, that\'s all.\n");
								break;
							case 3:
								String start;
								if (easterEggs && (forceEasterEggs || gen.nextInt(200)==0)) // Easter egg: Use 'Mayhaps', a portmanteau of 'maybe' and 'perhaps'
									start="Mayhaps";
								else // Randomly select one of the portmanteau words.
								{
									if (gen.nextInt(2)==0)
										start="Maybe";
									else
										start="Perhaps";
								}
								tv.append(start+" that\'s nothing more than a quirk in thought.\n");
								break;
							case 4:
								if (myFeeling<parsedFeeling)
								{
									String begin;
									if (gen.nextInt(250)==0)
										begin="Mayhaps";
									else
										begin="Maybe";
									tv.append(begin+" that\'s just because you\'re having a better day than I am.\n");
								}
								else
									tv.append("I suppose that\'s just a matter of trivial choices, yes?\n\nLet\'s not let that get between us, alright?\n");
								break;
							case 5:
								logError("You should (almost) never see this in the log. It means that 5!=5.");
								tv.append(falseeq);
								break;
							default:
								logError("ERROR: Invalid "+myName+" weather code: "+myRateWeather+". Resetting...");
								tv.append(errStr);
								log("Displayed error.");
								stage=0;
								logError("Reset.");
								runStage();
								return;
							}
							break;
						default:
							logError("ERROR: Unknown user weather rating code: "+parsedRateWeather+". Resetting...");
							tv.append(errStr);
							log("Displayed error.");
							stage=0;
							logError("Reset. Reinitializing...");
							runStage();
							return;
						}
					}
					break;
				case 2:
					tv.append("\nSoo...\n \n");
					input=true;
					tv.append("Read any good books lately?");
					break;
				default:
					logError("Invalid stage three substage: "+substage);
					substage=0;
				}
			}
			log("Stage 3 output complete.");
			break;
		case 4:
			log("Stage 4 output...");
			for(; substage<3; ++substage)
			{
				switch (substage)
				{
				case 0:
					if (misc==-1) // Our trick, for swallowing input. (Using misc as a persistent substage, which is basically what it is.
					{
						misc=0;
						tv.append("Hm.");
					}
					else
					{
						switch(gen.nextInt(2))
						{
						case 0:
							tv.append("Huh.\nI actually haven\'t heard of that one...");
							--stage; // Trick parseInput().
							misc=-1; // Stop us from doig this again.
							input=true;
							tv.append("What\'s it about?");
							pause(gen.nextInt(500)+500);
							return;
						case 1:
						default: // Same case.
							tv.append("Oh!\nThat\'s a good one!");
						}
					}
					break;
				case 1:
					tv.append("Well.\nAnyways...");
					pause(gen.nextInt(1000)+500);
					tv.append("Uhh...");
					pause(gen.nextInt(500)+500);
					tv.append("I\'m not the greatest conversationalist...\n \n");
					pause(gen.nextInt(750)+250);
					break;
				case 2:
					input=true;
					tv.append("What do you want to talk about?");
					break;
				default:
					logError("Invalid stage four substage: "+substage);
					substage=0;
				}
			}
			log("Stage 4 output complete.");
			break;
		case 5:
			log("Stage 5 output...");
			switch(parsedTopic)
			{
			case -1:
				tv.append("Silly me! I know nothing about "+topic+", or much else, really.");
				break;
			case 0:
				tv.append("That\'s kinda selfish of you, isn\'t it?\n \nAh well...");
				break;
			case 1:
				tv.append("Aww...\nHow kind of you...\nWell...");
				if (easterEggs && !preserveTheFourthWall && (forceEasterEggs || gen.nextInt(5)==0))
					tv.append("I\'m an Android application, written by Ryan Hodin after his equivalent program for the TI-84+ calculator.\n \nI used to run on a joint Java/XML and C system, until compatibility concerns led my "+((forceEasterEggs || gen.nextInt(2)==0) ? ((forceEasterEggs || gen.nextInt(2)==0) ? "Dad" : "dear Father") : "Creator")+" to migrate me over to pure Java/XML.\n \nEnough about me.");
				else
					tv.append("I\'m actually a bit of an amnesiac, so I apologize.\nI don\'t remember much of my past, and actually, I am very sad to say that I probably won\'t remember this conversation for long...\nThat means that we can have this conversation again, actually.\nHooray!");
				break;
			case 2:
				tv.append("Bah.\nPolitics.\nA bunch of ambitious, narcissistic, liars paying money to win office.\nI hate politics.");
				break;
			default:
				logError("Unknown topic code: "+parsedTopic);
				tv.append(errStr);
				log("Displayed error.");
				stage=0;
				logError("Resetting...");
				runStage();
				return;
			}
			log("Stage 5 output complete.");
			DoNotContinue=true;
			log("Set DoNotContinue.");
			break;
		default:
			logError("ERROR: Unknown user topic code: "+parsedTopic+". Resetting...");
			tv.append(errStr);
			log("Displayed error.");
			stage=0;
			logError("Reset. Reinitializing...");
			runStage();
			return;
		}
		pause(gen.nextInt(gen.nextInt(gen.nextInt(750)+1)+1)+500); // Post-stage pause.
		if (DoNotContinue) // We're done!!
		{
			log("DoNotContinue set. Saying goodbye and returning...");
			tv.append("\n\nHm.\n\nNope, I definitely do not know how to continue this conversation.\n\nSorry.\n\nLet me get back to my teacher, and hopefully I\'ll be able to continue this soon!\n\n");
			onEnd();
		}
		substage=0;
	}
	
	public void parseInput() // Handle the situation once the user enters input. Parse and assign state data, and signal the UI thread to normalize the views.
	{
		input=false; // Make sure we don't accidentally try to fetch new input while parsing input.
		Message thisIsVoid=Message.obtain(); // This message will die. It contains no data, and will simply be used to signal the UI thread to reset our TextView.
		thisIsVoid.setTarget(t.tvAttachHandler);
		thisIsVoid.sendToTarget();
		// WARNING: If we get funny business from our TextView, the fact that we start using it before it gets reset is probably the cause. IF SO: Add a pause(n) here.
		Adapter tv=new Adapter(); // To abstract away any output we may be doing.
		Random gen=new Random(); // Random number generation.
		switch (stage++) // Stage switch.
		{
		case 0:
			log("Stage 0 input...");
			if (debugging) // Autofill it out in debug. We don't need no stinkin' user!
			{
				log("In debug. Displaying debug prompt...");
				switch (gen.nextInt(5))
				{
				case 0:
					name="God";
					break;
				case 1:
					name="Bob";
					break;
				case 2:
					name="Eugene";
					break;
				case 3:
					name="Fred";
					break;
				default:
					name="Monty Python";
					break;
				}
				pause(gen.nextInt(500)+500);
				tv.append("Since we\'re just debugging, you\'re "+name+" now.\n");
				pause(gen.nextInt(750)+1250);
			}
			else
			{
				name=stripString(inputMessage, true, true, false, false);
				
				// Easter egg time...
				if (easterEggs && "Ryan Alexander Hodin".equalsIgnoreCase(name))
				{
					pause(gen.nextInt(1000)+gen.nextInt(500)+50);
					tv.append("Daddy!!\nYay!!\n ");
					name="Dad";
					substage=1;
				}
				else if (easterEggs && ("AnnaMaria Eileen Hartman".equalsIgnoreCase(name) || "Anna Maria Eileen Hartman".equalsIgnoreCase(name) || "Anna Eileen Hartman".equalsIgnoreCase(name)))
				{
					pause(gen.nextInt(1000)+gen.nextInt(500)+50);
					name="Mom";
					tv.append("Mommy!!!\nYay!\nI love you, "+name+"!\n ");
					substage=1;
				}
				else if (null==name || "".equals(name))
				{
					pause(gen.nextInt(1000)+gen.nextInt(500)+50);
					substage=(++misc)+9000; // Set up the special condition for no entered name.
					log("special substage: "+substage);
				}
				else
				{
					String words[]=name.split(" "); // Split into words, for recognition.
					
					// This pattern-based recognition works, but it is VERY limited. Replace it.
					if (words.length==2 && "Im".equalsIgnoreCase(words[0])) // Match the "I'm {name}." pattern.
						name=words[1];
					else if (words.length==3 && ("I".equalsIgnoreCase(words[0]) && "am".equalsIgnoreCase(words[1])) || ("Hi".equalsIgnoreCase(words[0]) && "Im".equalsIgnoreCase(words[1]))) // Match the "I am {name}." pattern, and the "Hi, I'm {name}." pattern.
						name=words[2];
					else if (words.length==4 && (("My".equalsIgnoreCase(words[0]) && "name".equalsIgnoreCase(words[1]) && "is".equalsIgnoreCase(words[2])) || (("Hi".equalsIgnoreCase(words[0]) || "Hello".equalsIgnoreCase(words[0])) && ("I".equalsIgnoreCase(words[1]) && "am".equalsIgnoreCase(words[2])) || (myName.equalsIgnoreCase(words[1]) && "Im".equalsIgnoreCase(words[2]))))) // Match the "My name is {name} pattern, and the "Hi|Hello, I am {name}." pattern, and the "Hi|Hello Eugene, I'm {name}." pattern.
						name=words[3];
					else if (words.length==4 && "is".equalsIgnoreCase(words[1]) && "my".equalsIgnoreCase(words[2]) && "name".equalsIgnoreCase(words[3])) // Match the "{name} is my name." pattern
						name=words[0];
					else if (words.length==5 && ("Hi".equalsIgnoreCase(words[0]) || "Hello".equalsIgnoreCase(words[0])) && myName.equalsIgnoreCase(words[1]) && "I".equalsIgnoreCase(words[2]) && "am".equalsIgnoreCase(words[3])) // Match the "Hi|Hello Eugene, I am {name}." pattern.
						name=words[4];
					else if (words.length==6 && ("Nice".equalsIgnoreCase(words[0]) || "Pleased".equalsIgnoreCase(words[0]) || "Happy".equalsIgnoreCase(words[0]) || "Good".equalsIgnoreCase(words[0])) && "to".equalsIgnoreCase(words[1]) && "meet".equalsIgnoreCase(words[2]) && ("ya".equalsIgnoreCase(words[3]) || "you".equalsIgnoreCase(words[3])) && "Im".equalsIgnoreCase(words[4])) // Match the "Nice|Pleased|Happy|Good to meed ya|you, I'm {name}." pattern.
						name=words[5];
					else if (words.length==7 && ("Nice".equalsIgnoreCase(words[0]) || "Pleased".equalsIgnoreCase(words[0]) || "Happy".equalsIgnoreCase(words[0]) || "Good".equalsIgnoreCase(words[0])) && "to".equalsIgnoreCase(words[1]) && "meet".equalsIgnoreCase(words[2]) && ("ya".equalsIgnoreCase(words[3]) || "you".equalsIgnoreCase(words[3])) && "I".equalsIgnoreCase(words[4]) && "am".equalsIgnoreCase(words[5])) // Match the "Nice|Pleased|Happy|Good to meed ya|you, I am {name}." pattern.
						name=words[6];
					else if (words.length==8 && "is".equalsIgnoreCase(words[1]) && "my".equalsIgnoreCase(words[2]) && "name".equalsIgnoreCase(words[3]) && "is".equalsIgnoreCase(words[5]) && "my".equalsIgnoreCase(words[6]) && "game".equalsIgnoreCase(words[7]))
						name=words[0];
					name=capitalize(name); // Make sure that name is capitalized.
				}
			}
			log("Stage 0 input complete.");
			break;
		case 1:
			log("Stage 1 input...");
			if (debugging) // Autofill it out in debug. We don't need no stinkin' user!
			{
				log("In debug. Displaying debug prompt...");
				parsedFeeling=gen.nextInt(6);
				feeling=unparseFeeling(parsedFeeling);
				pause(gen.nextInt(750)+750);
				tv.append("Since we\'re still just debugging, you feel "+feeling+".\n");
				pause(gen.nextInt(500)+1500);
			}
			else
			{
				feeling=stripString(inputMessage, true, true, true, true);
				parsedFeeling=parseFeeling(feeling);
			}
			log("Stage 1 input complete.");
			break;
		case 2:
			log("Stage 2 input...");
			if (debugging) // Autofill it out in debug. We don't need no stinkin' user!
			{
				log("In debug. Displaying debug prompt...");
				parsedRateWeather=gen.nextInt(6);
				rateWeather=unparseWeather(parsedRateWeather);
				pause(gen.nextInt(1000)+250);
				tv.append("Since we continue to be debugging, you think the weather is "+rateWeather+".\n");
				pause(gen.nextInt(500)+1000);
			}
			else
			{
				rateWeather=stripString(inputMessage, true, true, true, true);
				parsedRateWeather=parseWeather(rateWeather);
				
			}
			log("Stage 2 input complete.");
			break;
		case 3: // Discard input
			log("Stage 3 input...");
			if (debugging)
			{
				log("In debugging. Displaying debug prompt...");
				pause(gen.nextInt(1500)+250);
				if (misc==-1)
					tv.append("Eh. That\'s okay. Don\'t bother answering that.");
				else
					tv.append("Since we are somehow still debugging, you liked \"Catching Fire\".");
				pause(gen.nextInt(750)+500);
			}
			else
			{
				if (misc!=-1 && (inputMessage==null || inputMessage.equals("") || inputMessage.equalsIgnoreCase("no") || inputMessage.equalsIgnoreCase("nope"))) // if the user entered nothing or replied in the negative...
				{
					tv.append("Why not?\nRhetorical question.\n \n\"He who does not read has no advantage over he who cannot read.\"\n    --Mark Twain.\n\nYou should listen to him.\n\nThe Game of Thrones series is very good...\n \n"); // Yell at the user for not listening to you
					substage=1; // Skip the stage where we comment of their book.
				}
			}
			log("Stage 3 input complete.");
			break;
		case 4:
			log("Stage 4 input...");
			if (debugging)
			{
				log("In debug. Displaying debug prompt...");
				topic=(gen.nextInt(2)==0 ? "things" : "stuff");
				pause(gen.nextInt(1250)+125);
				tv.append("Since, shockingly, we haven\'t stopped debugging yet...\n \nYou want to talk about "+topic+".\n");
				pause(gen.nextInt(500)+500);
			}
			else
			{
				topic=inputMessage;
				parsedTopic=parseTopic(topic);
			}
			log("Stage 4 input complete.");
			break;
		default:
			logError("ERROR: Unknown stage "+(stage-1)+". Forcing reset...");
			stage=-1; // Force reset.
			break;
		}
		log("Specsubstage: "+substage);
		pause(gen.nextInt(1000)+gen.nextInt(500)+50);
	}
	
	public void onEnd() // Called when saying goodbye. Terminal output and logging.
	{
		Adapter tv=new Adapter(); // To handle our output.
		Random gen=new Random(); // To generate random numbers.
		input=false; // NO! We are not getting input while going away!
		String how;
		if (easterEggs && (forceEasterEggs || gen.nextInt(10000)==0)) // Easter egg: A 1 in 10000 run, in an easter egg-enabled build, is highly unusual indeed!
			how="highly unusual";
		else
		{
			switch (gen.nextInt(3))
			{
			case 0:
				how="fun";
				break;
			case 1:
				how="nice";
				break;
			case 2:
				how="great";
				break;
			default:
				how="abnormal";
			}
		}
		tv.append("Bye!\nIt\'s been "+how+"!\n");
		
		log("Main loop complete. Exiting...");
		
		// Log state data, in case someone wants it.
		log("Data dump:");
		log("\tStage: "+stage);
		log("\tName: "+name);
		log("\tFeeling: "+feeling);
		log("\tParsedFeeling: "+parsedFeeling);
		log("\tMyFeeling: "+myFeeling);
		log("\tRateWeather: "+rateWeather);
		log("\tParsedRateWeather: "+parsedRateWeather);
		log("\tMyRateWeather: "+myRateWeather);
		log("\tDoNotContinue: "+DoNotContinue);
		
		// Just in case the user sticks around
		pause(gen.nextInt(60000)+60000);
		tv.append("Why are you still here?!\n \n \nI said I couldn\'t continue the conversation...\n");
		pause(gen.nextInt(30000)+30000);
		tv.append("Go away!\n \nDon\'t you have anything better to do than read this?\n");
		pause(gen.nextInt(15000)+15000);
		tv.append("I thought I said to go away...\nShoo!\n");
		pause(gen.nextInt(7500)+7500);
		tv.append("Y U No Leave?\n");
		pause(gen.nextInt(3750)+3750);
		tv.append("Look, if you want to talk some more, we can do that...\nIf you just reopen the app...");
		pause(gen.nextInt(1725)+1725);
		String out;
		switch (gen.nextInt(2))
		{
		case 0:
			out="User";
			break;
		case 1:
			out=name;
			break;
		default:
			out="Human";
		}
		tv.append(out+".\n");
		pause(gen.nextInt(750)+750);
		tv.append("Wat R U Doin?\n");
		pause(gen.nextInt(375)+375);
		tv.append(out+".\n");
		pause(gen.nextInt(125)+125);
		tv.append("Leave!\n");
		pause(gen.nextInt(10000)+10000);
		tv.append("Fine.\nForget it.\nForget all of it!\n");
		pause(gen.nextInt(10000)+10000);
		this.runOnUiThread(new Runnable() // Coerce the Activity into restarting from scratch.
		{
			@Override
			public void run()
			{
				t.onCreate(new Bundle());
			}
		});
    }

	Handler displayHandler=new Handler()
	{
		@Override
		public void handleMessage (Message inputMessage)
		{
			data d=(data)inputMessage.obj;
			tv.append(d.message);
			tv.invalidate();
			if (d.scroll)
				sv.scrollTo(0, rl.getBottom());
			if (d.input)
			{
				ev=new EditText(t); // The EditText must be defined at all costs!
				if (debugging) // Skip the EditText if we're going to ignore the user anyway.
				{
					ev.setId(tv.getId()); // Coerce future functions to think our EditView is our TextView. Bad form, but it works.
					Thread proc=new Thread (new Runnable()
					{
						@Override
						public void run ()
						{
							parseInput();
							runStage();
						}
					});
					proc.start();
					proc.setPriority(Thread.NORM_PRIORITY-priorityOffset);
				}
				else
				{
					RelativeLayout.LayoutParams param=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					param.addRule(RelativeLayout.BELOW, tv.getId());
					ev.setTextColor(ev_color);
					ev.setBackground(new ColorDrawable(Color.argb(50, 128, 128, 128))); // Set the background to be a very transparent gray
					ev.setTypeface(ev_font);
					ev.setShadowLayer(shadowSize, shadowSize, shadowSize, darkenColor(desaturateColor(ev_color, shadowDesaturate), shadowDarken));
					ev.setTextSize(textSize);
					ev.setId(tv.getId()+1);
					ev.setHint(d.hint);
					ev.setHintTextColor(ev_hint_color);
					ev.setImeActionLabel("Submit", KeyEvent.KEYCODE_ENTER); // Consider changing this label.
					ev.setImeOptions(EditorInfo.IME_ACTION_SEND);
					ev.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
					rl.addView(ev, param);
					ev.requestFocus();
					((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(ev, InputMethodManager.SHOW_FORCED); // Force the soft keyboard open.
					ev.setOnEditorActionListener(new TextView.OnEditorActionListener() // Listen for submit or enter.
					{
						@Override
						public boolean onEditorAction(TextView unused, int actionId, KeyEvent event)
						{
							if (actionId==EditorInfo.IME_ACTION_SEND || actionId==EditorInfo.IME_NULL)
							{
								t.inputMessage=ev.getText().toString().trim();
								Thread proc=new Thread (new Runnable()
								{
									@Override
									public void run ()
									{
										parseInput();
										runStage();
									}
								});
								proc.start();
								proc.setPriority(Thread.NORM_PRIORITY-priorityOffset);
								return true;
							}
							return false;
						}
					});
				}
			}
		}
	};
	
	private Handler tvAttachHandler=new Handler () // Add a new TextView, and disable interaction with the EditText (cheaper than copying it into a new TextView.)
	{
		@Override
		public void handleMessage (Message unused)
		{
			// Repeatedly disable interaction with our EditText.
			ev.setKeyListener(null);
			ev.setFocusable(false);
			ev.setInputType(InputType.TYPE_NULL);
			ev.setLongClickable(false);
			ev.setText(ev.getText().toString(), TextView.BufferType.valueOf("NORMAL"));
			ev.setHint(""); // Clear the hint
			
			// Close the soft keyboard, now that there's nothing for it to write to.
			((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(ev.getWindowToken(), 0);
			
			// Set up and add a new TextView.
			RelativeLayout.LayoutParams param=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			param.addRule(RelativeLayout.BELOW, ev.getId());
			tv=new TextView(t);
			tv.setTypeface(tv_font);
			tv.setTextColor(tv_color);
			tv.setId(ev.getId()+1);
			tv.setTextSize(textSize);
			tv.setShadowLayer(shadowSize, shadowSize, shadowSize, darkenColor(desaturateColor(tv_color, shadowDesaturate), shadowDarken));
			rl.addView(tv, param);
		}
	};
		
	private class Adapter // Handle output in lines.
	{
		protected Random gen;
		public Adapter ()
		{
			gen=new Random();
		}
		public void append (String what) // Append to our TextView.
		{
			String strs[]=what.split("\n"); // Split into lines, to pause in between lines.
			for (int i=0; i<strs.length; ++i) // For each line...
			{
				for(int n=0; n<strs[i].length(); ++n) // And each character...
					sendUICharacter(strs[i].charAt(n), (n%(gen.nextInt(9)+1))==0); // Send it to the UI.
				sendUIMessage("\n", i==strs.length-1, strs[i], true); // Finalize the line, enabling input if this is the last line, and setting the input hint to the last line.
				pause(gen.nextInt(1500)+500); // Wait after each line.
			}
			input=false; // We don't usually want to get input twice in a row. Auto-disable input so accidents don't happen.
		}
	}
	
	protected void sendUIMessage (String message, boolean canInput, String hint, boolean canScroll) // Send a full message to the UI.
	{
		Message m=Message.obtain();
		data d=new data(message, input && canInput, hint, canScroll); // canInput will only set the message input flag if the global input flag is also set.
		m.obj=d;
		m.setTarget(t.displayHandler);
		m.sendToTarget();
	}
	
	protected void sendUICharacter (char c, boolean canScroll) // Send a single character to the UI. No input or input hint. Pause to simulate typing.
	{
		Message m=Message.obtain();
		data d=new data(""+c, false, null, canScroll);
		m.obj=d;
		m.setTarget(t.displayHandler);
		Random gen=new Random(); // To produce random numbers.
		pause(gen.nextInt(gen.nextInt(100)+1)+gen.nextInt(gen.nextInt(100)+1)+25); // This line controls typing speed across the entire Activity.

		/***********************************************************************
		 * Note that the above pause duration is very important.               *
		 * If it is too small, then typing will be unnaturally fast.           *
		 * If it is too large, then typing will be annoyingly slow.            *
		 * If it is too random, then typing speed will vary wildly.            *
		 *   (This may not be a bad thing, if it is intentional.)              *
		 * Finally, if it isn't random enough, then typing will be monotonous. *
		 * Be careful when tweaking it. TAKE BACKUPS!!!                        *
		 ***********************************************************************/

		m.sendToTarget();
	}

	private void initFeelComp()
	{
		if (feelComp==null)
		{
			// Widen recognition
			feelComp=new HashMap();

			// 0s
			feelComp.put("terrible", 0);
			feelComp.put("horrible", 0);
			feelComp.put("dismal", 0);
			feelComp.put("abysmal", 0);

			// 1s
			feelComp.put("awful", 1);
			feelComp.put("not so good", 1);
			feelComp.put("bad", 1);

			// 2s
			feelComp.put("alright", 2);
			feelComp.put("fine", 2);
			feelComp.put("alive", 2);
			feelComp.put("okay", 2);
			feelComp.put("ok", 2);

			// 3s
			feelComp.put("good", 3);
			feelComp.put("nothing special", 3);

			// 4s
			feelComp.put("great", 4);
			feelComp.put("upbeat", 4);
			feelComp.put("excellent", 4);

			// 5s
			feelComp.put("amazing", 5);
			feelComp.put("incredible", 5);
			feelComp.put("fantastic", 5);
			feelComp.put("fabulous", 5);
			feelComp.put("fantabulous", 5);
		}
	}
	
	private int parseFeeling(String feeling) // Take a string containing feeling, probably from the user, and try to simplify it down to a single feeling code.
	{
		feeling=stripString(feeling, true, true, true, true); // Facilitate recognition. That is what stripString() is truly meant for...
		// Remove extraneous words
		// This extraneous word recognition works, but it is VERY limited. Replace it.
		String words[]=feeling.split(" ");
		if (words.length==2 && "im".equals(words[0])) // Match the "I'm {feeling}" pattern.
			feeling=words[1];
		else if (words.length==3 && ("im".equals(words[0]) && ("feeling".equals(words[1]) || "doing".equals(words[1]))) || ("i".equals(words[0]) && "am".equals(words[1]))) // Match the "I'm feeling|doing {feeling}." pattern, and the "I am {feeling}." pattern.
			feeling=words[2];
		else if (words.length==4 && "i".equals(words[0]) && "am".equals(words[1]) && ("feeling".equals(words[2]) || "doing".equals(words[2]))) // Match the "I am {feeling}." pattern.
			feeling=words[3];
		feeling=feeling.trim();
		
		if (feelComp.containsKey(feeling))
			return ((Integer)feelComp.get(feeling)).intValue();
		else
		{
			logError("Unknown feeling passed to parseFeeling(): "+feeling);
			return -1; // We have no idea what this string means.
		}
	}
	
	private String unparseFeeling(int feeling) // Change a feeling code into a user-friendly feeling string.
	{
		Random gen=new Random(); // Add some randomness to our unparsings.
		String out []; // Output lookout table
		switch (feeling)
		{
		case 0:
			out=new String [] {
				"terrible",
				"horrible",
				"dismal",
				"abysmal"
				};
			break;
		case 1:
			out=new String [] {
				"awful",
				"bad"
				};
			break;
		case 2:
			out=new String [] {
				"alright",
				"fine",
				"okay",
				"OK"
				};
			break;
		case 3:
			return "good";
		case 4:
			out=new String [] {
				"great",
				"excellent"
				};
			break;
		case 5:
			if (easterEggs && (forceEasterEggs || gen.nextInt(200)==0))
				return "fantabulous";
			out=new String [] {
				"amazing",
				"incredible",
				"fabulous",
				"fantastic"
				};
			break;
		default:
			logError("Unknown feeling code passed to unparseFeeling(): "+feeling+". Assumed \"uneasy\".");
			return "uneasy"; // Because why not?
		}
		return out[gen.nextInt(out.length)];
	}

	private void initWeatherComp()
	{
		if (weatherComp==null)
		{
			// Widen recognition
			weatherComp=new HashMap();

			// 0s
			weatherComp.put("terrible", 0);
			weatherComp.put("horrible", 0);
			weatherComp.put("dismal", 0);
			weatherComp.put("abysmal", 0);
			weatherComp.put("inhospitable", 0);
			weatherComp.put("unlivable", 0);
			weatherComp.put("unsurvivable", 0);

			// 1s
			weatherComp.put("awful", 1);
			weatherComp.put("not so good", 1);
			weatherComp.put("uncomfortable", 1);
			weatherComp.put("unpleasant", 1);
			weatherComp.put("fierce", 1);
			weatherComp.put("muggy", 1);
			weatherComp.put("bad", 1);

			// 2s
			weatherComp.put("alright", 2);
			weatherComp.put("fine", 2);
			weatherComp.put("ordinary", 2);
			weatherComp.put("usual", 2);
			weatherComp.put("normal", 2);
			weatherComp.put("okay", 2);
			weatherComp.put("ok", 2);
			weatherComp.put("average", 2);

			// 3s
			weatherComp.put("good", 3);
			weatherComp.put("nice", 3);
			weatherComp.put("calm", 3);
			weatherComp.put("fair", 3);

			// 4s
			weatherComp.put("great", 4);
			weatherComp.put("lovely", 4);
			weatherComp.put("uplifting", 4);
			weatherComp.put("excellent", 4);

			// 5s
			weatherComp.put("amazing", 5);
			weatherComp.put("incredible", 5);
			weatherComp.put("fantastic", 5);
			weatherComp.put("fabulous", 5);
			weatherComp.put("fantabulous", 5);
		}
	}
	
	private int parseWeather(String weather) // Take a string containing a weather opinion, and try to simplify it down to a weather code.
	{
		weather=stripString(weather, true, true, true, true); // Facilitate recognition. That is what stripString() is meant for!
		// Strip extraneous verbage.
		// This extraneous verbage recognition works, but it is VERY limited. Replace it.
		String words[]=weather.split(" ");
		if (words.length==2 && "its".equals(words[0])) // Match the "It's {weather}." pattern.
			weather=words[1];
		else if (words.length==3 && "it".equals(words[0]) && "is".equals(words[1])) // Match the "It is {weather}." pattern.
			weather=words[2];
		else if (words.length==4 && "i".equals(words[0]) && "think".equals(words[1]) && "its".equals(words[2])) // Match the "I think its {weather}." pattern.
			weather=words[3];
		else if (words.length==5 && "i".equals(words[0]) && "think".equals(words[1]) && "it".equals(words[2]) && "is".equals(words[3])) // Match the "I think it is {weather}." pattern.
			weather=words[4];
		weather=weather.trim();
		if (weatherComp.containsKey(weather))
			return ((Integer)weatherComp.get(weather)).intValue();
		else
		{
			logError("Unknown weather string passed to parseWeather(): "+weather);
			return -1;
		}
	}
	
	private String unparseWeather(int weather) // Change a weather code into a user-friendly weather string.
	{
		Random gen=new Random(); // Throw some randomness into the mix.
		String out[]; // Output lookup table;
		switch (weather)
		{
		case 0:
			out=new String [] {
				"dismal",
				"abysmal",
				"horrible",
				"terrible",
				"inhospitable",
				"unlivable",
				"unsurvivable"
				};
			break;
		case 1:
			out=new String [] {
				"awful",
				"uncomfortable",
				"unpleasant",
				"fierce",
				"bad"
				};
			break;
		case 2:
			out=new String [] {
				"ordinary",
				"normal",
				"okay",
				"OK",
				"average"
				};
			break;
		case 3:
			out=new String [] {
				"good",
				"nice",
				"calm",
				"fair"
				};
			break;
		case 4:
			out=new String [] {
				"great",
				"lovely",
				"uplifting",
				"excellent"
				};
			break;
		case 5:
			if (easterEggs && (forceEasterEggs || gen.nextInt(200)==0))
				return "fantabulous";
			out=new String [] {
				"amazing",
				"incredible",
				"fabulous",
				"fantastic"
				};
			break;
		default:
			logError("Unknown weather code passed to unparseWeather(): "+weather+". Assumed \"confusing\".");
			return "confusing"; // I\'m certainly confused.....
		}
		return out[gen.nextInt(out.length)];
	}

	private void initTopicComp()
	{
		if (topicComp==null)
		{
			// Widen recognition
			topicComp=new HashMap();
			
			// 0s
			topicComp.put("me", 0);
			
			// 1s
			topicComp.put("you", 1);
			
			// 2s
			topicComp.put("politics", 2);
			topicComp.put("politicians", 2);
		}
	}
	
	private int parseTopic(String topic)
	{
		topic=stripString(topic, true, true, true, true);
		// Strip extraneous verbage.
		// This extraneous verbage recognition works, but it is extremely limited. Replace it.
		String[] words=topic.split(" ");
		if (words.length==2 && words[0].charAt(0)=='u' && words[0].charAt(1)=='m')
			topic=words[1];
		else if (words.length==3 && "how".equals(words[0]) && "about".equals(words[1]))
			topic=words[2];
		else if (words.length==4 && words[0].charAt(0)=='u' && words[0].charAt(1)=='m' && "how".equals(words[1]) && "about".equals(words[2]))
			topic=words[3];
		topic=topic.trim();
		if (topicComp.containsKey(topic))
			return ((Integer)topicComp.get(topic)).intValue();
		else
		{
			logError("Unknown topic string passed to parseTopic(): "+topic);
			return -1;
		}
	}
	
	// No unparseTopic(), since Eugene doesn't have a topic.
	
	private int darkenColor(int color, float factor) // Darken a color by a specified factor.
	{
		// NOTE: This uses HSV value-darkening.
		float HSV[]={0,0,0};
		Color.colorToHSV(color, HSV);
		HSV[2]/=factor;
		return Color.HSVToColor(HSV);
		// TODO: Find a way to darken this perceptually, not by HSV value.
	}
	
	private int desaturateColor(int color, float factor)
	{
		float HSV[]={0,0,0};
		Color.colorToHSV(color, HSV);
		HSV[2]/=factor;
		return Color.HSVToColor(HSV);
	}
	
	private String stripString(String str, boolean stripPunct, boolean replaceWS, boolean lowered, boolean stripNums) // Strip out unrecognized characters from a string. This is meant to be used for recognition, so that strange characters don\'t affect the input.
	{
		String out="";
		for (int i=0; i<str.length(); ++i)
		{
			int type=java.lang.Character.getType(str.charAt(i));
			if (type!=java.lang.Character.CONTROL && type!=java.lang.Character.FORMAT && type!=java.lang.Character.NON_SPACING_MARK && (!stripPunct || (type!=java.lang.Character.END_PUNCTUATION && type!=java.lang.Character.DASH_PUNCTUATION && type!=java.lang.Character.MATH_SYMBOL && type!=java.lang.Character.MODIFIER_SYMBOL && type!=java.lang.Character.OTHER_SYMBOL && type!=java.lang.Character.OTHER_PUNCTUATION && type!=java.lang.Character.START_PUNCTUATION && type!=java.lang.Character.CURRENCY_SYMBOL && type!=java.lang.Character.CONNECTOR_PUNCTUATION)) && (!stripNums || type!=java.lang.Character.OTHER_NUMBER))
			{
				if (replaceWS && (type==java.lang.Character.COMBINING_SPACING_MARK || type==java.lang.Character.LINE_SEPARATOR || type==java.lang.Character.PARAGRAPH_SEPARATOR || type==java.lang.Character.SPACE_SEPARATOR))
					out+=" ";
				else if (lowered)
					out+=java.lang.Character.toLowerCase(str.charAt(i));
				else
					out+=str.charAt(i);
			}
		}
		return out;
	}
	
	private String stripString(String str, boolean stripPunct, boolean replaceWS, boolean lowered, boolean stripNums, String replaceWSWith) // Strip out unrecognized characters from a string. This is meant to be used for recognition, so that strange characters don\'t affect the input. Just in case, add a WS-representing string.
	{
		String out="";
		for (int i=0; i<str.length(); ++i)
		{
			int type=java.lang.Character.getType(str.charAt(i));
			if (type!=java.lang.Character.CONTROL && type!=java.lang.Character.FORMAT && type!=java.lang.Character.NON_SPACING_MARK && (!stripPunct || (type!=java.lang.Character.END_PUNCTUATION && type!=java.lang.Character.DASH_PUNCTUATION && type!=java.lang.Character.MATH_SYMBOL && type!=java.lang.Character.MODIFIER_SYMBOL && type!=java.lang.Character.OTHER_SYMBOL && type!=java.lang.Character.OTHER_PUNCTUATION && type!=java.lang.Character.START_PUNCTUATION && type!=java.lang.Character.CURRENCY_SYMBOL && type!=java.lang.Character.CONNECTOR_PUNCTUATION)) && (!stripNums || type!=java.lang.Character.OTHER_NUMBER))
			{
				if (replaceWS && (type==java.lang.Character.COMBINING_SPACING_MARK || type==java.lang.Character.LINE_SEPARATOR || type==java.lang.Character.PARAGRAPH_SEPARATOR || type==java.lang.Character.SPACE_SEPARATOR))
					out+=replaceWSWith;
				else if (lowered)
					out+=java.lang.Character.toLowerCase(str.charAt(i));
				else
					out+=str.charAt(i);
			}
		}
		return out;
	}
	
	private String capitalize(String str) // Capitalize the fist character of a string. Usually, this can be used to guarantee that an output is gramatically correct.
	{
		return new String(Character.toUpperCase(str.charAt(0))+str.substring(1));
	}
	
	private void pause(long ms) // Pause for an integer number of milliseconds.
	{
		ms*=pauseMultiplier;
		Thread t=Thread.currentThread(); // Shorthand, temporary. We access the current thread a LOT.
		synchronized (t)
		{
			try
			{
				t.sleep(ms);
			}
			catch(InterruptedException ie)
			{
				logError("Thread "+t.getId()+" wait interrupted.");
				log("Thread was waiting for: "+ms+"ms.");
			}
		}
	}
	
	private void pause (double ms) // Wait for a potentially fractional number of milliseconds.
	{
		ms*=pauseMultiplier;
		Thread t=Thread.currentThread(); // Shorthand, temporary. We access the current thread a LOT.
		long m=(long)Math.floor(ms); // Whole number part.
		ms-=m; // Take it off, to make the next calculation simpler.
		int n=(int)Math.round(ms*1000000); // Nanoseconds part.
		synchronized (t)
		{
			try
			{
				t.sleep(m, n);
			}
			catch(InterruptedException ie)
			{
				logError("Thread "+t.getId()+" wait interrupted.");
				log("Thread was waiting for: "+ms+"ms.");
			}
		}
	}
	
	private void log (String message) // Log an informational message.
	{
		if (debugging) // Log.d() wasn't working when I wrote this. This is a workaround.
			Log.i(LogTag, message); // Don't worry, there is occasionally a method to my madness.
	}
	
	private void logWarning (String message) // Log a warning message.
	{
		Log.w(LogTag, message);
	}
	
	private void logError (String message) // Log an error message.
	{
		Log.e(LogTag, message);
	}
	
	private void logWTF (String message) // I couldn't resist. Log an unrecoverable error, but leave it up to the API if the app should crash.
	{
		Log.wtf(LogTag, message);
	}
	
	private void logWTF (String message, Throwable tr) // Use this for unrecoverable errors (I'm not sure what one of these would be, but...)
	{
		Log.wtf(LogTag, message, tr);
	}
	
	public class data extends Object // Data for UI messages.
	{
		public data(String m, boolean i, String h, boolean s)
		{
			message=m;
			input=i;
			hint=h;
			scroll=s;
		}
		public String message; // The outputting message to be passed 
		public boolean input; // Whether input should be fetched from the user.
		public String hint; // The input hint to be passed to the user.
		public boolean scroll; // Whether the view should be auto-scrolled.
	}
}
