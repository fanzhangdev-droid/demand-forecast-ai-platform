// Spring Boot API Gateway のベース URL。
// App.jsx から切り出すことで、URL 変更時にここだけ直せばよくなる。
const BASE_URL = 'http://localhost:8080/api/forecast';

// フロントエンドの camelCase フォームデータを、バックエンドが期待する
// snake_case リクエストボディに変換する内部関数。
// App.jsx に書き続けると「UI の関心」と「通信フォーマットの関心」が混在するため分離した。
function toForecastRequest(formData) {
  return {
    date: formData.date,
    gender: formData.gender,
    age: Number(formData.age),
    product_category: formData.productCategory,
    price_per_unit: Number(formData.pricePerUnit),
  };
}

// POST /api/forecast を呼び出し、レスポンス JSON をそのまま返す。
// response.ok でない場合は Error を throw することで、呼び出し元（App.jsx）が
// catch して setError できるようにする。setState はここでは一切行わない。
export async function postForecast(formData) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toForecastRequest(formData)),
  });

  if (!res.ok) {
    // バックエンドがエラー詳細を JSON で返す場合はそれを使い、
    // 読み取れなかった場合はステータスに応じたデフォルトメッセージを使う。
    let message;
    try {
      const errJson = await res.json();
      message = errJson.message ?? errJson.error ?? null;
    } catch {
      message = null;
    }

    if (!message) {
      if (res.status >= 500) {
        message = 'サーバー内部でエラーが発生しました。しばらく待ってから再試行してください。';
      } else if (res.status === 400 || res.status === 422) {
        message = '入力値に誤りがあります。各フィールドの内容を確認してください。';
      } else {
        message = `予測リクエストに失敗しました（HTTP ${res.status}）`;
      }
    }

    throw new Error(message);
  }

  return res.json();
}

// GET /api/forecast/history を呼び出し、レスポンス JSON をそのまま返す。
// filters: { productCategory?, date?, page?, size? }
// productCategory / date が空値の場合は query string に含めない。
// page / size は常に送信し、省略時はそれぞれ 0 / 10 をデフォルトとする。
export async function fetchHistory(filters = {}) {
  const params = new URLSearchParams();
  if (filters.productCategory) params.append('productCategory', filters.productCategory);
  if (filters.date) params.append('date', filters.date);
  params.append('page', filters.page ?? 0);
  params.append('size', filters.size ?? 10);

  const url = `${BASE_URL}/history?${params.toString()}`;

  const res = await fetch(url);

  if (!res.ok) {
    let message;
    try {
      const errJson = await res.json();
      message = errJson.message ?? errJson.error ?? null;
    } catch {
      message = null;
    }
    throw new Error(message ?? `履歴の取得に失敗しました（HTTP ${res.status}）`);
  }

  return res.json();
}
