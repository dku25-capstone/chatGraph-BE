document.getElementById('newChatBtn').addEventListener('click', async () => {
    try {
        const response = await fetch('/new-chat', { method: 'POST' });
        const sessionId = await response.text();
        document.getElementById('sessionId').value = sessionId;
        alert(`새 세션이 생성되었습니다: ${sessionId}`);
    } catch (err) {
        alert("세션 생성 실패");
        console.error(err);
    }
});

document.getElementById('askBtn').addEventListener('click', async () => {
    const sessionId = document.getElementById('sessionId').value.trim();
    const prompt = document.getElementById('prompt').value.trim();
    const previousQuestionId = document.getElementById('previousQuestionId').value.trim();

    if (!sessionId || !prompt) {
        alert("세션 ID와 질문을 모두 입력해주세요.");
        return;
    }

    const payload = { prompt };
    if (previousQuestionId) {
        payload.previousQuestionId = previousQuestionId;
    }

    try {
        const response = await fetch(`/ask-context?sessionId=${encodeURIComponent(sessionId)}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const answer = await response.text();
        document.getElementById('answerArea').innerText = answer;
    } catch (err) {
        alert("질문 전송 실패");
        console.error(err);
    }
});
