<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class ApiTokenMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $expectedToken = (string) env('POS_API_TOKEN', '');

        // If token is empty, auth is disabled (useful for local dev).
        if ($expectedToken === '') {
            return $next($request);
        }

        $providedToken = $this->extractToken($request);
        if ($providedToken === null || ! hash_equals($expectedToken, $providedToken)) {
            return new JsonResponse([
                'success' => false,
                'error' => 'Unauthorized token',
            ], 401);
        }

        return $next($request);
    }

    private function extractToken(Request $request): ?string
    {
        $authorization = (string) $request->header('Authorization', '');
        if (str_starts_with($authorization, 'Bearer ')) {
            return trim(substr($authorization, 7));
        }

        $headerToken = $request->header('X-API-TOKEN');
        if (is_string($headerToken) && $headerToken !== '') {
            return $headerToken;
        }

        $queryToken = $request->query('token');
        return is_string($queryToken) && $queryToken !== '' ? $queryToken : null;
    }
}
