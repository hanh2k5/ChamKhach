<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Telesales Assistant API Server</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
        .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 32px; max-width: 650px; width: 100%; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5); }
        .badge { display: inline-flex; align-items: center; gap: 6px; background: rgba(34, 197, 94, 0.2); color: #4ade80; padding: 6px 14px; border-radius: 9999px; font-size: 13px; font-weight: 600; margin-bottom: 20px; border: 1px solid rgba(34, 197, 94, 0.3); }
        .badge-dot { width: 8px; height: 8px; background: #22c55e; border-radius: 50%; box-shadow: 0 0 10px #22c55e; }
        h1 { font-size: 24px; font-weight: 700; margin-bottom: 8px; color: #fff; }
        p.subtitle { color: #94a3b8; font-size: 14px; margin-bottom: 24px; line-height: 1.5; }
        .api-box { background: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 16px; margin-bottom: 20px; }
        .api-label { font-size: 12px; color: #94a3b8; font-weight: 600; margin-bottom: 8px; }
        .code { background: #1e293b; padding: 10px 14px; border-radius: 8px; font-family: monospace; font-size: 13px; color: #38bdf8; word-break: break-all; border: 1px solid #334155; }
        .token-box { margin-top: 10px; font-size: 13px; color: #cbd5e1; }
        .footer { margin-top: 24px; text-align: center; font-size: 12px; color: #64748b; }
        .err-box { margin-top: 15px; padding: 12px; background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; border-radius: 8px; color: #fca5a5; font-size: 12px; font-family: monospace; word-break: break-all; max-height: 200px; overflow-y: auto; }
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">
            <span class="badge-dot"></span>
            TELESALES API SERVER
        </div>
        <h1>Telesales Assistant API</h1>
        <p class="subtitle">Máy chủ Backend phục vụ ứng dụng Android Telesales.</p>

        <div class="api-box">
            <div class="api-label">Base URL dùng cho App Android:</div>
            <div class="code">https://chamkhach.onrender.com/api/v1/</div>
            <div class="token-box" style="margin-top: 12px;">
                <span style="color: #94a3b8;">Token truy cập:</span> <code style="background: #1e293b; padding: 2px 6px; border-radius: 4px; color: #f59e0b;">2005</code>
            </div>
        </div>

        @if(isset($exception) && $exception->getMessage())
        <div class="err-box">
            <b>Chi tiết ngoại lệ:</b> {{ $exception->getMessage() }}
        </div>
        @endif

        <div class="footer">
            Telesales Assistant &copy; 2026 &bull; Laravel 13 &bull; Supabase Postgres &bull; Render Cloud
        </div>
    </div>
</body>
</html>
