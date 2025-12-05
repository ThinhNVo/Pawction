var socket = new SockJS('/ws-auction');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to home page updates
    stompClient.subscribe('/topic/home', function(message) {
        var update = JSON.parse(message.body);
        updateHomePage(update);
    });

    stompClient.subscribe('/topic/myAccount', function(message) {
        var update = JSON.parse(message.body);
        updateMyAccount(update);
    });



    // Subscribe to specific auction (product view page)
    var auctionIdElement = document.getElementById("auctionId");
    if (auctionIdElement) {
        var auctionId = auctionIdElement.value;
        stompClient.subscribe('/topic/auction/' + auctionId, function(message) {
            var update = JSON.parse(message.body);
            updateProductPage(update);
        });
    }

    stompClient.subscribe('/topic/bids/' + auctionId, function(message) {
        var bid = JSON.parse(message.body);
        appendBidRow(bid);

        if (bid.winning) {
            var winningEl = document.getElementById("winningBidder");
            if (winningEl && bid.bidderName) {
                winningEl.innerText = bid.bidderName;
            }
        }
    });
});

function updateHomePage(update) {
    var priceEl = document.getElementById("auction-price-" + update.auctionId);
    var bidsEl = document.getElementById("auction-bids-" + update.auctionId);

    if (priceEl) {
        priceEl.innerText = "Current Bid Price: $" + formatCurrency(update.highestBid);
    }
    if (bidsEl) {
        bidsEl.innerText = update.bidCount + " bids";
    }
}

function updateMyAccount(update) {
    let priceEl = document.getElementById('auction-price-' + update.auctionId);
    let bidsEl = document.getElementById('auction-bids-' + update.auctionId);
    if (priceEl) {
        priceEl.textContent = 'Current Bid Price: $' + formatCurrency(update.highestBid);
    }
    if (bidsEl) {
        bidsEl.textContent = update.bidCount + ' bids';
    }
}

function formatCurrency(value) {
    return Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function updateProductPage(update) {
    var priceEl = document.getElementById("currentPrice");
    var countEl = document.getElementById("bidCount");
    var userBidEl = document.getElementById("userBidAmount");
    var bidInput = document.getElementById("minNextBidAmount");
    if (priceEl) {
        priceEl.innerText = formatCurrency(update.highestBid);
    }
    if (countEl) {
        countEl.innerText = update.bidCount;
    }
    if (userBidEl && update.userBidAmount) {
        userBidEl.innerText = formatCurrency(update.userBidAmount);
    }
    if (bidInput && update.minNextBidAmount) {
        bidInput.min = update.minNextBidAmount; // 👈 update the min attribute
    }
}

function appendBidRow(bid) {
    var tbody = document.getElementById("bidListBody");
    var row = document.createElement("tr");
    row.innerHTML =
        "<td>" + bid.bidderName + "</td>" +
        "<td>$" + formatCurrency(bid.amount) + "</td>" +
        "<td>" + formatBidTime(bid.bidTime) + "</td>";
    tbody.insertBefore(row, tbody.firstChild);
}

function formatBidTime(isoString) {
    const d = new Date(isoString);
    const month = d.toLocaleString('en-US', { month: 'short' }); // "Dec"
    const day = d.getDate(); // 4
    const year = d.getFullYear(); // 2025

    let hours = d.getHours(); // 0–23
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12 || 12; // convert to 12-hour, 0 -> 12

    return `${month} ${day}, ${year} ${hours}:${minutes} ${ampm}`;
}

