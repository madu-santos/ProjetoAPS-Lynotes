document.addEventListener("DOMContentLoaded", function () {
    var targets = document.querySelectorAll("[data-reveal]");

    if ("IntersectionObserver" in window) {
        var observer = new IntersectionObserver(function (entries, obs) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    obs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.18 });

        targets.forEach(function (target) {
            observer.observe(target);
        });
    } else {
        targets.forEach(function (target) {
            target.classList.add("is-visible");
        });
    }

    document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
        anchor.addEventListener("click", function (event) {
            var href = anchor.getAttribute("href");
            if (!href || href === "#") {
                return;
            }

            var section = document.querySelector(href);
            if (!section) {
                return;
            }

            event.preventDefault();
            section.scrollIntoView({ behavior: "smooth", block: "start" });
        });
    });
});
