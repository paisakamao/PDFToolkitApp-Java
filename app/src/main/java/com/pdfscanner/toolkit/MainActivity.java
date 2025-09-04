@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    remoteConfig = FirebaseRemoteConfig.getInstance();

    mAdView = findViewById(R.id.adView);

    String bannerAdId = remoteConfig.getString("android_banner_ad_id");
    if (bannerAdId == null || bannerAdId.isEmpty()) {
        // ✅ Fallback to Google test banner ID
        bannerAdId = "ca-app-pub-3940256099942544/6300978111";
    }

    mAdView.setAdUnitId(bannerAdId);
    AdRequest adRequest = new AdRequest.Builder().build();
    mAdView.loadAd(adRequest);

    // ✅ Preload interstitial so it's ready before WebView JS calls it
    AdManager.getInstance().loadInterstitialAd(this);

    webView = findViewById(R.id.webView);
    WebView.setWebContentsDebuggingEnabled(true);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false);

    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient() {
        ...
    });

    webView.addJavascriptInterface(new JSBridge(this), "Android");

    Intent intent = getIntent();
    String htmlFileToLoad = intent.getStringExtra(EXTRA_HTML_FILE);
    if (htmlFileToLoad == null || htmlFileToLoad.isEmpty()) {
        htmlFileToLoad = "index.html";
    }
    webView.loadUrl("file:///android_asset/" + htmlFileToLoad);
}