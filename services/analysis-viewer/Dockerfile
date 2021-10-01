FROM debian:stretch-slim
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q wget nginx

# replace nginx config
COPY services/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY services/ui/default /etc/nginx/sites-available/default

# nginx config
COPY services/ui/spreadviz.org /etc/nginx/sites-available/spreadviz.org

# setup error page
COPY services/ui/X0X.html /usr/share/nginx/html/X0X.html

# setup site
RUN ln -s -f /etc/nginx/sites-available/spreadviz.org /etc/nginx/sites-enabled/spreadviz.org

# copy static content
COPY services/analysis-viewer/index.html /spread/index.html
COPY services/analysis-viewer/js/main.js /spread/js/main.js
COPY services/analysis-viewer/css/ /spread/css/
COPY services/analysis-viewer/fonts/ /spread/fonts/

# favicon and meta content
COPY services/analysis-viewer/android-chrome-192x192.png spread/android-chrome-192x192.png
COPY services/analysis-viewer/android-chrome-512x512.png spread/android-chrome-512x512.png
COPY services/analysis-viewer/apple-touch-icon.png spread/apple-touch-icon.png
COPY services/analysis-viewer/browserconfig.xml spread/browserconfig.xml
COPY services/analysis-viewer/favicon-16x16.png spread/favicon-16x16.png
COPY services/analysis-viewer/favicon-32x32.png spread/favicon-32x32.png
COPY services/analysis-viewer/favicon.ico spread/favicon.ico
COPY services/analysis-viewer/mstile-144x144.png spread/mstile-144x144.png
COPY services/analysis-viewer/mstile-150x150.png spread/mstile-150x150.png
COPY services/analysis-viewer/mstile-310x150.png spread/mstile-310x150.png
COPY services/analysis-viewer/mstile-310x310.png spread/mstile-310x310.png
COPY services/analysis-viewer/mstile-70x70.png spread/mstile-70x70.png
COPY services/analysis-viewer/safari-pinned-tab.svg spread/safari-pinned-tab.svg
COPY services/analysis-viewer/site.webmanifest spread/site.webmanifest

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
