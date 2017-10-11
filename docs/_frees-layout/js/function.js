// anime({
//   targets: '.hidden-on-start',
//   opacity: 1,
//   duration: 200,
//   easing: 'linear',
//   elasticity: 0,
// });

//
// anime({
//   targets: '.leftSide',
//   translateX: (-50),
//   opacity: 0,
//   duration: 500,
//   // loop: 2,
//   easing: 'linear',
//   elasticity: 0,
//   direction: 'reverse'
// });

var basicTimeline = anime.timeline({
  direction: 'reverse',
  // loop: true
});

var gdur = 1500

basicTimeline
  .add({
    targets: '#freestyle-tag',
    translateY: (-20),
    opacity: 0,
    duration: gdur,
    easing: 'easeInCubic',
    elasticity: 500,
    offset: '-=300',
  })
  .add({
    targets: '.piece-top',
    translateX: (-40),
    translateY: (40, 20),
    opacity: 0,
    duration: 1000,
    // loop: 2,
    easing: 'easeInCubic',
    elasticity: 500,
    // direction: 'reverse',
    offset: 300,
  })

  .add({
    targets: '.piece-center',
    opacity: 1,
    duration: 400,
    // loop: 2,
    easing: 'easeInCubic',
    elasticity: 500,
    // direction: 'reverse',
    offset: 150,
  })


  .add({
    targets: '.piece-bottom',
    translateX: (40),
    translateY: (-40, -20),
    opacity: 0,
    duration: 1200,
    // loop: 2,
    easing: 'easeInCubic',
    elasticity: 500,
    // direction: 'reverse',
    offset: 300
  });
