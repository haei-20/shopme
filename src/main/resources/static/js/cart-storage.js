(function () {
    const storageProto = Storage.prototype;
    const rawGetItem = storageProto.getItem;
    const rawSetItem = storageProto.setItem;
    const rawRemoveItem = storageProto.removeItem;

    const ownerHint = (document.currentScript && document.currentScript.dataset.cartOwner) || 'guest';
    const activeOwner = ownerHint.trim() || 'guest';
    const initializedOwners = new Set();

    function isScopedKey(key) {
        return key === 'cart' || key === 'cartItemCount';
    }

    function scopedKey(owner, key) {
        return `gearshop.${owner}.${key}`;
    }

    function parseCart(rawValue) {
        if (!rawValue) {
            return [];
        }

        try {
            const parsed = JSON.parse(rawValue);
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function countItems(cart) {
        return cart.reduce((total, item) => total + Number(item.quantity || 0), 0);
    }

    function ensureOwnerStorage(owner) {
        if (initializedOwners.has(owner)) {
            return;
        }

        const ownerCartKey = scopedKey(owner, 'cart');
        const ownerCountKey = scopedKey(owner, 'cartItemCount');
        const guestCartKey = scopedKey('guest', 'cart');
        const guestCountKey = scopedKey('guest', 'cartItemCount');

        let ownerCartRaw = rawGetItem.call(localStorage, ownerCartKey);
        let ownerCountRaw = rawGetItem.call(localStorage, ownerCountKey);

        if (ownerCartRaw == null) {
            let sourceCartRaw = null;
            let sourceCountRaw = null;

            if (owner !== 'guest') {
                sourceCartRaw = rawGetItem.call(localStorage, guestCartKey);
                sourceCountRaw = rawGetItem.call(localStorage, guestCountKey);

                if (sourceCartRaw == null) {
                    sourceCartRaw = rawGetItem.call(localStorage, 'cart');
                    sourceCountRaw = rawGetItem.call(localStorage, 'cartItemCount');
                }
            } else {
                sourceCartRaw = rawGetItem.call(localStorage, 'cart');
                sourceCountRaw = rawGetItem.call(localStorage, 'cartItemCount');
            }

            const sourceCart = parseCart(sourceCartRaw);
            const sourceCount = sourceCountRaw != null ? sourceCountRaw : String(countItems(sourceCart));

            rawSetItem.call(localStorage, ownerCartKey, JSON.stringify(sourceCart));
            rawSetItem.call(localStorage, ownerCountKey, sourceCount);

            if (owner !== 'guest' && sourceCartRaw != null) {
                rawSetItem.call(localStorage, guestCartKey, JSON.stringify([]));
                rawSetItem.call(localStorage, guestCountKey, '0');
            }

            rawRemoveItem.call(localStorage, 'cart');
            rawRemoveItem.call(localStorage, 'cartItemCount');
        } else if (ownerCountRaw == null) {
            rawSetItem.call(localStorage, ownerCountKey, String(countItems(parseCart(ownerCartRaw))));
        }

        initializedOwners.add(owner);
    }

    storageProto.getItem = function (key) {
        if (key === 'resetCart') {
            return null;
        }

        if (!isScopedKey(key)) {
            return rawGetItem.call(this, key);
        }

        ensureOwnerStorage(activeOwner);

        const value = rawGetItem.call(this, scopedKey(activeOwner, key));
        if (key === 'cartItemCount' && value == null) {
            const cart = parseCart(rawGetItem.call(this, scopedKey(activeOwner, 'cart')));
            const count = String(countItems(cart));
            rawSetItem.call(this, scopedKey(activeOwner, key), count);
            return count;
        }

        return value;
    };

    storageProto.setItem = function (key, value) {
        if (key === 'resetCart') {
            return;
        }

        if (!isScopedKey(key)) {
            rawSetItem.call(this, key, value);
            return;
        }

        ensureOwnerStorage(activeOwner);

        if (key === 'cart') {
            const serializedCart = typeof value === 'string' ? value : JSON.stringify(value);
            const cart = parseCart(serializedCart);
            rawSetItem.call(this, scopedKey(activeOwner, 'cart'), JSON.stringify(cart));
            rawSetItem.call(this, scopedKey(activeOwner, 'cartItemCount'), String(countItems(cart)));
            return;
        }

        rawSetItem.call(this, scopedKey(activeOwner, key), String(value));
    };

    storageProto.removeItem = function (key) {
        if (key === 'resetCart') {
            return;
        }

        if (!isScopedKey(key)) {
            rawRemoveItem.call(this, key);
            return;
        }

        ensureOwnerStorage(activeOwner);

        rawRemoveItem.call(this, scopedKey(activeOwner, key));
        if (key === 'cart') {
            rawSetItem.call(this, scopedKey(activeOwner, 'cartItemCount'), '0');
        }
    };
})();

(function () {
    function updateUnreadBadgeToZero() {
        const badge = document.querySelector('#thongBaoDropdown .badge');
        if (badge) {
            badge.textContent = '0';
        }
    }

    function markItemsAsReadInUI(menu) {
        menu.querySelectorAll('.thong-bao-item').forEach(function (item) {
            item.classList.remove('tb-unread');
            if (!item.classList.contains('tb-read')) {
                item.classList.add('tb-read');
            }
        });
    }

    function ensureMarkAllButton(menu) {
        if (!menu || menu.dataset.markBtnBound === '1') return;
        const header = menu.querySelector('.dropdown-header');
        if (!header) return;

        const row = document.createElement('div');
        row.className = 'd-flex justify-content-between align-items-center';
        const title = document.createElement('span');
        title.textContent = header.textContent || 'Thong bao';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-sm btn-outline-secondary';
        btn.textContent = 'Danh dau tat ca da xem';

        row.appendChild(title);
        row.appendChild(btn);
        header.textContent = '';
        header.appendChild(row);

        btn.addEventListener('click', function (e) {
            e.preventDefault();
            fetch('/thongbao/markAsRead?all=true', { method: 'POST' })
                .then(function () {
                    markItemsAsReadInUI(menu);
                    updateUnreadBadgeToZero();
                });
        });

        menu.addEventListener('click', function (e) {
            const link = e.target.closest('.thong-bao-item');
            if (!link) return;
            fetch('/thongbao/markAsRead?all=true', { method: 'POST' });
        });

        menu.dataset.markBtnBound = '1';
    }

    document.addEventListener('DOMContentLoaded', function () {
        const menus = document.querySelectorAll('ul.dropdown-menu[aria-labelledby="thongBaoDropdown"]');
        menus.forEach(ensureMarkAllButton);
    });
})();
