document.getElementById("signupForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = document.getElementById("signupEmail").value;
  const password = document.getElementById("signupPassword").value;
  const res = await fetch("/signup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (res.ok) {
    document.getElementById("signupMsg").innerText = "회원가입 성공!";
  } else {
    document.getElementById("signupMsg").innerText = "회원가입 실패";
  }
});
