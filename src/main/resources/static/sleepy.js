window
    .addEventListener('load', function (e) {

        var css = [
            'bower_components/owlcarousel/assets/css/bootstrapTheme.css',
            'bower_components/owlcarousel/assets/css/custom.css',
            'bower_components/owlcarousel/owl-carousel/owl.carousel.css',
            'bower_components/owlcarousel/owl-carousel/owl.theme.css'
        ];

        css
            .forEach(function (x, n, a) {
                var el = document.createElement('link');
                el.setAttribute('href', x);
                el.setAttribute('rel', 'stylesheet');
                document.getElementsByTagName('head').item(0).appendChild(el);
            });

        var js = [
            'bower_components/owlcarousel/assets/js/jquery-1.9.1.min.js',
            'bower_components/owlcarousel/owl-carousel/owl.carousel.js',
            'bower_components/owlcarousel/assets/js/bootstrap-collapse.js',
            'bower_components/owlcarousel/assets/js/bootstrap-transition.js',
            'bower_components/owlcarousel/assets/js/bootstrap-tab.js',
            'slideshow.js'
        ];

        function loadScript(indx) {

            var src = js[indx];
            var next = indx + 1;

            console.log('loading src ' + src);

            var el = document.createElement('script');
            el.setAttribute('src', src);

            if (js.length > next) {
                el.addEventListener('load', function (e) {
                    console.log('  queued ' + js[next] + ' up..');
                    loadScript(next);
                });
            }

            document.getElementsByTagName('body').item(0).appendChild(el);
        }


        fetch('/images.json')
            .then(function (response) {

                if (response.status == 200) {
                    response.json().then(function (data) {

                        var body = document.getElementsByTagName('body').item(0);

                        var container = document.createElement('div');
                        container.setAttribute('id', 'owl-demo');
                        container.setAttribute('class', 'owl-carousel');

                        data.forEach(function (img, n, imgArr) {

                            var imgDiv = document.createElement('div');
                            imgDiv.setAttribute('class', 'item');

                            var imgNode = document.createElement('img');
                            imgNode.setAttribute('src', img['uri']);

                            imgDiv.appendChild(imgNode);

                            container.appendChild(imgDiv);
                        });


                        body.appendChild(container);

                    });
                }
            })
            .catch(function (err) {
                console.log('error when trying to read images.json! ' + err.toString());
            });

        loadScript(0);

    });