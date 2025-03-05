document.addEventListener("DOMContentLoaded", function() {
  const starCount = 45; // Number of stars to create
  const shootingStarInterval = 3000; // Interval between shooting stars in milliseconds

  function createStar() {
    const star = document.createElement("div");
    star.className = "star";
    star.style.top = `${Math.random() * 100}%`;
    star.style.left = `${Math.random() * 100}%`;
    document.getElementById("stars").appendChild(star);
  }

  function createShootingStar() {
    const shootingStar = document.createElement("div");
    shootingStar.className = "shooting-star";
    shootingStar.style.top = `${Math.random() * 100}%`;
    shootingStar.style.left = `${Math.random() * 100}%`;
    document.body.appendChild(shootingStar);
    setTimeout(() => {
      document.body.removeChild(shootingStar);
    }, 3000);
  }

  function generateStars() {
    for (let i = 0; i < starCount; i++) {
      createStar();
    }
  }

  generateStars();
  setTimeout(createShootingStar, Math.random() * shootingStarInterval); // Initial shooting star

  // Randomize shooting star interval
  function randomizeShootingStarInterval() {
    const interval = Math.random() * shootingStarInterval;
    setTimeout(function() {
      createShootingStar();
      randomizeShootingStarInterval();
    }, interval);
  }

  randomizeShootingStarInterval();
});
