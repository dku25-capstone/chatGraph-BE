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

//userID 표시
const token = sessionStorage.getItem("token");
const payload = parseJwt(token);
document.getElementById("userIdValue").innerText =
  payload && payload.sub ? payload.sub : "알 수 없음";

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
    console.log("token", token);
    const response = await fetch(`/ask-context`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
      body: JSON.stringify(payload),
    });

    console.log("Response: ", response.ok, "Status: ", response.status);

    const data = await response.json();
    console.log("Data: ", data);

    document.getElementById("answerArea").innerText = data.answer;
    document.getElementById("questionIdValue").innerText = data.questionId;
  } catch (err) {
    alert("질문 전송 실패");
    console.error(err);
  }
});

// 토픽 목록 조회 버튼 기능
document.getElementById("topicsBtn").addEventListener("click", async () => {
  try {
    const token = sessionStorage.getItem("token");
    const response = await fetch(`/topics/history`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
    });

    console.log("Topics Response: ", response.ok, "Status: ", response.status);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const topics = await response.json();
    console.log("Topics Data: ", topics);

    const topicsArea = document.getElementById("topicsArea");

    if (topics && topics.length > 0) {
      const topicsList = topics
        .map(
          (topic) =>
            `<div style="margin-bottom: 10px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; background-color: white;">
          <strong>토픽 ID:</strong> ${topic.topicId}<br>
          <strong>토픽명:</strong> ${topic.topicName}<br>
          <strong>생성일:</strong> ${topic.createdAt || "N/A"}
          <br><button onclick="viewTopicQuestions('${
            topic.topicId
          }')" style="margin-top: 5px; padding: 5px 10px; background-color: #007bff; color: white; border: none; border-radius: 3px; cursor: pointer;">질문-답변 보기</button>
        </div>`
        )
        .join("");

      topicsArea.innerHTML = topicsList;
    } else {
      topicsArea.innerHTML = "<em>아직 생성된 토픽이 없습니다.</em>";
    }
  } catch (err) {
    console.error("토픽 목록 조회 실패:", err);
    document.getElementById("topicsArea").innerHTML =
      '<em style="color: red;">토픽 목록 조회에 실패했습니다.</em>';
  }
});

// 토픽의 질문-답변 조회 함수
async function viewTopicQuestions(topicId) {
  try {
    const token = sessionStorage.getItem("token");
    const response = await fetch(`/topics/${topicId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + token,
      },
    });

    console.log(
      "Questions Response: ",
      response.ok,
      "Status: ",
      response.status
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const questions = await response.json();
    console.log("Questions Data: ", questions);

    const questionsArea = document.getElementById("questionsArea");

    if (questions && questions.length > 0) {
      const questionsList = questions
        .map(
          (question) =>
            `<div style="margin-bottom: 15px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; background-color: white;">
          <div style="margin-bottom: 8px;">
            <strong>질문 (Level ${question.level}):</strong> ${
              question.questionText
            }<br>
            <small style="color: #666;">질문 ID: ${question.questionId}</small>
          </div>
          <div style="margin-left: 20px; padding: 8px; background-color: #f8f9fa; border-left: 3px solid #007bff;">
            <strong>답변:</strong> ${question.answerText}<br>
            <small style="color: #666;">답변 ID: ${question.answerId}</small>
          </div>
          <small style="color: #999;">생성일: ${
            question.createdAt || "N/A"
          }</small>
        </div>`
        )
        .join("");

      questionsArea.innerHTML = questionsList;
    } else {
      questionsArea.innerHTML =
        "<em>이 토픽에는 아직 질문-답변이 없습니다.</em>";
    }
  } catch (err) {
    console.error("질문-답변 조회 실패:", err);
    document.getElementById("questionsArea").innerHTML =
      '<em style="color: red;">질문-답변 조회에 실패했습니다.</em>';
  }
}
