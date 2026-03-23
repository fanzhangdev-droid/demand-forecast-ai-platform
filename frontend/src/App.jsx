import { useState, useEffect } from 'react';
import { postForecast, fetchHistory } from './api/forecastApi';

function App() {
  // フォームの各入力値をまとめて1つの state で管理する。
  const [form, setForm] = useState({
    date: '',
    gender: '',
    age: '',
    productCategory: '',
    pricePerUnit: '',
  });

  // 予測結果の数値。null のときは未取得と判断して表示しない。
  const [predictedQuantity, setPredictedQuantity] = useState(null);

  // 予測履歴のリスト。
  const [history, setHistory] = useState([]);

  // 履歴検索フォームの絞り込み条件。空文字 = フィルターなし。
  const [historyFilter, setHistoryFilter] = useState({ productCategory: '', date: '' });

  // ページング状態。loadHistory が完了するたびに更新される。
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  // フィルターが適用された状態かどうか。空データ時のメッセージ出し分けに使う。
  const [isFiltered, setIsFiltered] = useState(false);

  // API 呼び出し中かどうかを示すフラグ。ボタンの無効化に使う。
  const [loading, setLoading] = useState(false);

  // リクエスト送信中に表示するステータスメッセージ。
  // loading が true の間だけ意味を持ち、完了後（finally）で空に戻す。
  const [statusMessage, setStatusMessage] = useState('');

  // 予測が完了したときに表示するメッセージ。
  // 次のリクエスト開始時にクリアすることで「直近の操作結果」だと伝わる。
  const [successMessage, setSuccessMessage] = useState('');

  // エラーメッセージ。空文字のときは非表示にする。
  const [error, setError] = useState('');

  // フォーム送信前の入力値を検証し、エラーメッセージの配列を返す。
  // 空配列のときは検証通過。
  const validateForm = (f) => {
    const errors = [];
    if (!f.date) errors.push('Date は必須です');
    if (!f.gender) errors.push('Gender を選択してください');
    const age = Number(f.age);
    if (f.age === '' || isNaN(age) || age < 1 || age > 120)
      errors.push('Age は 1〜120 の整数を入力してください');
    if (!f.productCategory) errors.push('Product Category を選択してください');
    const price = Number(f.pricePerUnit);
    if (f.pricePerUnit === '' || isNaN(price) || price <= 0)
      errors.push('Price per Unit は 0 より大きい値を入力してください');
    return errors;
  };

  // 履歴を取得して state に格納する。
  // pageNum を省略すると 0 ページ目（先頭）を取得する。
  // useEffect より前に定義することで、参照エラー（TDZ）を確実に回避する。
  const loadHistory = async (filters = {}, pageNum = 0) => {
    try {
      const json = await fetchHistory({ ...filters, page: pageNum, size: 10 });
      const d = json.data;
      setHistory(d.content ?? []);
      setPage(d.page ?? 0);
      setTotalPages(d.totalPages ?? 1);
      setHasNext(d.hasNext ?? false);
      setHasPrevious(d.hasPrevious ?? false);
    } catch (err) {
      // 履歴取得の失敗はページ全体のエラーにはせず、コンソールのみに出す。
      console.error('履歴の取得に失敗しました:', err.message);
    }
  };

  // マウント時に履歴を取得する。依存配列が [] なので初回のみ実行される。
  useEffect(() => {
    loadHistory();
  }, []);

  // どの入力欄が変わっても同じ関数で処理できるよう、name 属性をキーに使う。
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // Predict ボタンを押したときの処理。
  const handleSubmit = async (e) => {
    e.preventDefault();

    // フロントエンド側の入力検証。失敗時はリクエストを送らずにエラーを表示する。
    const validationErrors = validateForm(form);
    if (validationErrors.length > 0) {
      setError(validationErrors.join('\n'));
      return;
    }

    // 新しいリクエストを始めるとき、前回の結果・メッセージをすべてリセットする。
    // こうすることで「古い結果が残って混乱する」状況を防げる。
    setLoading(true);
    setError('');
    setSuccessMessage('');
    setPredictedQuantity(null);
    setStatusMessage('予測リクエストを送信しています...');

    try {
      // fetch・request body 組み立て・response.ok チェックは forecastApi.js に委譲。
      // App.jsx はここで「何が返ってきたか」だけを見ればよい。
      const json = await postForecast(form);

      // Day 6 以降は { success: true, data: { predictedQuantity: ... } } 形式。
      const quantity = json.data ? json.data.predictedQuantity : json.predictedQuantity;
      setPredictedQuantity(quantity);

      // 予測と履歴更新の両方が完了したことを1メッセージで伝える。
      // 2つのメッセージに分けると視線が分散するため、まとめて1行にしている。
      setSuccessMessage('予測が完了しました。履歴も更新済みです。');

      // 最新の予測を先頭で見せるため page 0 に戻してリロードする。
      loadHistory(historyFilter, 0);
    } catch (err) {
      // TypeError は fetch 自体の失敗（サーバー未起動・ネットワーク切断）を示す。
      // それ以外は forecastApi.js が throw した Error なので message をそのまま使う。
      if (err.name === 'TypeError') {
        setError('API サーバーに接続できませんでした。Spring Boot（ポート 8080）が起動しているか確認してください。');
      } else {
        setError(err.message);
      }
    } finally {
      // 成功・失敗にかかわらず、ローディング状態とステータスメッセージを解除する。
      setLoading(false);
      setStatusMessage('');
    }
  };

  // 検索フォームの入力変更ハンドラ。予測フォームと同じ汎用パターンを使う。
  const handleFilterChange = (e) => {
    setHistoryFilter({ ...historyFilter, [e.target.name]: e.target.value });
  };

  // 「Search」ボタン：フィルターを適用して page 0 から再取得する。
  const handleFilterSearch = (e) => {
    e.preventDefault();
    setIsFiltered(!!(historyFilter.productCategory || historyFilter.date));
    loadHistory(historyFilter, 0);
  };

  // 「Reset」ボタン：フィルターをクリアして page 0 から全件取得する。
  const handleFilterReset = () => {
    const cleared = { productCategory: '', date: '' };
    setHistoryFilter(cleared);
    setIsFiltered(false);
    loadHistory(cleared, 0);
  };

  // ページング操作。現在のフィルターを保持したまま前後ページへ移動する。
  const handleNextPage = () => loadHistory(historyFilter, page + 1);
  const handlePrevPage = () => loadHistory(historyFilter, page - 1);

  return (
    <div className="container">
      <h1>需要予測 AI プラットフォーム</h1>

      {/* ページ全体の用途を一言で伝える。長すぎると読まれないため1文に絞る。 */}
      <p className="page-description">
        入力条件（日付・属性・商品情報）をもとに需要数量を AI で予測し、結果を履歴として確認できるデモです。
      </p>

      {/* ── 予測フォーム ── */}
      <section className="card">
        {/* h2 と説明文を並べることで「何をするセクションか」を即座に伝える */}
        <h2>予測リクエスト</h2>
        <p className="section-description">条件を入力して「Predict」を押すと、ML サービスが需要数量を推論します。</p>
        <form onSubmit={handleSubmit}>

          <div className="form-row">
            <label htmlFor="date">Date</label>
            <input
              id="date"
              type="date"
              name="date"
              value={form.date}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-row">
            <label htmlFor="gender">Gender</label>
            <select
              id="gender"
              name="gender"
              value={form.gender}
              onChange={handleChange}
              required
            >
              <option value="">選択してください</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
            </select>
          </div>

          <div className="form-row">
            <label htmlFor="age">Age</label>
            <input
              id="age"
              type="number"
              name="age"
              value={form.age}
              onChange={handleChange}
              min="1"
              max="120"
              required
            />
          </div>

          <div className="form-row">
            {/* 自由入力だと ML サービスが未知カテゴリでエラーになるため select に変更。
                選択肢はモデルが学習した 3 カテゴリに固定する。 */}
            <label htmlFor="productCategory">Product Category</label>
            <select
              id="productCategory"
              name="productCategory"
              value={form.productCategory}
              onChange={handleChange}
              required
            >
              <option value="">選択してください</option>
              <option value="Beauty">Beauty</option>
              <option value="Clothing">Clothing</option>
              <option value="Electronics">Electronics</option>
            </select>
          </div>

          <div className="form-row">
            <label htmlFor="pricePerUnit">Price per Unit</label>
            <input
              id="pricePerUnit"
              type="number"
              name="pricePerUnit"
              value={form.pricePerUnit}
              onChange={handleChange}
              min="0.01"
              step="0.01"
              required
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? '予測中...' : 'Predict'}
          </button>
        </form>

        {/* ローディング中のステータスメッセージ。finally で消えるため残り続けることはない。 */}
        {statusMessage && <p className="status-message">{statusMessage}</p>}

        {/* エラーメッセージ。成功時はクリアされるため、エラーと成功が同時に出ることはない。 */}
        {error && <p className="error">{error}</p>}

        {/* 予測結果エリア。predictedQuantity が null でない場合のみ表示する。 */}
        {predictedQuantity !== null && (
          <div className="result">
            {/* 成功メッセージを結果の直上に置くことで、数値と文脈を一緒に伝える。 */}
            {successMessage && <p className="success-message">{successMessage}</p>}
            {/* 数値だけでは意味が伝わらないため、何の数値かを一言添える */}
            <p className="result-context">現在の入力条件に基づく推論結果</p>
            <div className="result-number">
              <span className="result-label">予測数量：</span>
              {/* 小数は業務上意味がないため、表示レイヤーのみ Math.round で整数に丸める。
                  内部値・API・DB は変更しない。 */}
              <span className="result-value">{Math.round(predictedQuantity)}</span>
            </div>
          </div>
        )}
      </section>

      {/* ── 予測履歴 ── */}
      <section className="card">
        <h2>予測履歴</h2>
        {/* 履歴が「自動保存されるもの」であることを伝えることで、ユーザーが能動的に操作する必要がないと分かる */}
        <p className="section-description">予測を実行するたびに結果が自動保存されます。過去のリクエストをここで確認できます。</p>

        {/* ── 絞り込みフォーム ── */}
        {/* 複雑な動的検索は不要なため、ボタン押下時のみ API を呼ぶシンプルな実装にした。 */}
        <form className="filter-form" onSubmit={handleFilterSearch}>
          <select
            name="productCategory"
            value={historyFilter.productCategory}
            onChange={handleFilterChange}
          >
            <option value="">All Categories</option>
            <option value="Beauty">Beauty</option>
            <option value="Clothing">Clothing</option>
            <option value="Electronics">Electronics</option>
          </select>
          <input
            type="date"
            name="date"
            value={historyFilter.date}
            onChange={handleFilterChange}
          />
          <button type="submit">Search</button>
          <button type="button" onClick={handleFilterReset}>Reset</button>
        </form>

        {history.length === 0 ? (
          // フィルター適用中か否かでメッセージを出し分ける。
          <p className="empty">
            {isFiltered ? 'No records found for the current filter.' : '履歴がありません。'}
          </p>
        ) : (
          <>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Date</th>
                    <th>Gender</th>
                    <th>Age</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Predicted Qty</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>{item.date}</td>
                      <td>{item.gender}</td>
                      <td>{item.age}</td>
                      <td>{item.productCategory}</td>
                      <td>{item.pricePerUnit}</td>
                      <td>{item.predictedQuantity?.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {/* ページネーションコントロール */}
            <div className="pagination">
              <button onClick={handlePrevPage} disabled={!hasPrevious}>Previous</button>
              <span>Page {page + 1} of {totalPages}</span>
              <button onClick={handleNextPage} disabled={!hasNext}>Next</button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}

export default App;
