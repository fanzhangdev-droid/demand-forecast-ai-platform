# frontend

React + TypeScript single-page application for the demand forecast dashboard.

## Tech Stack

- React 18
- TypeScript
- Vite
- React Query (data fetching)
- Recharts (charts)

## Local Development

```bash
npm install
npm run dev        # http://localhost:3000
npm run build      # production build → dist/
npm run lint
npm run test
```

## Environment Variables

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the API gateway |

Copy `.env.example` in the repo root and adjust as needed.
