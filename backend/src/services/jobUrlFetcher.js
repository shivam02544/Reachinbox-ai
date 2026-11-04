const dns = require('dns').promises;
const net = require('net');
const { URL } = require('url');

const BLOCKED_HOSTNAMES = new Set(['localhost', 'metadata.google.internal']);

const isPrivateIp = (ip) => {
    if (!ip) return true;
    if (net.isIP(ip) === 4) {
        const parts = ip.split('.').map(Number);
        if (parts[0] === 10) return true;
        if (parts[0] === 127) return true;
        if (parts[0] === 0) return true;
        if (parts[0] === 169 && parts[1] === 254) return true;
        if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
        if (parts[0] === 192 && parts[1] === 168) return true;
        return false;
    }
    if (net.isIP(ip) === 6) {
        const normalized = ip.toLowerCase();
        return normalized === '::1'
            || normalized.startsWith('fc')
            || normalized.startsWith('fd')
            || normalized.startsWith('fe80');
    }
    return true;
};

const assertSafeUrl = async (rawUrl) => {
    let parsed;
    try {
        parsed = new URL(rawUrl);
    } catch {
        const error = new Error('Invalid job URL');
        error.statusCode = 400;
        error.code = 'INVALID_JOB_URL';
        throw error;
    }

    if (!['http:', 'https:'].includes(parsed.protocol)) {
        const error = new Error('Only http and https job URLs are supported');
        error.statusCode = 400;
        error.code = 'INVALID_JOB_URL';
        throw error;
    }

    const hostname = parsed.hostname.toLowerCase();
    if (BLOCKED_HOSTNAMES.has(hostname) || hostname.endsWith('.local')) {
        const error = new Error('Job URL host is not allowed');
        error.statusCode = 400;
        error.code = 'UNSAFE_JOB_URL';
        throw error;
    }

    if (net.isIP(hostname)) {
        if (isPrivateIp(hostname)) {
            const error = new Error('Job URL host is not allowed');
            error.statusCode = 400;
            error.code = 'UNSAFE_JOB_URL';
            throw error;
        }
        return parsed;
    }

    let addresses;
    try {
        addresses = await dns.lookup(hostname, { all: true });
    } catch {
        const error = new Error('Could not resolve job URL host');
        error.statusCode = 400;
        error.code = 'UNRESOLVABLE_JOB_URL';
        throw error;
    }

    if (!addresses.length || addresses.some((entry) => isPrivateIp(entry.address))) {
        const error = new Error('Job URL host is not allowed');
        error.statusCode = 400;
        error.code = 'UNSAFE_JOB_URL';
        throw error;
    }

    return parsed;
};

const stripHtml = (html) => {
    return html
        .replace(/<script[\s\S]*?<\/script>/gi, ' ')
        .replace(/<style[\s\S]*?<\/style>/gi, ' ')
        .replace(/<noscript[\s\S]*?<\/noscript>/gi, ' ')
        .replace(/<!--[\s\S]*?-->/g, ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/gi, ' ')
        .replace(/&amp;/gi, '&')
        .replace(/&lt;/gi, '<')
        .replace(/&gt;/gi, '>')
        .replace(/&quot;/gi, '"')
        .replace(/&#39;/gi, "'")
        .replace(/\s+/g, ' ')
        .trim();
};

const fetchJobPageText = async (rawUrl) => {
    const parsed = await assertSafeUrl(rawUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);

    try {
        const response = await fetch(parsed.toString(), {
            method: 'GET',
            redirect: 'follow',
            signal: controller.signal,
            headers: {
                'User-Agent': 'ReachInboxJobFetcher/1.0',
                Accept: 'text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.8'
            }
        });

        if (!response.ok) {
            const error = new Error(`Failed to fetch job page (HTTP ${response.status})`);
            error.statusCode = 400;
            error.code = 'JOB_URL_FETCH_FAILED';
            throw error;
        }

        const contentType = (response.headers.get('content-type') || '').toLowerCase();
        const body = await response.text();
        const text = contentType.includes('text/plain') ? body.trim() : stripHtml(body);

        if (!text || text.length < 40) {
            const error = new Error('Could not extract enough job description text from the URL');
            error.statusCode = 400;
            error.code = 'JOB_URL_EMPTY_CONTENT';
            throw error;
        }

        return text.substring(0, 15000);
    } catch (err) {
        if (err.statusCode) throw err;
        if (err.name === 'AbortError') {
            const error = new Error('Timed out fetching job URL');
            error.statusCode = 400;
            error.code = 'JOB_URL_TIMEOUT';
            throw error;
        }
        const error = new Error(err.message || 'Failed to fetch job URL');
        error.statusCode = 400;
        error.code = 'JOB_URL_FETCH_FAILED';
        throw error;
    } finally {
        clearTimeout(timeout);
    }
};

module.exports = {
    fetchJobPageText,
    assertSafeUrl,
    stripHtml
};
