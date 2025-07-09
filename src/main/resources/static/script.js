if (!sessionStorage.getItem("token")) {
  alert("로그인이 필요합니다.");
  window.location.href = "/login.html";
}

// JWT 디코딩 함수 (payload만 추출)
function parseJwt(token) {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map(function (c) {
          return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

// 토큰 및 만료 카운트 표시 (토큰은 1/4만 표시)
const token = sessionStorage.getItem("token");
const shortToken = token
  ? token.substring(0, Math.floor(token.length / 4)) + "..."
  : "";
document.getElementById("tokenValue").innerText = shortToken;

const payload = parseJwt(token);
if (payload && payload.exp) {
  function updateExpireCountdown() {
    const now = Math.floor(Date.now() / 1000);
    const remain = payload.exp - now;
    document.getElementById("tokenExpire").innerText = remain > 0 ? remain : 0;
    if (remain <= 0) {
      alert("토큰이 만료되었습니다. 다시 로그인 해주세요.");
      sessionStorage.removeItem("token");
      window.location.href = "/login.html";
    }
  }
  updateExpireCountdown();
  setInterval(updateExpireCountdown, 1000);
} else {
  document.getElementById("tokenExpire").innerText = "알 수 없음";
}

// 로그아웃 버튼 기능
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    sessionStorage.removeItem("token");
    alert("로그아웃되었습니다.");
    window.location.href = "/login.html";
  });
}

document.getElementById("askBtn").addEventListener("click", async () => {
  const prompt = document.getElementById("prompt").value.trim();
  const previousQuestionId = document
    .getElementById("previousQuestionId")
    .value.trim();

  if (!prompt) {
    alert("질문을 입력해주세요.");
    return;
  }

  const payload = { prompt };
  if (previousQuestionId) {
    payload.previousQuestionId = previousQuestionId;
  }

  try {
    const token = sessionStorage.getItem("token");
    const response = await fetch(`/ask-context`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
      body: JSON.stringify(payload),
    });

    const answer = await response.text();
    document.getElementById("answerArea").innerText = answer;
  } catch (err) {
    alert("질문 전송 실패");
    console.error(err);
  }
});
