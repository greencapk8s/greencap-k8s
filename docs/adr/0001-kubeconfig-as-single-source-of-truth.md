# Kubeconfig as single source of truth for cluster access

A Cluster stores only the encrypted kubeconfig — no separate `apiUrl` field. The server URL is derived from the kubeconfig content when needed. This avoids redundancy and the risk of `apiUrl` drifting out of sync with the kubeconfig. The trade-off is that displaying the URL in the UI requires decrypting the kubeconfig, but this is acceptable given that the UI already depends on the decryption pipeline for all cluster operations.
