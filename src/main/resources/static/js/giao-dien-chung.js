document.addEventListener("DOMContentLoaded", function () {
    const thongBaoDropdown = document.getElementById("thongBaoDropdown");
    if (thongBaoDropdown) {
        thongBaoDropdown.addEventListener("click", function () {
            fetch("/thongbao/markAsRead", { method: "POST" })
                .then((response) => response.text())
                .then(() => {
                    const unreadBadge = document.querySelector(".badge.bg-danger");
                    if (unreadBadge) {
                        unreadBadge.innerText = "0";
                    }
                });
        });
    }
});

document.addEventListener("DOMContentLoaded", function () {
    const cartItemsContainer = document.getElementById("cartItems");
    const cartItemCount = document.getElementById("cartItemCount");
    if (!cartItemsContainer || !cartItemCount) return;

    const cart = JSON.parse(localStorage.getItem("cart")) || [];

    function updateCartUI() {
        cartItemCount.textContent = cart.reduce((total, item) => total + item.quantity, 0);

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

    updateCartUI();
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
