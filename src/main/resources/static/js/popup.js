/* ============================================================
   Global pop-up alert helper — JustBuyIt style
   Usage:  showPopup('Message', 'success' | 'error', durationMs)
   ============================================================ */
(function () {
    const popup        = document.getElementById('globalPopup');
    const popupIcon    = document.getElementById('popupIcon');
    const popupMessage = document.getElementById('popupMessage');
    const popupClose   = document.getElementById('popupCloseBtn');
    const popupBar     = document.getElementById('popupProgress');

    if (!popup) return;

    let hideTimer = null;

    function hidePopup() {
        popup.classList.remove('show');
        if (hideTimer) clearTimeout(hideTimer);
        hideTimer = null;
    }

    function showPopup(message, type = 'success', duration = 4500) {
        hidePopup();
        popupMessage.textContent = message;
        popup.className = `popup-alert ${type}`;
        popupIcon.textContent = (type === 'success') ? '✓' : '!';

        // restart animation
        void popup.offsetWidth;
        popup.classList.add('show');

        // progress bar
        popupBar.style.transition = 'none';
        popupBar.style.width = '100%';
        void popupBar.offsetWidth;
        popupBar.style.transition = `width ${duration}ms linear`;
        popupBar.style.width = '0%';

        if (duration > 0) {
            hideTimer = setTimeout(hidePopup, duration);
        }
    }

    popupClose.addEventListener('click', hidePopup);
    window.showPopup = showPopup;
    window.hidePopup = hidePopup;
})();