#if !defined(__WIN32__) && !defined(_WIN32)
#include "org_f3_media_web_awesomium_Browser.h"
#include "Awesomium/WebCore.h"
#include "awesomium_capi.h"
#include <iostream>
#include <fstream>
#include <string.h>
#if defined(__WIN32__) || defined(_WIN32)
#include <windows.h>
#elif defined(__APPLE__)
#include <unistd.h>
#endif

using namespace Awesomium;

static Awesomium::WebCore *webCore = 0;

static void ensureWebCore() {
  if (webCore == 0) {
    Awesomium::WebCoreConfig config;
    config.setEnablePlugins(true);
    webCore = new Awesomium::WebCore(config);
  }
}

static void updateAll() {
  ensureWebCore();
  webCore->update();
}

class MyWebViewListener : public Awesomium::WebViewListener
{

public:

  Awesomium::WebView *webView;
  std::string currentURL;
  //  std::wstring currentFrameName;
  std::string currentTitle;
  bool resized;

  void update() {
    //webCore->update();
  }

  ~MyWebViewListener() {
    webView->destroy();
  }

  MyWebViewListener()
  {
     webView = 0;
     resized = true;
  }

  int width, height;

  Awesomium::CursorType cursorType;

  void setSize(int width, int height) {
    this->width = width;
    this->height = height;
    if (webView == 0) {
      webView = webCore->createWebView(width, height);
      webView->setListener(this);
      if (currentURL.length() > 0) {
          webView->loadURL(currentURL);
      }
    } else {
      resized = true;
    }
    std::cout << "plugins enabled " << webCore->arePluginsEnabled() << std::endl;
  }

  void onRequestFileChooser(Awesomium::WebView* caller,
                            bool selectMultipleFiles,
                            const std::wstring& title,
                            const std::wstring& defaultPath) {
  }

  void onGetScrollData(Awesomium::WebView* caller,
                       int contentWidth,
                       int contentHeight,
                       int preferredWidth,
                       int scrollX,
                       int scrollY) {
  }

  void onGetFindResults(Awesomium::WebView* caller,
                        int requestID,
                        int numMatches,
                        const Awesomium::Rect& selection,
                        int curMatch,
                        bool finalUpdate) {
  }

  void onUpdateIME(Awesomium::WebView* caller,
                   Awesomium::IMEState imeState,
                   const Awesomium::Rect& caretRect) {
  }

  void onShowContextMenu(Awesomium::WebView* caller,
                         int mouseX,
                         int mouseY,
                         Awesomium::MediaType type,
                         int mediaState,
                         const std::string& linkURL,
                         const std::string& srcURL,
                         const std::string& pageURL,
                         const std::string& frameURL,
                         const std::wstring& selectionText,
                         bool isEditable,
                         int editFlags) {
  }

  void onRequestLogin(Awesomium::WebView* caller,
                      int requestID,
                      const std::string& requestURL,
                      bool isProxy,
                      const std::wstring& hostAndPort,
                      const std::wstring& scheme,
                      const std::wstring& realm) {
  }

  void onChangeHistory(Awesomium::WebView* caller,
                       int backCount,
                       int forwardCount) {
  }

  void onShowJavascriptDialog(Awesomium::WebView* caller,
                              int requestID,
                              int dialogFlags,
                              const std::wstring& message,
                              const std::wstring& defaultPrompt,
                              const std::string& frameURL) {
  }

  void onFinishResize(Awesomium::WebView* caller,
                      int width,
                      int height) {
  }

  void onJavascriptConsoleMessage(Awesomium::WebView* caller,
                                  const std::wstring& message,
                                  int lineNumber,
                                  const std::wstring& source) {
  }

  void onRequestDownload(Awesomium::WebView* caller, const std::string& url) {
  }


  void onBeginNavigation(Awesomium::WebView* caller, const std::string& url, const std::wstring& frameName)
  {
    std::cout << "Navigating to URL: " << url << std::endl;
    //    currentFrameName = frameName;
    std::wcout << L"frame name = " << frameName << std::endl;
    std::cout << "url =  " << url << std::endl;
    if (frameName.length() == 0) {
      currentURL = url;
    }
  }
  
  void onBeginLoading(Awesomium::WebView* caller, const std::string& url, const std::wstring& frameName, int statusCode, const std::wstring& mimeType)
  {
    std::cout << "Beginning to load URL: " << url;
    std::cout << "\n\twith status code: " << statusCode;
    std::wcout << L"\n\twith mime-type: " << mimeType << std::endl;
    std::wcout << L"frame name = " << frameName << std::endl;
    std::cout << "url =  " << url << std::endl;
    if (frameName.length() == 0) {
      currentURL = url;
    }
  }
  
  void onFinishLoading(Awesomium::WebView* caller)
  {
  }
  
  void onCallback(Awesomium::WebView* caller, const std::wstring& objectName, const std::wstring& callbackName, const Awesomium::JSArguments& args)
  {
  }
  
  void onReceiveTitle(Awesomium::WebView* caller, const std::wstring& title, const std::wstring& frameName)
  {
    //currentTitle = title;
  }
  
  void onChangeTooltip(Awesomium::WebView* caller, const std::wstring& tooltip)
  {
  }
  
#if defined(_WIN32)
  void onChangeCursor(Awesomium::WebView* caller, const HCURSOR& cursor)
  {
  }
#endif
  
  void onChangeKeyboardFocus(Awesomium::WebView* caller, bool isFocused)
  {
  }

  void onChangeCursor(Awesomium::WebView* caller, Awesomium::CursorType cursor)  {
    if (this->webView == caller) {
      cursorType = cursor;
    }
  }

  
  void onChangeTargetURL(Awesomium::WebView* caller, const std::string& url)
  {
  }

  void onOpenExternalLink(Awesomium::WebView* caller, const std::string& url, const std::wstring& source)
  {
  }

  void onWebViewCrashed(Awesomium::WebView* caller)
  {
    std::cerr << "web view crashed" << std::endl;
  }
  
  void onPluginCrashed(Awesomium::WebView* caller, const std::wstring& pluginName)
  {
    std::wcerr << L"web view crashed " << pluginName << std::endl;
  }
  
  void onCreateWindow(Awesomium::WebView* caller, Awesomium::WebView* createdWindow, int width, int height)
  {
    std::cout << "create window " << width << " " << height << std::endl;
  }
  
  void onRequestMove(Awesomium::WebView* caller, int x, int y)
  {
  }
  
  void onGetPageContents(Awesomium::WebView* caller, const std::string& url, const std::wstring& contents)
  {
  }
  
  void onDOMReady(Awesomium::WebView* caller)
  {
  }

};

/*
 * Class:     org_f3_media_web_Browser
 * Method:    updateAll
 * Signature: ()V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_updateAll
(JNIEnv *env, jclass cls) {
  updateAll();
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    create
 * Signature: (II)I
 */
extern "C" jlong JNICALL Java_org_f3_media_web_awesomium_Browser_create
(JNIEnv *env, jclass clazz, jint w, jint h) {
  ensureWebCore();
  MyWebViewListener *p = new MyWebViewListener(); 
  p->setSize(w, h);
  return (jlong)p;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    create
 * Signature: (II)I
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_resize
(JNIEnv *env, jclass clazz, jlong handle, jint w, jint h) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->setSize(w, h);
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    destroy
 * Signature: (I)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_destroy
(JNIEnv *env, jclass clazz, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  delete p;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    getCursor
 * Signature: (I)I
 */
extern "C" jint JNICALL Java_org_f3_media_web_awesomium_Browser_getCursor
(JNIEnv *env, jclass clazz, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  return (jint)p->cursorType;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    render
 * Signature: (ILjava/nio/ByteBuffer;II[I)V
 */
extern "C" jboolean JNICALL Java_org_f3_media_web_awesomium_Browser_render
(JNIEnv *env, jclass clazz, jlong handle, jobject buffer, jint rowSpan, jint depth, jintArray rectArray) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  if (p == 0 || p->webView == 0) {
    return 0;
  }
  unsigned char *bufPtr = (unsigned char *)env->GetDirectBufferAddress(buffer);
  if (p->resized) {
    p->webView->resize(p->width, p->height);
  }
  Awesomium::WebCore::Get().update();
  if (p->webView->isDirty()) {
    p->resized = false;
    Awesomium::Rect rect = p->webView->getDirtyBounds();
    const Awesomium::RenderBuffer *buffer = p->webView->render();
    if (buffer != 0) {
        if (buffer->width == p->width && buffer->height == p->height) {
          unsigned char *outPtr = bufPtr;
          unsigned char *buf = buffer->buffer;
          jint arr[4];
          if (0) {
            //fprintf(stderr, "rect %d %d %d %d\n", rect.x, rect.y, rect.width, rect.height);
            for (int i = rect.height-1; i >= 0; i--) {
              int row = rect.y + i;
              for (int j = 0; j < rect.width; j++) {
                int col = rect.x + j;
                unsigned char *p = buf + row * rowSpan + (col * depth);
                unsigned char b = *p++;
                unsigned char g = *p++;
                unsigned char r = *p++;
                unsigned char a = *p++;
                *outPtr++ = r;
                *outPtr++ = g;
                *outPtr++ = b;
                *outPtr++ = a;
              }
            }
            arr[0] = rect.x;
            arr[1] = rect.y;
            arr[2] = rect.width;
            arr[3] = rect.height;
          } else {
            for (int i = buffer->height-1; i >= 0; i--) {
              int row = i;
              for (int j = 0; j < buffer->width; j++) {
                int col = j;
                unsigned char *p = buf + row * rowSpan + (col * depth);
                unsigned char b = *p++;
                unsigned char g = *p++;
                unsigned char r = *p++;
                unsigned char a = *p++;
                *outPtr++ = r;
                *outPtr++ = g;
                *outPtr++ = b;
                *outPtr++ = a;
              }
            }
            arr[0] = 0;
            arr[1] = 0;
            arr[2] = buffer->width;
            arr[3] = buffer->height;
          }
          env->SetIntArrayRegion(rectArray, 0, 4, (const jint*)arr);
          return 1;
        } else {
          std::cerr << "buffer mismatch " << buffer->width << " " << buffer->height << std::endl;
        }
    } else {
      std::cerr << "buffer from render is null" << std::endl;
    }
  }
  return 0;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectMouseDown
 * Signature: (II)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectMouseDown
(JNIEnv *env, jclass, jlong handle, jint button) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  Awesomium::MouseButton mb = button == 1 ? Awesomium::LEFT_MOUSE_BTN :
    button == 2 ? Awesomium::MIDDLE_MOUSE_BTN : Awesomium::RIGHT_MOUSE_BTN;
  p->webView->injectMouseDown(mb);
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectMouseUp
 * Signature: (II)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectMouseUp
(JNIEnv *, jclass, jlong handle, jint button) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  Awesomium::MouseButton mb = button == 1 ? Awesomium::LEFT_MOUSE_BTN :
    button == 2 ? Awesomium::MIDDLE_MOUSE_BTN : Awesomium::RIGHT_MOUSE_BTN;
  p->webView->injectMouseUp(mb);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectMouseMove
 * Signature: (III)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectMouseMove
(JNIEnv *, jclass, jlong handle, jint x, jint y) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->injectMouseMove(x, y);
}

extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectMouseWheel
(JNIEnv *, jclass, jlong handle, jint x, jint y) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->injectMouseWheel(x, y);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectKeyDown
 * Signature: (III)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectKeyDown
(JNIEnv *, jclass, jlong handle, jint mods, jint code) {
  Awesomium::WebKeyboardEvent e;
  e.type = e.TYPE_KEY_DOWN;
  e.modifiers = mods;
  e.virtualKeyCode = code;
  e.nativeKeyCode = 0;
  e.text[0] = 0;
  e.unmodifiedText[0] = 0;
  e.keyIdentifier[0] = 0;
  e.isSystemKey = 0;
  //Awesomium::getKeyIdentifierFromVirtualKeyCode(e.virtualKeyCode, (char**)&e.keyIdentifier);
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->injectKeyboardEvent(e);
  
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectKeyUp
 * Signature: (III)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectKeyUp
(JNIEnv *, jclass, jlong handle, jint mods, jint code) {
  Awesomium::WebKeyboardEvent e;
  e.type = e.TYPE_KEY_UP;
  e.modifiers = mods;
  e.virtualKeyCode = code;
  e.nativeKeyCode = 0;
  e.text[0] = 0;
  e.unmodifiedText[0] = 0;
  e.keyIdentifier[0] = 0;
  e.isSystemKey = 0;
  //Awesomium::getKeyIdentifierFromVirtualKeyCode(e.virtualKeyCode, (char**)&e.keyIdentifier);
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->injectKeyboardEvent(e);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    injectKeyInput
 * Signature: (IIILjava/lang/String;)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_injectKeyInput
(JNIEnv *env, jclass, jlong handle, jint mods, jint code, jchar ch) {
  Awesomium::WebKeyboardEvent e;
  e.type = e.TYPE_CHAR;
  e.modifiers = mods;
  e.virtualKeyCode = code;
  e.nativeKeyCode = 0;
  e.text[0] = ch;
  e.text[1] = 0;
  e.unmodifiedText[0] = ch;
  e.unmodifiedText[1] = 0;
  e.keyIdentifier[0] = 0;
  e.isSystemKey = 0;
  //  Awesomium::getKeyIdentifierFromVirtualKeyCode(e.virtualKeyCode, (char**)e.keyIdentifier);
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->injectKeyboardEvent(e);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    setURL
 * Signature: (ILjava/lang/String;)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_setURL
(JNIEnv *env, jclass clazz, jlong handle, jstring url) {
  jboolean iscopy;
  const char *chs = env->GetStringUTFChars(url, &iscopy);
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->loadURL(chs);
  env->ReleaseStringChars(url, (const jchar*)chs);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    setURL
 * Signature: (ILjava/lang/String;)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_setContent
(JNIEnv *env, jclass clazz, jlong handle, jstring url) {
  jboolean iscopy;
  const char *chs = env->GetStringUTFChars(url, &iscopy);
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->loadHTML(chs);
  env->ReleaseStringChars(url, (const jchar*)chs);
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    getURL
 * Signature: (I)Ljava/lang/String;
 */
extern "C" jstring JNICALL Java_org_f3_media_web_awesomium_Browser_getURL
(JNIEnv *env, jclass, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  return env->NewStringUTF(p->currentURL.c_str());
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    focus
 * Signature: (I)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_focus
(JNIEnv *, jclass, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->focus();
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    unfocus
 * Signature: (I)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_unfocus
(JNIEnv *, jclass, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->unfocus();
}



/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    zoomIn
 * Signature: (I)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_setZoom
                                                                 (JNIEnv *, jclass, jlong handle, jint percent) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->setZoom(percent);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    resetZoom
 * Signature: (I)V
 */
extern "C" void JNICALL Java_org_f3_media_web_awesomium_Browser_resetZoom
(JNIEnv *, jclass, jlong handle) {
  MyWebViewListener *p = (MyWebViewListener*)handle;
  p->webView->resetZoom();
}

static jclass Integer = 0;
static jclass Double = 0;
static jclass Boolean = 0;
static jclass JSArrayClazz = 0;
static jclass JSObjectClazz = 0;
static jmethodID NewInteger = 0;
static jmethodID NewDouble = 0;
static jmethodID NewBoolean = 0;
static jmethodID NewJSObject = 0;
static jmethodID NewJSArray = 0;

void initMethodIds(JNIEnv *env) {
  jclass cls = env->FindClass("java/lang/Integer");
  jmethodID methodId = env->GetMethodID(cls, "<init>", "(I)V");
  Integer = cls;
  NewInteger = methodId;
  cls = env->FindClass("java/lang/Double");
  methodId = env->GetMethodID(cls, "<init>", "(D)V");
  NewDouble = methodId;
  Double = cls;
  cls = env->FindClass("java/lang/Boolean");
  methodId = env->GetMethodID(cls, "<init>", "(Z)V");
  NewBoolean =  methodId;
  Boolean = cls;
  cls = env->FindClass("org/f3/media/web/awesomium/JSArray");
  methodId = env->GetMethodID(cls, "<init>", "(J)V");
  NewJSArray =  methodId;
  JSArrayClazz = cls;
  cls = env->FindClass("org/f3/media/web/awesomium/JSObject");
  methodId = env->GetMethodID(cls, "<init>", "(J)V");
  NewJSObject =  methodId;
  JSObjectClazz = cls;
}

static jobject newInteger(JNIEnv *env, jint value) {
  return env->NewObject(Integer, NewInteger, value);
}

static jobject newDouble(JNIEnv *env, jdouble value) {
  return env->NewObject(Double, NewDouble, value);
}

static jobject newBoolean(JNIEnv *env, jboolean value) {
  return env->NewObject(Boolean, NewBoolean, value);
}

#if 1

static jobject newJSArray(JNIEnv *env, const awe_jsarray *value) {
  return env->NewObject(JSArrayClazz, NewJSArray, (long)value);
}

static jobject newJSObject(JNIEnv *env, const awe_jsobject *value) {
  return env->NewObject(JSObjectClazz, NewJSObject, (long)value);
}

static jobject newString(JNIEnv *env, awe_string *str) {
  int size = awe_string_to_utf8(str, 0, 0);
  char *chs = new char[size+1];
  chs[size] = 0;
  int count = awe_string_to_utf8(str, chs, size);
  jobject result = env->NewStringUTF(chs);
  delete chs;
  return result;
}  

static awe_string *toWebString(JNIEnv *env, jstring value) {
  jboolean iscopy;
  const char *chs = env->GetStringUTFChars(value, &iscopy);
  awe_string *str = awe_string_create_from_utf8(chs, strlen(chs));
  env->ReleaseStringChars(value, (const jchar*)chs);
  return str;
}

static jobject fromJSValue(JNIEnv *env, const awe_jsvalue *v) {
  switch (awe_jsvalue_get_type(v)) {
  case JSVALUE_TYPE_BOOLEAN:
    return newBoolean(env, awe_jsvalue_to_boolean(v));
  case JSVALUE_TYPE_INTEGER:
    return newInteger(env, awe_jsvalue_to_integer(v));
  case JSVALUE_TYPE_DOUBLE:
    return newDouble(env, awe_jsvalue_to_double(v));
  case JSVALUE_TYPE_ARRAY:
    return newJSArray(env, awe_jsvalue_get_array(v));
  case JSVALUE_TYPE_STRING:
    return newString(env, awe_jsvalue_to_string(v));
  case JSVALUE_TYPE_OBJECT:
    return newJSObject(env, awe_jsvalue_get_object(v));
  } //else if (v.isNull()) {
  return 0;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    execute_js
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jobject JNICALL Java_org_f3_media_web_awesomium_Browser_execute_1js
  (JNIEnv *env, jclass, jlong h, jstring script)
{
  MyWebViewListener *l = (MyWebViewListener*)h;
  fprintf(stderr, "handle=%p\n", h);
  if (l->webView == 0) {
    return 0;
  }
  fprintf(stderr, "webview=%p\n", l->webView);
  fprintf(stderr, "script=%p\n", script);
  awe_string *str = toWebString(env, script);
  fprintf(stderr, "str=%p\n", str);
  awe_jsvalue *value = awe_webview_execute_javascript_with_result((awe_webview*)l->webView, str, awe_string_empty(), 5000);
  fprintf(stderr, "value=%p\n", value);
  jobject result = fromJSValue(env, value);
  awe_string_destroy(str);
  awe_jsvalue_destroy(value);
  return result;
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    create_js_array
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_f3_media_web_awesomium_Browser_create_1js_1array
  (JNIEnv *, jclass)
{
  return (long)awe_jsarray_create(0, 0);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    create_js_object
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_f3_media_web_awesomium_Browser_create_1js_1object
(JNIEnv *, jclass) {
  return (long) awe_jsobject_create();
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    destroy_js_array
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_destroy_1js_1array
  (JNIEnv *, jclass, jlong h)
{
  awe_jsarray_destroy((awe_jsarray*)h);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    destroy_js_object
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_destroy_1js_1object
(JNIEnv *, jclass, jlong h) {
  awe_jsobject_destroy((awe_jsobject*)h);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    invoke
 * Signature: (Ljava/lang/String;J)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_f3_media_web_awesomium_Browser_invoke
(JNIEnv *env, jclass, jlong h, jstring method, jlong a)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_jsarray *arr = (awe_jsarray*)a;
  awe_string *m = toWebString(env, method);
  awe_jsvalue *value = 0;//obj->Invoke(m, *arr);
  jobject result = fromJSValue(env, value);
  awe_jsvalue_destroy(value);
  awe_string_destroy(m);
  return result;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    getPropertyNames
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_f3_media_web_awesomium_Browser_getPropertyNames
  (JNIEnv *env, jclass cls, jlong h)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  return (long)awe_jsobject_get_keys(obj);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    getMethodNames
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_f3_media_web_awesomium_Browser_getMethodNames
  (JNIEnv *env, jclass cls, jlong h)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  return (long)0;//(new awe_jsarray(h.getMethodNames()));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    has
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_f3_media_web_awesomium_Browser_has
  (JNIEnv *env, jclass, jlong h, jstring index)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  bool result = awe_jsobject_has_property(obj, str);
  awe_string_destroy(str);
  return result;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    hasMethod
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_f3_media_web_awesomium_Browser_hasMethod
  (JNIEnv *env, jclass cls, jlong h, jstring index)
{
  // awe_jsobject *obj = (awe_jsobject*)obj;
  //awe_string *str = toWebString(env, index);
  return false;
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_null
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1null
(JNIEnv *env, jclass cls, jlong h, jstring index) 
{
  awe_jsobject *obj = (awe_jsobject*)obj;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_null_value());
  awe_string_destroy(str);
}


/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_boolean
 * Signature: (JLjava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1boolean
(JNIEnv *env, jclass cls, jlong h, jstring index, jboolean value) 
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_bool_value(value));
  awe_string_destroy(str);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_int
 * Signature: (JLjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1int
(JNIEnv *env, jclass cls, jlong h, jstring index, jint value) 
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_integer_value(value));
  awe_string_destroy(str);

}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_double
 * Signature: (JLjava/lang/String;D)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1double
(JNIEnv *env, jclass cls, jlong h, jstring index, jdouble value) 
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_double_value(value));
  awe_string_destroy(str);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_string
 * Signature: (JLjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1string
  (JNIEnv *env, jclass cls, jlong h, jstring index, jstring value)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_string_value(toWebString(env, value)));
  awe_string_destroy(str);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_object
 * Signature: (JLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1object
  (JNIEnv *env, jclass cls, jlong h, jstring index, jlong value)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_object_value((awe_jsobject*)value));
  awe_string_destroy(str);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_array
 * Signature: (JLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1array
  (JNIEnv *env, jclass cls, jlong h, jstring index, jlong value)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, index);
  awe_jsobject_set_property(obj, str, awe_jsvalue_create_array_value((awe_jsarray*)value));
  awe_string_destroy(str);
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    get
 * Signature: (JLjava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_f3_media_web_awesomium_Browser_get
  (JNIEnv *env, jclass cls, jlong h, jstring value)
{
  awe_jsobject *obj = (awe_jsobject*)h;
  awe_string *str = toWebString(env, value);
  jobject result = fromJSValue(env, awe_jsobject_get_property(obj, str));
  awe_string_destroy(str);
  return result;
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    get_element
 * Signature: (JI)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_f3_media_web_awesomium_Browser_get_1element
  (JNIEnv *env, jclass cls, jlong h, jint index)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  return fromJSValue(env, awe_jsarray_get_element(arr, index));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_null_element
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1null_1element
(JNIEnv *env, jclass cls, jlong h,  jint index)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  //awe_js_array_put_element(index, awe_jsvalue_create_null_value());
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_boolean_element
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1boolean_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jboolean v)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  //awe_js_array_put_element(index, awe_jsvalue_create_bool_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_int_element
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1int_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jint v)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  //awe_js_array_put_element(index, awe_jsvalue_create_int_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_double_element
 * Signature: (JID)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1double_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jdouble v)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  //awe_js_array_put_element(index, awe_jsvalue_create_double_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_string_element
 * Signature: (JILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1string_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jstring value)
{
  awe_string *v = toWebString(env, value);
  awe_jsarray *arr = (awe_jsarray*)h;
  //awe_js_array_put_element(index, awe_jsvalue_create_string_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_object_element
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1object_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jlong value)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  awe_jsobject *v = (awe_jsobject*)value;
  //awe_js_array_put_element(index, awe_jsvalue_create_object_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    put_array_element
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_org_f3_media_web_awesomium_Browser_put_1array_1element
  (JNIEnv *env, jclass cls, jlong h, jint index, jlong value)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  awe_jsarray *v = (awe_jsarray*)value;
  //awe_js_array_put_element(index, awe_jsvalue_create_array_value(v));
}

/*
 * Class:     org_f3_media_web_awesomium_Browser
 * Method:    getSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_f3_media_web_awesomium_Browser_getSize
  (JNIEnv *env, jclass cls, jlong h)
{
  awe_jsarray *arr = (awe_jsarray*)h;
  return awe_jsarray_get_size(arr);
}


#endif

#endif
