#if 0
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
 
 #define MAX_NAME 100
 
unsigned stage=0;
char name[1+MAX_NAME];
const char* errStr="I\'m sorry, I am experiencing an issue right now...\n";
const char* myName="Eugene";
unsigned char cont=1;

jstring
Java_com_ryan_eugene_HelloJni_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
	char* str;
	size_t len;
	switch (stage++)
	{
	case 0:
		str=malloc(strlen(myName)+strlen("Hello.\nI am .\nWhat\'s your name?\n")+1);
		sprintf(str, "Hello\nI am %s.\nWhat\'s your name?\n", myName);
		break;
	case 1:
		str=malloc(strlen("Nice to meet you, .\n")+strlen(name)+1);
		sprintf(str, "Nice to meet you, %s.\n", name);
		cont=0;
		break;
	default:
		str=malloc(strlen(errStr));
		strcpy(str, errStr);
		stage=0;
		break;
	}
    jstring out=(*env)->NewStringUTF(env, str);
    free(str);
    return out;
}

void Java_com_ryan_eugene_HelloJNI_sendInput(JNIEnv* env, jobject obj, jstring jdata)
{
	const char* data=(*env)->GetStringUTFChars(env, jdata, 0);
	switch (stage) // Here, case is the case to which we are sending input.
	{
	case 1:
		setName(data);
		break;
	default:
		stage=~0; // We know that the string function will soon be called. Break it.
		break;
	}
	(*env)->ReleaseStringUTFChars(env, jdata,data);
}

void setName (const char* str)
{
	strncpy (name, str, MAX_NAME);
}

jlong Java_com_ryan_eugene_HelloJNI_getStage (JNIEnv* env, jobject obj)
{
	return (jlong)stage;
}

jboolean Java_com_ryan_eugene_HelloJNI_shouldContinue (JNIEnv* env, jobject obj)
{
	return cont;
}
#endif
