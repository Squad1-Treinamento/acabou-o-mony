---
id: vision-000
status: active
links:
  - spec/user-stories/index.md
  - spec/specs/index.md
---
# Vision

Acabou o Mony exists to remove friction from payments in live and
conversational commerce. It prioritizes speed, trust, and reliability
for merchants who cannot afford checkout failures during peak moments.

Primary users and context:
- merchants and platforms running live or chat-driven sales
- customers who must complete purchases without leaving the host flow

Outcomes we optimize for:
- end-to-end payment confirmation in under 1 second at scale
- high availability during traffic spikes and live events
- secure handling of sensitive data with strong authentication
- seamless integration with modern acquirers (e.g., Mercado Pago)

Non-goals and boundaries:
- do not build a proprietary acquiring network or card scheme
- do not store raw PAN data; tokenization only
- do not optimize for offline or in-person payments in the MVP
- do not add UX flows that force users to leave live/chat experiences

Tradeoffs and guiding principles:
- prioritize latency and resilience over feature breadth
- prefer clear, auditable flows over hidden automation
- keep the architecture simple enough to scale horizontally
