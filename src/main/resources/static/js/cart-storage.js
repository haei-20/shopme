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
    function updateUnreadBadgeCount(nextCount) {
        const badge = document.querySelector('#thongBaoDropdown .badge');
        if (badge) {
            badge.textContent = String(Math.max(0, Number(nextCount || 0)));
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

    function renderThongBaoItems(menu, items) {
        if (!menu) return;
        menu.querySelectorAll('li').forEach(function (li) {
            if (!li.querySelector('.dropdown-header')) {
                li.remove();
            }
        });

        if (!Array.isArray(items) || items.length === 0) {
            const emptyLi = document.createElement('li');
            emptyLi.className = 'thongbao-empty';
            emptyLi.innerHTML = '<i class="bi bi-inbox" aria-hidden="true"></i><p class="mb-0">Không có thông báo mới.</p>';
            menu.appendChild(emptyLi);
            return;
        }

        items.forEach(function (item) {
            const li = document.createElement('li');
            const link = document.createElement('a');
            link.className = 'dropdown-item thong-bao-item ' + (item.cssClass || 'tb-default');
            link.href = item.link || '#';
            link.setAttribute('data-thong-bao-id', String(item.id || ''));

            const content = document.createElement('div');
            content.textContent = item.noiDung || '';
            const time = document.createElement('small');
            time.textContent = item.ngayThongBao || '';
            link.appendChild(content);
            link.appendChild(time);
            li.appendChild(link);
            menu.appendChild(li);
        });
    }

    function refreshThongBaoMenu(menu) {
        if (!menu) return;
        fetch('/thongbao/list', { method: 'GET' })
            .then(function (res) {
                if (!res.ok) throw new Error('Không thể tải thông báo');
                return res.json();
            })
            .then(function (data) {
                if (!data || data.success !== true) return;
                renderThongBaoItems(menu, data.items || []);
                updateUnreadBadgeCount(data.unreadCount || 0);
            })
            .catch(function () {
                // Giữ nguyên danh sách hiện tại nếu lỗi mạng.
            });
    }

    function ensureMarkAllButton(menu) {
        if (!menu || menu.dataset.markBtnBound === '1') return;
        const header = menu.querySelector('.dropdown-header');
        if (!header) return;

        const row = document.createElement('div');
        row.className = 'thongbao-header-row';
        const title = document.createElement('span');
        title.className = 'thongbao-header-title';
        title.textContent = (header.textContent || '').trim() || 'Thông báo';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'thongbao-mark-all-btn';
        btn.setAttribute('aria-label', 'Đánh dấu tất cả thông báo đã xem');
        btn.innerHTML = '<i class="bi bi-check2-all" aria-hidden="true"></i><span>Đánh dấu tất cả đã xem</span>';

        row.appendChild(title);
        row.appendChild(btn);
        header.textContent = '';
        header.appendChild(row);

        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            fetch('/thongbao/markAsRead?all=true', { method: 'POST' })
                .then(function () {
                    markItemsAsReadInUI(menu);
                    updateUnreadBadgeCount(0);
                    refreshThongBaoMenu(menu);
                });
        });

        const dropdownTrigger = document.getElementById('thongBaoDropdown');
        if (dropdownTrigger) {
            dropdownTrigger.addEventListener('shown.bs.dropdown', function () {
                refreshThongBaoMenu(menu);
            });
        }

        menu.dataset.markBtnBound = '1';
    }

    document.addEventListener('DOMContentLoaded', function () {
        const menus = document.querySelectorAll('ul.dropdown-menu[aria-labelledby="thongBaoDropdown"]');
        menus.forEach(ensureMarkAllButton);
    });
})();
