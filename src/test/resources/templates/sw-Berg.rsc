
/system identity set name="Station Berg"

/user set admin password="HelloPw"

# all interfaces in routing mode
/interface ethernet
	set [ find mac-address=B2:8E:04:FA:70:90 ] master-port=none speed=1Gbps name=gateway-cyberlink
	set [ find mac-address=B2:8E:04:FA:70:91 ] master-port=none speed=1Gbps name=customer
	set [ find mac-address=B2:8E:04:FA:70:92 ] master-port=none speed=1Gbps name=station-connection-1
	set [ find mac-address=B2:8E:04:FA:70:93 ] master-port=none speed=1Gbps name=station-connection-2
	set [ find mac-address=B2:8E:04:FA:70:94 ] master-port=none speed=1Gbps name=station-connection-3
/interface vlan
	remove numbers=[find]

/interface ipipv6
#	remove numbers=[find]
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1500 name=tunnel-Chalchegg remote-address=fd7e:907d:34ab:0:0:0:0:1
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1500 name=tunnel-Fäsigrund remote-address=fd7e:907d:34ab:0:0:0:0:3
	add local-address=fd7e:907d:34ab:0:0:0:0:0 mtu=1500 name=tunnel-Susanne remote-address=fd7e:907d:34ab:0:0:0:0:2


/interface bridge
	print
#	remove numbers=[find]
	add name=loopback auto-mac=no admin-mac=01:00:00:00:01:00
	
/interface pppoe-client
	add add-default-route=yes dial-on-demand=yes disabled=no interface=gateway-cyberlink name=cyberlink password=pppoe-password user=pppoe-user

/ip address	
#	remove numbers=[find]
# Loopback IP
	add address=172.16.0.1/32 interface=loopback network=172.16.0.1
	
# configured IPs
	add address=172.30.30.2/30 interface=gateway-cyberlink
	add address=172.17.0.1/24 interface=customer
	add address=172.16.1.33/29 interface=station-connection-1
	add address=172.16.1.41/29 interface=station-connection-2
	add address=172.16.1.49/29 interface=station-connection-3

# Tunnel Endpoints
	add address=172.16.4.1/30 interface=tunnel-Chalchegg
	add address=172.16.4.14/30 interface=tunnel-Fäsigrund
	add address=172.16.4.18/30 interface=tunnel-Susanne

# ipv4 ospf Routing
/routing ospf instance set [ find default=yes ] router-id=172.16.0.1

/routing ospf interface
	remove numbers=[find dynamic=no]
	add interface=tunnel-Chalchegg network-type=point-to-point
	add interface=tunnel-Fäsigrund network-type=point-to-point
	add interface=tunnel-Susanne network-type=point-to-point
	add interface=gateway-cyberlink passive=yes
	add interface=customer passive=yes
	add interface=station-connection-1 passive=yes
	add interface=station-connection-2 passive=yes
	add interface=station-connection-3 passive=yes

/routing ospf network
	remove numbers=[find dynamic=no]
	add area=backbone network=172.16.0.1/32
	add area=backbone network=172.30.30.0/30
	add area=backbone network=172.17.0.0/24
	add area=backbone network=172.16.1.32/29
	add area=backbone network=172.16.1.40/29
	add area=backbone network=172.16.1.48/29
	add area=backbone network=172.16.4.0/30
	add area=backbone network=172.16.4.12/30
	add area=backbone network=172.16.4.16/30


# ipv4 dhcp

/ip pool
	remove numbers=[find]
	add name=customer_pool ranges=172.17.0.20-172.17.0.100
	add name=station-connection-1_pool ranges=172.16.1.34-172.16.1.38
	add name=station-connection-2_pool ranges=172.16.1.42-172.16.1.46
	add name=station-connection-3_pool ranges=172.16.1.50-172.16.1.54
/ip dhcp-server
	remove numbers=[find]
	add address-pool=customer_pool disabled=no interface=customer lease-time=30m name=dhcp_customer
	add address-pool=station-connection-1_pool disabled=no interface=station-connection-1 lease-time=10m name=dhcp_station-connection-1
	add address-pool=station-connection-2_pool disabled=no interface=station-connection-2 lease-time=10m name=dhcp_station-connection-2
	add address-pool=station-connection-3_pool disabled=no interface=station-connection-3 lease-time=10m name=dhcp_station-connection-3
/ip dhcp-server network	
	remove numbers=[find]
	add address=172.17.0.0/24 gateway=172.17.0.1
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
	add action=reject chain=input in-interface=cyberlink reject-with=icmp-admin-prohibited
	add action=reject chain=forward in-interface=customer reject-with=icmp-admin-prohibited src-address=!172.17.0.0/24
	add action=reject chain=input in-interface=customer reject-with=icmp-admin-prohibited src-address=!172.17.0.0/24
/ip firewall nat
	remove numbers=[find]
	add action=masquerade chain=srcnat out-interface=cyberlink src-address=172.16.0.0/12


/ipv6 address
	remove numbers=[find]
# Loopback IP
	add address=fd7e:907d:34ab:0:0:0:0:0/128 interface=loopback advertise=no

# Connection IPs
	add address=2001:1620:bba:0:0:0:0:0/64 interface=customer
	add address=fd7e:907d:34ab:104:0:0:0:0/64 interface=station-connection-1
	add address=fd7e:907d:34ab:105:0:0:0:0/64 interface=station-connection-2
	add address=fd7e:907d:34ab:106:0:0:0:0/64 interface=station-connection-3
 
# ospf-v3 Routing 
 
/routing ospf-v3 instance set [ find default=yes ] router-id=172.16.0.1
/routing ospf-v3 interface
	remove numbers=[find]
# Loopback
	add area=backbone interface=loopback passive=yes
	
# Physically Connections
	add area=backbone interface=customer passive=yes
	add area=backbone interface=station-connection-1
	add area=backbone interface=station-connection-2
	add area=backbone interface=station-connection-3

/ipv6 nd
	remove numbers=[find default=no]
	set [ find default=yes ] advertise-dns=yes interface=customer
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
	add action=reject chain=forward in-interface=cyberlink reject-with=icmp-admin-prohibited
	add action=reject chain=input in-interface=cyberlink reject-with=icmp-admin-prohibited
	add action=reject chain=forward in-interface=customer reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:0:0:0:0:0/64
	add action=reject chain=input in-interface=customer reject-with=icmp-admin-prohibited src-address=!2001:1620:bba:0:0:0:0:0/64

/ip dns
set servers=2001:4860:4860:0:0:0:0:8888,8.8.8.8

/system reboot
