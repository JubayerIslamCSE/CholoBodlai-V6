(() => {
  const route = document.querySelector('#routeLine');
  const painted = document.querySelector('#routeProgress');
  const truck = document.querySelector('#truckSprite');
  const startButton = document.querySelector('#playDemo');
  const resetButton = document.querySelector('#resetDemo');
  if (!route || !painted || !truck || !startButton || !resetButton) return;
  const total = route.getTotalLength();
  let progress = 0, running = false, prior = 0, frame = null;
  const pct = document.querySelector('#percentDone');
  const bar = document.querySelector('#progressFill');
  const stage = document.querySelector('#currentStage');
  const status = document.querySelector('#playStatus');
  const speed = document.querySelector('#demoSpeed');
  painted.style.strokeDasharray = String(total);
  const draw = () => {
    const point = route.getPointAtLength(total * progress);
    truck.setAttribute('transform', `translate(${point.x.toFixed(2)} ${point.y.toFixed(2)})`);
    painted.style.strokeDashoffset = String(total * (1 - progress));
    pct.textContent = Math.round(progress * 100) + '%';
    bar.style.width = (progress * 100) + '%';
    const label = progress === 0 ? 'Ready at pickup point' : progress < .23 ? 'Truck assigned to delivery' :
      progress < .54 ? 'Heading toward pickup / cargo loading' : progress < .86 ? 'Cargo in transit' :
      progress < 1 ? 'Approaching destination' : 'Simulation complete — shipment arrived';
    stage.textContent = label;
    status.textContent = progress >= 1 ? 'Demo finished' : running ? 'Simulation playing' : progress > 0 ? 'Paused' : 'Ready to start';
    startButton.textContent = running ? '❚❚ Pause' : progress >= 1 ? '▶ Play again' : progress > 0 ? '▶ Resume' : '▶ Start demo';
  };
  const tick = timestamp => {
    if (!running) return;
    if (prior > 0) progress = Math.min(1, progress + (timestamp - prior) / 20000 * Number(speed.value));
    prior = timestamp; draw();
    if (progress >= 1) { running = false; prior = 0; frame = null; draw(); return; }
    frame = requestAnimationFrame(tick);
  };
  startButton.addEventListener('click', () => {
    if (running) { running = false; cancelAnimationFrame(frame); frame = null; prior = 0; }
    else { if (progress >= 1) progress = 0; running = true; prior = 0; frame = requestAnimationFrame(tick); }
    draw();
  });
  resetButton.addEventListener('click', () => {
    running = false; if (frame) cancelAnimationFrame(frame); frame = null; prior = 0; progress = 0; draw();
  });
  draw();
})();
