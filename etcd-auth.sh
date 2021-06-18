#!/bin/sh
./etcdctl user add root
./etcdctl --user=root --password="H&O%T=C9AFF]" user grant-role root root
./etcdctl --user=root --password="H&O%T=C9AFF]" auth enable
./etcdctl --user=root --password="H&O%T=C9AFF]" role add client
./etcdctl --user=root --password="H&O%T=C9AFF]" role add worker
./etcdctl --user=root --password="H&O%T=C9AFF]" user add worker
./etcdctl --user=root --password="H&O%T=C9AFF]" user grant-role worker worker
./etcdctl --user=root --password="H&O%T=C9AFF]" role grant-permission client read /hotcaffeine --prefix=true
./etcdctl --user=root --password="H&O%T=C9AFF]" role grant-permission worker readwrite /hotcaffeine --prefix=true
