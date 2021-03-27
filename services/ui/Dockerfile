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
COPY services/ui/spread.com /etc/nginx/sites-available/spread.com

# setup error page
COPY services/ui/X0X.html /usr/share/nginx/html/X0X.html

# setup site
RUN ln -s -f /etc/nginx/sites-available/spread.com /etc/nginx/sites-enabled/spread.com

# copy static content
COPY services/ui/index.html /spread/index.html
COPY services/ui/js/main.js /spread/js/main.js
COPY services/ui/css/main.css /spread/css/main.css

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
