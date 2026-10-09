# UnityTranslate Server Relay
This represents the backend relay that allows the standalone UnityTranslate clients to
connect to each other.

## Design Philosophies
- Use end-to-end encryption, always. We do not need to know what is happening.
- Try to focus on using [UPnP](https://www.rapid7.com/blog/post/2020/12/22/upnp-with-a-holiday-cheer/) to connect between users, otherwise fallback to proxy.
  The relay should determine the proxy URL, it should never be made a constant in the codebase.
  If we ever need to migrate to a different URL, or if we want to introduce additional proxy locations,
  it is made easier with this system.
- The relay should do as little as possible, to avoid too much CPU utilization.
- Any connections made to this relay should not be logged except for debugging purposes.
- Use UDP, so we don't have to hold direct connections.
- Connections between users (P2P, proxied) should use QUIC.
