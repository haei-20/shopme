document.addEventListener("DOMContentLoaded", function () {
    const cartItemsContainer = document.getElementById("cartItems");
    const cartItemCount = document.getElementById("cartItemCount");
    if (!cartItemsContainer || !cartItemCount) return;

    function renderCartUI(cart) {
        cartItemCount.textContent = cart.reduce((total, item) => total + Number(item.quantity || 0), 0);

        if (cart.length === 0) {
            cartItemsContainer.innerHTML = '<p class="text-muted">Giỏ hàng của bạn đang trống.</p>';
            return;
        }

        cartItemsContainer.innerHTML = cart
            .map(
                (item) => `
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div>
                        <strong><a href="/chitietsanpham/${item.sanPhamID}" style="text-decoration: none; color: #3b3428;">${item.name}</a></strong>
                        <p class="mb-0 text-muted">Số lượng: ${item.quantity}</p>
                    </div>
                    <span>${item.price}</span>
                </div>
            `
            )
            .join("");
    }

    async function updateCartUIFromServer() {
        try {
            const response = await fetch("/api/cart", { method: "GET", headers: { "Content-Type": "application/json" } });
            if (!response.ok) {
                renderCartUI([]);
                return;
            }
            const data = await response.json();
            renderCartUI(Array.isArray(data.cart) ? data.cart : []);
        } catch (error) {
            renderCartUI([]);
        }
    }

    updateCartUIFromServer();
    window.addEventListener("storage", function (event) {
        if (event.key === "cart" || event.key === "cartItemCount") {
            updateCartUIFromServer();
        }
    });
    window.addEventListener("cart:updated", updateCartUIFromServer);
    document.addEventListener("visibilitychange", function () {
        if (!document.hidden) {
            updateCartUIFromServer();
        }
    });
});

document.addEventListener("DOMContentLoaded", function () {
    const cartDropdown = document.getElementById("cartDropdown");
    const dropdownMenu = cartDropdown ? cartDropdown.closest(".dropdown")?.querySelector(".dropdown-menu") : null;
    if (!cartDropdown || !dropdownMenu) return;

    cartDropdown.addEventListener("click", function (e) {
        e.preventDefault();
        dropdownMenu.classList.toggle("show");
    });

    dropdownMenu.addEventListener("click", function (e) {
        e.stopPropagation();
    });

    document.addEventListener("click", function (e) {
        if (!cartDropdown.contains(e.target) && !dropdownMenu.contains(e.target)) {
            dropdownMenu.classList.remove("show");
        }
    });
});

document.addEventListener("DOMContentLoaded", function () {
    const chatbotIcon = document.getElementById("chatbotIcon");
    const chatContainer = document.getElementById("chat-container");
    const chatBox = document.getElementById("chat-box");
    const userInput = document.getElementById("user-input");
    if (!chatbotIcon || !chatContainer || !chatBox || !userInput) {
        window.sendMessage = function () {};
        return;
    }

    chatbotIcon.addEventListener("click", function (e) {
        e.preventDefault();
        chatContainer.style.display = chatContainer.style.display === "none" ? "block" : "none";
    });

    document.addEventListener("click", function (e) {
        if (!chatContainer.contains(e.target) && !chatbotIcon.contains(e.target)) {
            chatContainer.style.display = "none";
        }
    });

    function sendMessage() {
        const userMessageText = userInput.value.trim();
        if (userMessageText === "") return;

        const userMessage = document.createElement("div");
        userMessage.className = "user-message";
        userMessage.textContent = userMessageText;
        chatBox.appendChild(userMessage);

        fetch("/chatbot/sendMessage", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: userMessageText }),
        })
            .then((response) => response.json())
            .then((data) => {
                const botMessage = document.createElement("div");
                botMessage.className = "bot-message";
                const tenShop = (document.querySelector(".site-brand a, .fs-3.fw-bold a")?.textContent || "GearShop").trim();
                botMessage.textContent = data.error ? `Bot: ${data.error}` : `${tenShop}: ${data.response}`;
                chatBox.appendChild(botMessage);
                userInput.value = "";
                chatBox.scrollTop = chatBox.scrollHeight;
            })
            .catch(() => {
                const errorMessage = document.createElement("div");
                errorMessage.className = "bot-message";
                errorMessage.textContent = "Bot: Failed to fetch response.";
                chatBox.appendChild(errorMessage);
            });
    }

    window.sendMessage = sendMessage;
});

document.addEventListener("DOMContentLoaded", function () {
    const productFilterForms = document.querySelectorAll(
        'body.with-category-nav nav.bg-light.py-4.border-bottom form[action^="/sanpham"]'
    );
    if (productFilterForms.length === 0) return;

    productFilterForms.forEach((form) => {
        const controls = form.querySelectorAll("select");
        controls.forEach((control) => {
            control.addEventListener("change", () => form.submit());
        });
    });
});

document.addEventListener("DOMContentLoaded", function () {
    if (!document.body.classList.contains("with-category-nav")) return;

    const productLinks = document.querySelectorAll(
        '.product-card[href*="/chitietsanpham/"], .product-card-image > a[href*="/chitietsanpham/"]'
    );
    if (productLinks.length === 0) return;
    const processedCards = new Set();

    function extractProductId(url) {
        if (!url) return null;
        const cleanUrl = url.split("?")[0].replace(/\/+$/, "");
        const parts = cleanUrl.split("/");
        return parts.length ? parts[parts.length - 1] : null;
    }

    function normalizePrice(rawPrice) {
        return (rawPrice || "").replace(/\s+/g, " ").trim();
    }

    function writeCartToStorage(nextCart) {
        const cart = Array.isArray(nextCart) ? nextCart : [];
        localStorage.setItem("cart", JSON.stringify(cart));
        localStorage.setItem(
            "cartItemCount",
            cart.reduce((total, item) => total + Number(item.quantity || 0), 0)
        );
        window.dispatchEvent(new Event("cart:updated"));
    }

    function showAddToCartToast(message) {
        const toastNode = document.createElement("div");
        toastNode.className = "floating-cart-toast";
        toastNode.textContent = message;
        document.body.appendChild(toastNode);
        window.setTimeout(() => toastNode.classList.add("show"), 10);
        window.setTimeout(() => {
            toastNode.classList.remove("show");
            window.setTimeout(() => toastNode.remove(), 220);
        }, 1700);
    }

    productLinks.forEach((link) => {
        const card = link.classList.contains("product-card") ? link : link.closest(".product-card-image");
        if (!card || card.querySelector(".btn-quick-add-cart")) return;
        if (processedCards.has(card)) return;

        card.classList.add("quick-add-host");
        const sanPhamID = extractProductId(link.getAttribute("href"));
        if (!sanPhamID) return;

        const titleNode = card.querySelector(".card-title");
        const priceNode = card.querySelector(".card-text, .text-danger");
        const imageNode = card.querySelector("img");

        const addBtn = document.createElement("button");
        addBtn.type = "button";
        addBtn.className = "btn btn-sm btn-warning btn-quick-add-cart";
        addBtn.innerHTML = '<i class="bi bi-cart-plus" aria-hidden="true"></i>';
        addBtn.setAttribute("aria-label", "Thêm sản phẩm vào giỏ hàng");
        addBtn.title = "Thêm vào giỏ hàng";
        addBtn.dataset.productId = sanPhamID;
        addBtn.dataset.productName = titleNode ? titleNode.textContent.trim() : "Sản phẩm";
        addBtn.dataset.productPrice = normalizePrice(priceNode ? priceNode.textContent : "");
        addBtn.dataset.productImage = imageNode ? imageNode.getAttribute("src") || "" : "";
        card.appendChild(addBtn);
        processedCards.add(card);

        addBtn.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            if (addBtn.dataset.loading === "1") return;

            const product = {
                sanPhamID: addBtn.dataset.productId,
                name: addBtn.dataset.productName || "Sản phẩm",
                price: addBtn.dataset.productPrice || "",
                image: addBtn.dataset.productImage || "",
                quantity: 1,
            };

            addBtn.dataset.loading = "1";
            addBtn.disabled = true;

            fetch("/chitietsanpham/add-to-cart", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(product),
            })
                .then(async (response) => {
                    const data = await response.json();
                    if (!response.ok || !data.success) {
                        throw new Error((data && data.message) || "Không thể thêm vào giỏ hàng");
                    }
                    writeCartToStorage(data.cart || []);
                    showAddToCartToast("Đã thêm sản phẩm vào giỏ hàng");
                })
                .catch((error) => {
                    showAddToCartToast(error.message || "Lỗi khi thêm giỏ hàng");
                })
                .finally(() => {
                    addBtn.dataset.loading = "0";
                    addBtn.disabled = false;
                });
        });
    });
});
