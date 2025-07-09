document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = document.getElementById("loginEmail").value;
  const password = document.getElementById("loginPassword").value;
  const res = await fetch("/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (res.ok) {
    const data = await res.json();
    sessionStorage.setItem("token", data.token); // 세션 유지
    document.getElementById("loginMsg").innerText = "로그인 성공!";
  } else {
    document.getElementById("loginMsg").innerText = "로그인 실패";
  }
});
