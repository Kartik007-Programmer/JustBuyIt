/* ============================================================
   SHARED NAVBAR + AUTH BOOTSTRAP
   Include this on every user-facing page AFTER the DOM is ready.
   Requires: a <nav class="navbar"> block in the HTML with the
   structure below, and a common.css that defines the auth CSS.
   ============================================================ */

(function () {
    "use strict";

    const API_BASE = window.API_BASE || "http://localhost:8080";

    /* Expose a couple of things globally so per-page scripts
       (addToCart, checkout, cart, etc.) can read them. */
    window.API_BASE       = API_BASE;
    window.isGuestUser    = true;
    window.currentUser    = null;
    window.__authReady    = null;  // a Promise pages can await

    /* ---------- Helpers ---------- */
    window.isUnauthorized = function (res) {
        return res.status === 401 || res.status === 403;
    };

    /* ---------- Set UI states ---------- */
    function setGuestUI() {
        window.isGuestUser = true;
        window.currentUser = null;
        document.body.classList.remove("is-logged-in");

        const menu = document.getElementById("userMenu");
        if (menu) { menu.classList.remove("open"); menu.hidden = true; }

        const badge = document.getElementById("cartBadge");
        if (badge) { badge.innerText = "0"; badge.dataset.empty = "true"; }
    }

    function setLoggedInUI(user) {
        window.isGuestUser = false;
        window.currentUser = user;
        document.body.classList.add("is-logged-in");

        const menu = document.getElementById("userMenu");
        if (menu) menu.hidden = false;

        const display = (user.name && user.name.trim()) || user.email || "U";
        const initials = display.trim().split(/\s+/).map(s => s[0]).slice(0, 2).join("").toUpperCase();

        const avatarEl = document.getElementById("userAvatar");
        const nameEl   = document.getElementById("userName");
        if (avatarEl) avatarEl.textContent = initials;
        if (nameEl)   nameEl.textContent   = display;

        const heroTitle = document.getElementById("heroTitle");
        const heroSub   = document.getElementById("heroSub");
        if (heroTitle) heroTitle.textContent = `Welcome back, ${user.name || "shopper"} 👋`;
        if (heroSub)   heroSub.textContent   = "Pick up where you left off — your cart is waiting.";

        document.dispatchEvent(new CustomEvent("auth:logged-in", { detail: user }));
    }

    window.bootstrapAuth = async function () {
        try {
            const res = await fetch(`${API_BASE}/users/me`, {
                credentials: "include",
                headers: { "Accept": "application/json" }
            });

            // Only a real 401/403 means guest. Anything else is a transient error.
            if (res.status === 401 || res.status === 403) {
                setGuestUI();
                return null;
            }
            if (!res.ok) {
                // 5xx / network blip — leave UI in a neutral state
                return { __error: true };
            }

            const user = await res.json();

            if (user.role === "ADMIN" || user.role === "SECONDARY_ADMIN") {
                window.location.replace("/Admin_Pages/AdminDashboad.html");
                return null;
            }
            setLoggedInUI(user);
            return user;
        } catch (err) {
            console.warn("Auth check failed:", err);
            return { __error: true };  // do NOT setGuestUI here
        }
    };

    /* ---------- Logout (global) ---------- */
    window.logout = function () {
        fetch(`${API_BASE}/auth/v1/logout`, {
            method: "POST",
            credentials: "include"
        })
            .catch(err => console.error("Logout error:", err))
            .finally(() => {
                window.location.href = "/LoginForm.html";
            });
    };

    /* ---------- User pill dropdown ---------- */
    function setupUserMenu() {
        const menu = document.getElementById("userMenu");
        const pill = document.getElementById("userPill");
        if (!menu || !pill) return;

        const open  = () => { menu.classList.add("open");  pill.setAttribute("aria-expanded", "true"); };
        const close = () => { menu.classList.remove("open"); pill.setAttribute("aria-expanded", "false"); };

        pill.addEventListener("click", (e) => {
            e.stopPropagation();
            menu.classList.contains("open") ? close() : open();
        });

        document.addEventListener("click", (e) => {
            if (!menu.contains(e.target)) close();
        });

        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") close();
        });
    }

    /* ---------- Auth prompt modal ---------- */
    window.openAuthModal = function () {
        const modal = document.getElementById("authModal");
        if (!modal) return;

        const here = encodeURIComponent(location.pathname + location.search);
        const loginLink    = modal.querySelector(".auth-btn-primary");
        const registerLink = modal.querySelector(".auth-btn-secondary");
        if (loginLink)    loginLink.href    = `/LoginForm.html?redirect=${here}`;
        if (registerLink) registerLink.href = `/RegistrationForm.html?redirect=${here}`;

        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    };

    window.closeAuthModal = function () {
        const modal = document.getElementById("authModal");
        if (!modal) return;
        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";
    };

    /* ---------- Cart nav gate ---------- */
    function setupCartNavGate() {
        const cartLink = document.getElementById("cartNavLink");
        if (!cartLink) return;

        cartLink.addEventListener("click", (e) => {
            if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || e.button !== 0) return;

            if (window.isGuestUser) {
                e.preventDefault();
                window.openAuthModal();
            }
        });
    }

    /* ---------- Cart badge refresh ---------- */
    window.refreshCartBadge = function (animate = false) {
        if (window.isGuestUser) {
            const badge = document.getElementById("cartBadge");
            if (badge) { badge.innerText = "0"; badge.dataset.empty = "true"; }
            return;
        }

        fetch(`${API_BASE}/users/cart`, {
            credentials: "include",
            headers: { "Accept": "application/json" }
        })
            .then(res => {
                if (window.isUnauthorized(res)) return null;
                if (!res.ok) return null;
                return res.json();
            })
            .then(cart => {
                if (!cart) return;
                const badge = document.getElementById("cartBadge");
                if (!badge) return;

                const count = (cart.items || []).reduce((s, i) => s + (i.quantity || 0), 0);
                const changed = badge.innerText !== String(count);
                badge.innerText = count;
                badge.dataset.empty = count === 0 ? "true" : "false";

                if (animate && changed && count > 0) {
                    badge.classList.remove("pop");
                    void badge.offsetWidth;
                    badge.classList.add("pop");
                    setTimeout(() => badge.classList.remove("pop"), 500);
                }
            })
            .catch(() => {});
    };

    /* ---------- Ripple helper (shared) ---------- */
    window.spawnRipple = function (btn, event) {
        if (!btn || !event) return;
        const rect = btn.getBoundingClientRect();
        const ripple = document.createElement("span");
        ripple.className = "ripple";
        const size = Math.max(rect.width, rect.height) * 2;
        ripple.style.width = ripple.style.height = `${size}px`;
        ripple.style.left  = `${event.clientX - rect.left - size / 2}px`;
        ripple.style.top   = `${event.clientY - rect.top  - size / 2}px`;
        btn.appendChild(ripple);
        setTimeout(() => ripple.remove(), 600);
    };

    /* ---------- BFCACHE reload ---------- */
    window.addEventListener("pageshow", (e) => {
        if (e.persisted) window.location.reload();
    });

    /* ---------- Wire everything up on DOM ready ---------- */
    function init() {
        setupUserMenu();
        setupCartNavGate();

        // Modal close handlers
        document.addEventListener("click", (e) => {
            if (e.target && e.target.id === "authModal") window.closeAuthModal();
        });
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") window.closeAuthModal();
        });

        // Run the auth check; expose it as a Promise the page can await
        window.__authReady = window.bootstrapAuth();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

/* ---------- Wire everything up on DOM ready ---------- */
function init() {
    setupUserMenu();
    setupCartNavGate();

    document.addEventListener("click", (e) => {
        if (e.target && e.target.id === "authModal") window.closeAuthModal();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") window.closeAuthModal();
    });

    window.__authReady = window.bootstrapAuth();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
} else {
    init();
}

/* ------------------------------------------------------------
   Shared page guard.
   Usage:
     const user = await window.requireAuth();
     if (!user) return;     // guest → already redirected to login
     // otherwise, user is a valid logged-in user object
   ------------------------------------------------------------ */
window.requireAuth = async function requireAuth() {
    let user = await window.__authReady;

    // Distinguish "not authenticated" from "transient failure".
    // Only a confirmed 401/403 marks the user as a guest.
    if (user && user.__error) {
        // one retry — cookie may just have been set
        user = await window.bootstrapAuth();
    }

    if (window.isGuestUser || user === null) {
        const here = encodeURIComponent(location.pathname + location.search);
        window.location.href = `/LoginForm.html?redirect=${here}`;
        return null;
    }

    if (user && user.__error) {
        // Server unreachable — do NOT treat as guest, but don't proceed either
        return null;
    }

    // Admins should never land on user pages
    if (user.role === "ADMIN" || user.role === "SECONDARY_ADMIN") {
        window.location.replace("/Admin_Pages/AdminDashboad.html");
        return null;
    }

    return user;
};