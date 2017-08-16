# Kerberos proxy authentication example

This is the demonstration of authentication against kerberos-enabled proxy
with Java's Krb5LoginModule.

## Prerequisites

### Kerberos

You need to have running kerberos authentication server.
If you don't want to install your own Kerberos and just need something
quick to try then you can use [demo freeIPA server](https://ipa.demo1.freeipa.org).


### Proxy server

You need to have a proxy with kerberos authentication in place.

One such proxy is Squid - see [Proxy Authentication](http://wiki.squid-cache.org/Features/Authentication)
for more details

## Configuration

Update username and password in `KerberosCallBackHandler`
and proxy host and/port in `KerberosAuthExample`.

If appropriate, you can also update configuration in `login.conf` file.


## Running

Just run `KerberosAuthExample` main method.
If everything works, you should see content of example.com in console.


## proxy-vole

There's a great [proxy-vole](https://github.com/MarkusBernhardt/proxy-vole) library
which can be used for proxy configuration detection.

If you want, you can use it to detect system's proxy settings and use
system-wide proxy instead of hard-coded one in `KerberosAuthExample` - just
use appropriate proxy selector to retrieve proper proxy settings

```
// for java HTTP stuff
ProxySelector.setDefault(proxySearch.getProxySelector());

// for HTTP client, you have to set proper proxy router planner
...
```