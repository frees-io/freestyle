// We should wait until all assets on page gets loaded to trigger animations,
// by this way avoiding any FOUC problems. Since jQuery is on the page we
// take advantage of it.
$(window).on("load", function() {
    // General injection duration
    var injectionDuration = 300;

    // Element references to be animated
    var featureAlgebrasClass = '.feature-algebras';
    var featureAlgebrasEl = document.querySelector(featureAlgebrasClass);
    var featureModulesClass = '.feature-modules';
    var featureModulesEl = document.querySelector(featureModulesClass);
    var featureHandlersClass = '.feature-handlers';
    var featureHandlersEl = document.querySelector(featureHandlersClass);

    var librariesLinesClass = '.libraries-lines';
    var libraryEffectsClass = '.library-effects';
    var libraryEffectsEl = document.querySelector(libraryEffectsClass);
    var libraryCassandraClass = '.library-cassandra';
    var libraryCassandraEl = document.querySelector(libraryCassandraClass);
    var libraryRpcClass = '.library-rpc';
    var libraryRpcEl = document.querySelector(libraryRpcClass);
    var libraryKafkaClass = '.library-kafka';
    var libraryKafkaEl = document.querySelector(libraryKafkaClass);

    function setOpacity(elements) {
      anime({
        targets: elements,
        opacity: 1,
        duration: injectionDuration,
        easing: 'easeInCubic',
      })
    }

    function bulgePath(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        strokeDasharray: '1',
        easing: 'easeInOutCubic',
        duration: 600,
        direction: 'alternate',
        loop: true,
      });
    }

    function bulgePathBack(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        strokeDasharray: '3',
        easing: 'easeInOutCubic',
        duration: 300,
      });
    }

    function emitPath(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        strokeDashoffset: 30,
        easing: 'linear',
        elasticity: 500,
        duration: 1000,
        loop: true,
      });
    }

    function rotate(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        rotateZ: [0, 360],
        easing: 'easeInOutCubic',
        elasticity: 600,
        duration: 1200,
        loop: true,
      });
    }

    function rotateBack(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        rotateZ: 360,
        easing: 'linear',
        elasticity: 600,
        duration: 600,
      });
    }

    function scale(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        scale: 1.2,
        easing: 'easeInOutCubic',
        duration: 550,
      });
    }

    function scaleBack(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        scale: 1,
        easing: 'linear',
        duration: 400,
      });
    }

    function undrawPath(elements) {
      anime.remove(elements);
      anime({
        targets: elements,
        strokeDashoffset: [0, anime.setDashoffset],
        easing: 'easeInOutCubic',
        duration: 1500,
        direction: 'alternate',
        loop: true,
        delay: function(el, i) { return i * 150 },
      });
    }

    
    function drawPath(elements, duration) {
      anime.remove(elements);
      anime({
        targets: elements,
        strokeDashoffset: 0,
        easing: 'linear',
        duration: duration,
      });
    }

    // This timeline is the Freestyle logo and tag animation
    var logoTimeline = anime.timeline({
      direction: 'reverse',
    })
      .add({
        targets: '#freestyle-tag',
        translateY: (-20),
        opacity: 0,
        duration: 1500,
        easing: 'easeInCubic',
        elasticity: 500,
      })
      .add({
        targets: '.piece-top',
        translateX: (-40),
        translateY: (40, 20),
        opacity: 0,
        duration: 1000,
        easing: 'easeInCubic',
        offset: 300,
      })
      .add({
        targets: '.piece-bottom',
        translateX: (40),
        translateY: (-40, -20),
        opacity: 0,
        duration: 1200,
        easing: 'easeInCubic',
        offset: 300,
      });

    // This timeline draws and shows the libraries section
    var lineDrawing = anime.timeline({
      autoplay: false,
    })
      .add({
        targets: librariesLinesClass + ' .point-0',
        scale: [0, 1],
        duration: 2000,
        elasticity: 400,
        offset: 400,
      })
      .add({
        targets: librariesLinesClass + ' #line-vertical-0',
        strokeDashoffset: [anime.setDashoffset, 0],
        easing: 'easeInOutSine',
        duration: 1000,
        offset: 600,
      })
      .add({
        targets: [librariesLinesClass + ' #line-horizontal-1', librariesLinesClass + ' #line-horizontal-2'],
        strokeDashoffset: [anime.setDashoffset, 0],
        easing: 'easeInOutSine',
        duration: 1500,
        offset: 1500
      })
      .add({
        targets: [librariesLinesClass + ' .point-1', librariesLinesClass + ' .point-4'],
        scale: [0, 1],
        duration: 2000,
        elasticity: 400,
        offset: 2750
      })
      .add({
        targets: [librariesLinesClass + ' #line-vertical-1', librariesLinesClass + ' #line-vertical-4'],
        strokeDashoffset: [anime.setDashoffset, 0],
        strokeDasharray: 3,
        easing: 'easeInOutSine',
        duration: 1500,
        offset: 2700
      })
      .add({
        targets: [librariesLinesClass + ' .point-2', librariesLinesClass + ' .point-3'],
        scale: [0, 1],
        duration: 2000,
        elasticity: 400,
        offset: 2000
      })
      .add({
        targets: [librariesLinesClass + ' #line-vertical-2', librariesLinesClass + ' #line-vertical-3'],
        strokeDashoffset: [anime.setDashoffset, 0],
        strokeDasharray: 3,
        easing: 'easeInOutSine',
        duration: 1500,
        offset: 2000
      })
      .add({
        targets: [librariesLinesClass + ' #arrow-1', librariesLinesClass + ' #arrow-4'],
        translateY: ['-56px', 0],
        opacity: [
          { value: 1, duration: 400}
        ],
        easing: 'easeInOutSine',
        duration: 900,
        offset: 2800,
      })
      .add({
        targets: [librariesLinesClass + ' #arrow-2', librariesLinesClass + ' #arrow-3'],
        translateY: ['-56px', 0],
        opacity: [
          { value: 1, duration: 400}
        ],
        easing: 'easeInOutSine',
        duration: 900,
        offset: 2000,
      })
      .add({
        targets: [libraryCassandraClass, libraryRpcClass],
        // translateY: ['-10%', 0],
        opacity: [0, 1],
        easing: 'easeInOutSine',
        duration: 600,
        offset: 2900,
      })
      .add({
        targets: [libraryEffectsClass, libraryKafkaClass],
        // translateY: ['-10%', 0],
        opacity: [0, 1],
        easing: 'easeInOutSine',
        duration: 600,
        offset: 3700,
      });

    // Feature buttons functions
    function algebraButtonEnter() {
      bulgePath(featureAlgebrasClass + ' path');
      scale(featureAlgebrasClass + ' #hexagon-small');
    };
    function algebraButtonLeave() {
      bulgePathBack(featureAlgebrasClass + ' path');
      scaleBack(featureAlgebrasClass + ' #hexagon-small');
    };
    function modulesButtonEnter() {
      emitPath(featureModulesClass + ' path');
    };
    function modulesButtonLeave() {
      drawPath(featureModulesClass + ' path', 1);
    };
    function handlersButtonEnter() {
      rotate([featureHandlersClass + ' path', featureHandlersClass + ' polygon#arrow']);
    };
    function handlersButtonLeave() {
      rotateBack([featureHandlersClass + ' path', featureHandlersClass + ' polygon#arrow']);
    };

    // Library buttons functions
    function effectsButtonEnter() {
      scale(libraryEffectsClass + ' .effects-line');
    };
    function effectsButtonLeave() {
      scaleBack(libraryEffectsClass + ' .effects-line');
    };
    function cassandraButtonEnter() {
      scale(libraryCassandraClass + ' .cassandra-eye');
    };
    function cassandraButtonLeave() {
      scaleBack(libraryCassandraClass + ' .cassandra-eye');
    };
    function rpcButtonEnter() {
      emitPath(libraryRpcClass + ' path');
    };
    function rpcButtonLeave() {
      drawPath(libraryRpcClass + ' path', 1);
    };
    function kafkaButtonEnter() {
      undrawPath(libraryKafkaClass + ' path');
    };
    function kafkaButtonLeave() {
      drawPath(libraryKafkaClass + ' path', 400);
    };

    // Feature buttons functions attachments
    featureAlgebrasEl.addEventListener('mouseenter', algebraButtonEnter, false);
    featureAlgebrasEl.addEventListener('mouseleave', algebraButtonLeave, false);
    featureModulesEl.addEventListener('mouseenter', modulesButtonEnter, false);
    featureModulesEl.addEventListener('mouseleave', modulesButtonLeave, false);
    featureHandlersEl.addEventListener('mouseenter', handlersButtonEnter, false);
    featureHandlersEl.addEventListener('mouseleave', handlersButtonLeave, false);

    // Library buttons functions attachments
    libraryEffectsEl.addEventListener('mouseenter', effectsButtonEnter, false);
    libraryEffectsEl.addEventListener('mouseleave', effectsButtonLeave, false);
    libraryCassandraEl.addEventListener('mouseenter', cassandraButtonEnter, false);
    libraryCassandraEl.addEventListener('mouseleave', cassandraButtonLeave, false);
    libraryRpcEl.addEventListener('mouseenter', rpcButtonEnter, false);
    libraryRpcEl.addEventListener('mouseleave', rpcButtonLeave, false);
    libraryKafkaEl.addEventListener('mouseenter', kafkaButtonEnter, false);
    libraryKafkaEl.addEventListener('mouseleave', kafkaButtonLeave, false);


    // This function call changes these elements general opacity as it is set to 
    // 0 on start to avoid logo/element flashing when loaded and then animated
    setOpacity('.indirect-injection, .gitter-open-chat-button');

    // Logic related to the line drawing effect triggering when scrolling into libraries section
    var hT = $('#freestyle-lines-libraries').offset().top,
        hH = $('#freestyle-lines-libraries').outerHeight();

    var scrollHandler = function() {
      if (!lineDrawing.began) {
        var wH = $(window).height(),
            wW = $(window).width(),
            wS = $(this).scrollTop();

        if ((wS >= (hT + hH - wH) && (hT >= wS) && (wS + wH >= hT + hH))) {
          // When in mobile there's no lines, only libraries buttons
          if (wW <= 992) lineDrawing.seek(2899);
          lineDrawing.play();
        }
      }
      else {
        $(window).off('scroll', scrollHandler);
      }
    }

    $(window).scroll(scrollHandler);
    $(window).scroll();

});
