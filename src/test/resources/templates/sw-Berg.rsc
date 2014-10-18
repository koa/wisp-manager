
/system identity set name="Station Berg"

/user set admin password="HelloPw"

# all interfaces in routing mode
/interface ethernet
	set [ find mac-address=B2:8E:04:FA:70:90 ] master-port=none speed=1Gbps name=cyberlink
	set [ find mac-address=B2:8E:04:FA:70:91 ] master-port=none speed=1Gbps name=customer-1
	set [ find mac-address=B2:8E:04:FA:70:92 ] master-port=none speed=1Gbps name=station-connection-1
	set [ find mac-address=B2:8E:04:FA:70:93 ] master-port=none speed=1Gbps name=station-connection-2
	set [ find mac-address=B2:8E:04:FA:70:94 ] master-port=none speed=1Gbps name=station-connection-3
/interface vlan
	remove numbers=[find]
	add interface=customer-1 l2mtu=1594 name=customer-1-1 vlan-id=1
	add interface=customer-1 l2mtu=1594 name=customer-1-20 vlan-id=20

/interface ipipv6
#	remove numbers=[find]
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1450 name=tunnel-chalchegg remote-address=fd7e:907d:34ab:0:0:0:0:1
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1450 name=tunnel-susanne remote-address=fd7e:907d:34ab:0:0:0:0:2
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1450 name=tunnel-faesigrund remote-address=fd7e:907d:34ab:0:0:0:0:3


/interface bridge
	print
#	remove numbers=[find]
	add name=loopback auto-mac=no admin-mac=01:00:00:00:01:00
	
/interface pppoe-client
	add add-default-route=yes dial-on-demand=yes disabled=no interface=cyberlink name=cyberlink1 password=pppoe-password user=pppoe-user

/ip address	
#	remove numbers=[find]
# Loopback IP
	add address=172.16.0.1/32 interface=loopback network=172.16.0.1
	
# configured IPs
	add address=172.28.0.2/30 interface=cyberlink
	add address=10.14.10.1/16 interface=customer-1-1
	add address=172.30.30.2/16 interface=customer-1-20
	add address=172.16.1.1/29 interface=station-connection-1
	add address=172.16.1.9/29 interface=station-connection-2
	add address=172.16.1.17/29 interface=station-connection-3

# Tunnel Endpoints
	add address=172.16.4.2/30 interface=tunnel-chalchegg
	add address=172.16.4.6/30 interface=tunnel-susanne
	add address=172.16.4.10/30 interface=tunnel-faesigrund

# ipv4 ospf Routing
/routing ospf instance set [ find default=yes ] router-id=172.16.0.1

/routing ospf interface
	remove numbers=[find dynamic=no]
	add interface=tunnel-chalchegg network-type=point-to-point
	add interface=tunnel-susanne network-type=point-to-point
	add interface=tunnel-faesigrund network-type=point-to-point
	add interface=cyberlink passive=yes
	add interface=customer-1-1 passive=yes
	add interface=customer-1-20 passive=yes
	add interface=station-connection-1 passive=yes
	add interface=station-connection-2 passive=yes
	add interface=station-connection-3 passive=yes

/routing ospf network
	remove numbers=[find dynamic=no]
	add area=backbone network=172.16.0.1/32
	add area=backbone network=172.28.0.0/30
	add area=backbone network=10.14.0.0/16
	add area=backbone network=172.30.0.0/16
	add area=backbone network=172.16.1.0/29
	add area=backbone network=172.16.1.8/29
	add area=backbone network=172.16.1.16/29
	add area=backbone network=172.16.4.0/30
	add area=backbone network=172.16.4.4/30
	add area=backbone network=172.16.4.8/30


# ipv4 dhcp

/ip pool
	remove numbers=[find]
	add name=customer-1-1_pool ranges=10.14.60.0-10.14.60.255
	add name=customer-1-20_pool ranges=172.30.60.0-172.30.60.255
	add name=station-connection-1_pool ranges=172.16.1.2-172.16.1.6
	add name=station-connection-2_pool ranges=172.16.1.10-172.16.1.14
	add name=station-connection-3_pool ranges=172.16.1.18-172.16.1.22
/ip dhcp-server
	remove numbers=[find]
	add address-pool=customer-1-1_pool disabled=no interface=customer-1-1 lease-time=20m name=dhcp_customer-1-1
	add address-pool=customer-1-20_pool disabled=no interface=customer-1-20 lease-time=20m name=dhcp_customer-1-20
	add address-pool=station-connection-1_pool disabled=no interface=station-connection-1 lease-time=10m name=dhcp_station-connection-1
	add address-pool=station-connection-2_pool disabled=no interface=station-connection-2 lease-time=10m name=dhcp_station-connection-2
	add address-pool=station-connection-3_pool disabled=no interface=station-connection-3 lease-time=10m name=dhcp_station-connection-3
/ip dhcp-server network	
	remove numbers=[find]
	add address=10.14.0.0/16 domain=yourdomain.local gateway=10.14.10.1
	add address=172.30.0.0/16 domain=yourdomain.local gateway=172.30.30.2
	add address=172.16.1.0/29 gateway=172.16.1.1
	add address=172.16.1.8/29 gateway=172.16.1.9
	add address=172.16.1.16/29 gateway=172.16.1.17

/ip dhcp-client
	remove numbers=[find]

# ipv4 firewall

/ip firewall filter
	remove numbers=[find]
	add chain=forward connection-state=established
	add chain=forward connection-state=related
	add chain=forward protocol=icmp
	add chain=input connection-state=established
	add chain=input connection-state=related
	add chain=input protocol=icmp
	add action=reject chain=input in-interface=cyberlink1 reject-with=icmp-admin-prohibited
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!10.14.0.0/16
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!10.14.0.0/16
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.30.0.0/16
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.30.0.0/16
/ip firewall nat
	remove numbers=[find]
	add action=masquerade chain=srcnat dst-address=172.28.0.0/30
	add action=masquerade chain=srcnat out-interface=cyberlink1 src-address=172.16.0.0/12
	add action=masquerade chain=srcnat out-interface=cyberlink1 src-address=10.14.0.0/16
	add action=dst-nat chain=dstnat in-interface=cyberlink1 dst-port=80 protocol=tcp to-addresses=10.14.50.31 to-ports=80
	add action=dst-nat chain=dstnat in-interface=cyberlink1 dst-port=443 protocol=tcp to-addresses=10.14.50.31 to-ports=443
	add action=dst-nat chain=dstnat in-interface=cyberlink1 dst-port=22 protocol=tcp to-addresses=10.14.50.29 to-ports=22


/ipv6 address
	remove numbers=[find]
# Loopback IP
	add address=fd7e:907d:34ab:0:0:0:0:0/128 interface=loopback advertise=no

# Connection IPs
	add address=2001:1620:bba:0:0:0:10:1/64 interface=customer-1-1
	add address=2001:1620:bba:1:0:0:0:0/64 interface=customer-1-20
	add address=fd7e:907d:34ab:100:0:0:0:0/64 interface=station-connection-1
	add address=fd7e:907d:34ab:101:0:0:0:0/64 interface=station-connection-2
	add address=fd7e:907d:34ab:102:0:0:0:0/64 interface=station-connection-3
 
# ospf-v3 Routing 
 
/routing ospf-v3 instance set [ find default=yes ] router-id=172.16.0.1
/routing ospf-v3 interface
	remove numbers=[find]
# Loopback
	add area=backbone interface=loopback passive=yes
	
# Physically Connections
	add area=backbone interface=customer-1-1 passive=yes
	add area=backbone interface=customer-1-20 passive=yes
	add area=backbone interface=station-connection-1
	add area=backbone interface=station-connection-2
	add area=backbone interface=station-connection-3

/ipv6 nd
	remove numbers=[find default=no]
	set [ find default=yes ] advertise-dns=yes interface=customer-1-1
	add interface=customer-1-20 advertise-dns=yes
	add interface=station-connection-1 advertise-dns=yes
	add interface=station-connection-2 advertise-dns=yes
	add interface=station-connection-3 advertise-dns=yes

# IPv6 Firewall
/ipv6 firewall filter
	remove numbers=[find]
	add chain=forward connection-state=established
	add chain=forward connection-state=related
	add chain=forward protocol=icmpv6
	add chain=input connection-state=established
	add chain=input connection-state=related
	add chain=input protocol=icmpv6
	add chain=forward dst-address=2001:1620:bba:0:0:0:50:31/128 dst-port=80 protocol=tcp
	add chain=forward dst-address=2001:1620:bba:0:0:0:50:31/128 dst-port=443 protocol=tcp
	add chain=forward dst-address=2001:1620:bba:0:0:0:50:29/128 dst-port=22 protocol=tcp
	add action=reject chain=forward in-interface=cyberlink1 reject-with=icmp-admin-prohibited
	add action=reject chain=input in-interface=cyberlink1 reject-with=icmp-admin-prohibited
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:0:0:0:0:0/64
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:0:0:0:0:0/64
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:1:0:0:0:0/64
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:1:0:0:0:0/64

/ip dns
set allow-remote-requests=yes servers=192.168.1.50,192.168.1.51

/system reboot
