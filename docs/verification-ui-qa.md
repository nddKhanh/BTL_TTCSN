# Email verification UI states

| State | Expected UI | Security expectation |
|---|---|---|
| Registration complete | Explain that a verification email was sent | Never log the user in before verification |
| Valid token | Show success and a login link | Token becomes unusable after the request |
| Expired/used/invalid token | Show the same neutral error | Do not reveal the account state |
| Resend accepted | Always show the generic “if valid” message | Do not reveal whether the email exists |
| Rate limited | Ask the user to retry later | Do not automatically retry |

Manual QA:

1. Register, then confirm the browser moves to `verify-email.html` without writing an access token.
2. Open the email link and verify that a second use reports the neutral invalid-link state.
3. Request resend for both an existing and a missing email and compare the visible response.
4. Repeatedly resend until rate limiting applies and verify that the button becomes usable again.
