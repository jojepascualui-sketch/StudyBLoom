// StudyBloom service worker.
// Caches the app shell so the app opens offline. Task data is cached by the
// Java app itself (OfflineTaskRepository), so Supabase calls pass straight through.
// The version below is replaced at build time, so each deploy gets a fresh cache.
var VERSION = 'studybloom-${build.version}';
var SHELL = [
  '/',
  '/index.html',
  '/styles.css',
  '/config.js',
  '/js/studybloom.js',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/favicon.svg'
];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(VERSION).then(function (cache) { return cache.addAll(SHELL); })
      .then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== VERSION; })
        .map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('fetch', function (event) {
  var request = event.request;
  if (request.method !== 'GET') return;
  var url = new URL(request.url);

  // Page loads: network first, fall back to the cached shell when offline.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(function () { return caches.match('/index.html'); })
    );
    return;
  }

  // Google Fonts: cache first (they never change for a given URL).
  if (url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com') {
    event.respondWith(
      caches.match(request).then(function (hit) {
        return hit || fetch(request).then(function (res) {
          var copy = res.clone();
          caches.open(VERSION).then(function (c) { c.put(request, copy); });
          return res;
        });
      })
    );
    return;
  }

  // Our own static files: serve from cache, refresh in the background.
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.match(request).then(function (hit) {
        var network = fetch(request).then(function (res) {
          if (res.ok) {
            var copy = res.clone();
            caches.open(VERSION).then(function (c) { c.put(request, copy); });
          }
          return res;
        }).catch(function () { return hit; });
        return hit || network;
      })
    );
  }
  // Everything else (Supabase, Stripe) goes to the network untouched.
});

// Tapping a reminder opens (or focuses) the app.
self.addEventListener('notificationclick', function (event) {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (list) {
      for (var i = 0; i < list.length; i++) {
        if ('focus' in list[i]) return list[i].focus();
      }
      return self.clients.openWindow('/');
    })
  );
});
