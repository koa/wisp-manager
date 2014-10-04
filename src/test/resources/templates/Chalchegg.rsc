
/system identity set name="Station Chalchegg"

/user set admin password="HelloPw"

# all interfaces in routing mode
/interface ethernet
	set [ find mac-address=B2:8E:04:FA:70:90 ] master-port=none speed=1Gbps name=customer-1
	set [ find mac-address=B2:8E:04:FA:70:91 ] master-port=none speed=1Gbps name=station-connection-1
	set [ find mac-address=B2:8E:04:FA:70:92 ] master-port=none speed=1Gbps name=station-connection-2
	set [ find mac-address=B2:8E:04:FA:70:93 ] master-port=none speed=1Gbps name=station-connection-3
	set [ find mac-address=B2:8E:04:FA:70:94 ] master-port=none speed=1Gbps name=station-connection-4
/interface vlan
	remove numbers=[find]
	add interface=customer-1 l2mtu=1594 name=customer-1-1 vlan-id=1
	add interface=customer-1 l2mtu=1594 name=customer-1-2 vlan-id=2
	add interface=customer-1 l2mtu=1594 name=customer-1-10 vlan-id=10

/interface ipipv6
#	remove numbers=[find]
	add local-address=fd7e:907d:34ab:0:0:0:0:1 mtu=1500 name=tunnel-Berg remote-address=fd7e:907d:34ab:0:0:0:0:0


/interface bridge
	print
#	remove numbers=[find]
	add name=loopback auto-mac=no admin-mac=01:00:00:00:01:00
	
/interface pppoe-client

/ip address	
#	remove numbers=[find]
# Loopback IP
	add address=172.16.0.2/32 interface=loopback network=172.16.0.2
	
# configured IPs
	add address=172.17.0.1/24 interface=customer-1-1
	add address=172.17.1.1/24 interface=customer-1-2
	add address=172.17.2.1/24 interface=customer-1-10
	add address=172.16.1.25/29 interface=station-connection-1
	add address=172.16.1.33/29 interface=station-connection-2
	add address=172.16.1.41/29 interface=station-connection-3
	add address=172.16.1.49/29 interface=station-connection-4

# Tunnel Endpoints
	add address=172.16.4.1/30 interface=tunnel-Berg

# ipv4 ospf Routing
/routing ospf instance set [ find default=yes ] router-id=172.16.0.2

/routing ospf interface
	remove numbers=[find dynamic=no]
	add interface=tunnel-Berg network-type=point-to-point
	add interface=customer-1-1 passive=yes
	add interface=customer-1-2 passive=yes
	add interface=customer-1-10 passive=yes
	add interface=station-connection-1 passive=yes
	add interface=station-connection-2 passive=yes
	add interface=station-connection-3 passive=yes
	add interface=station-connection-4 passive=yes

/routing ospf network
	remove numbers=[find dynamic=no]
	add area=backbone network=172.16.0.2/32
	add area=backbone network=172.17.0.0/24
	add area=backbone network=172.17.1.0/24
	add area=backbone network=172.17.2.0/24
	add area=backbone network=172.16.1.24/29
	add area=backbone network=172.16.1.32/29
	add area=backbone network=172.16.1.40/29
	add area=backbone network=172.16.1.48/29
	add area=backbone network=172.16.4.0/30


# ipv4 dhcp

/ip pool
	remove numbers=[find]
	add name=customer-1-1_pool ranges=172.17.0.20-172.17.0.100
	add name=customer-1-2_pool ranges=172.17.1.20-172.17.1.100
	add name=customer-1-10_pool ranges=172.17.2.20-172.17.2.100
	add name=station-connection-1_pool ranges=172.16.1.26-172.16.1.30
	add name=station-connection-2_pool ranges=172.16.1.34-172.16.1.38
	add name=station-connection-3_pool ranges=172.16.1.42-172.16.1.46
	add name=station-connection-4_pool ranges=172.16.1.50-172.16.1.54
/ip dhcp-server
	remove numbers=[find]
	add address-pool=customer-1-1_pool disabled=no interface=customer-1-1 lease-time=30m name=dhcp_customer-1-1
	add address-pool=customer-1-2_pool disabled=no interface=customer-1-2 lease-time=30m name=dhcp_customer-1-2
	add address-pool=customer-1-10_pool disabled=no interface=customer-1-10 lease-time=30m name=dhcp_customer-1-10
	add address-pool=station-connection-1_pool disabled=no interface=station-connection-1 lease-time=10m name=dhcp_station-connection-1
	add address-pool=station-connection-2_pool disabled=no interface=station-connection-2 lease-time=10m name=dhcp_station-connection-2
	add address-pool=station-connection-3_pool disabled=no interface=station-connection-3 lease-time=10m name=dhcp_station-connection-3
	add address-pool=station-connection-4_pool disabled=no interface=station-connection-4 lease-time=10m name=dhcp_station-connection-4
/ip dhcp-server network	
	remove numbers=[find]
	add address=172.17.0.0/24 gateway=172.17.0.1
	add address=172.17.1.0/24 gateway=172.17.1.1
	add address=172.17.2.0/24 gateway=172.17.2.1
	add address=172.16.1.24/29 gateway=172.16.1.25
	add address=172.16.1.32/29 gateway=172.16.1.33
	add address=172.16.1.40/29 gateway=172.16.1.41
	add address=172.16.1.48/29 gateway=172.16.1.49

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
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.0.0/24
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.0.0/24
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.1.0/24
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.1.0/24
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.2.0/24
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!172.17.2.0/24
/ip firewall nat
	remove numbers=[find]


/ipv6 address
	remove numbers=[find]
# Loopback IP
	add address=fd7e:907d:34ab:0:0:0:0:1/128 interface=loopback advertise=no

# Connection IPs
	add address=2001:1620:bba:2:0:0:0:0/64 interface=customer-1-1
	add address=2001:1620:bba:3:0:0:0:0/64 interface=customer-1-2
	add address=2001:1620:bba:4:0:0:0:0/64 interface=customer-1-10
	add address=fd7e:907d:34ab:103:0:0:0:0/64 interface=station-connection-1
	add address=fd7e:907d:34ab:104:0:0:0:0/64 interface=station-connection-2
	add address=fd7e:907d:34ab:105:0:0:0:0/64 interface=station-connection-3
	add address=fd7e:907d:34ab:106:0:0:0:0/64 interface=station-connection-4
 
# ospf-v3 Routing 
 
/routing ospf-v3 instance set [ find default=yes ] router-id=172.16.0.2
/routing ospf-v3 interface
	remove numbers=[find]
# Loopback
	add area=backbone interface=loopback passive=yes
	
# Physically Connections
	add area=backbone interface=customer-1-1 passive=yes
	add area=backbone interface=customer-1-2 passive=yes
	add area=backbone interface=customer-1-10 passive=yes
	add area=backbone interface=station-connection-1
	add area=backbone interface=station-connection-2
	add area=backbone interface=station-connection-3
	add area=backbone interface=station-connection-4

/ipv6 nd
	remove numbers=[find default=no]
	set [ find default=yes ] advertise-dns=yes interface=customer-1-1
	add interface=customer-1-2 advertise-dns=yes
	add interface=customer-1-10 advertise-dns=yes
	add interface=station-connection-1 advertise-dns=yes
	add interface=station-connection-2 advertise-dns=yes
	add interface=station-connection-3 advertise-dns=yes
	add interface=station-connection-4 advertise-dns=yes

# IPv6 Firewall
/ipv6 firewall filter
	remove numbers=[find]
	add chain=forward connection-state=established
	add chain=forward connection-state=related
	add chain=forward protocol=icmpv6
	add chain=input connection-state=established
	add chain=input connection-state=related
	add chain=input protocol=icmpv6
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:2:0:0:0:0/64
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:2:0:0:0:0/64
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:3:0:0:0:0/64
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:3:0:0:0:0/64
	add action=reject chain=forward in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:4:0:0:0:0/64
	add action=reject chain=input in-interface=customer-1 reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:4:0:0:0:0/64

/ip dns
set servers=2001:4860:4860:0:0:0:0:8888,8.8.8.8

/system reboot
