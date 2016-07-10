#!/bin/sh

# For the original script checkout https://wiki.gentoo.org/wiki/Traffic_shaping

set -e

if [ "$#" -eq 4 ]; then
   ext="$1"
   port="$2"
   ext_up="$3"
   ext_down="$4"
elif [ "$#" -eq 2 ]; then
   ext="$1"
else
   echo -e "Usage\t: $0 <interface> <remote port> <upstream rate limit> <downstream rate limit> # to install limit rates"
   echo -e "Usage\t: $0 <interface>                                                             # to reset previous limit rates"
   echo -e "Example\t: $0 lo 9002 20kbps 50kbps"
   echo -e "Example\t: $0 lo reset"
   exit 1
fi

ext_ingress=ifb0
modprobe ifb

# INGRESS

# Also turn of gro on ALL interfaces 
ethtool -K $ext tso off gso off gro off 

# Clear old queuing disciplines (qdisc) on the interfaces
tc qdisc del dev $ext root            2> /dev/null || true
tc qdisc del dev $ext ingress         2> /dev/null || true
tc qdisc del dev $ext_ingress root    2> /dev/null || true
tc qdisc del dev $ext_ingress ingress 2> /dev/null || true

[[ -z $port || -z ext_up || -z ext_down ]] && exit 0

# Create ingress on external interface
tc qdisc add dev $ext handle ffff: ingress

# if the interace is not up bad things happen
ifconfig $ext_ingress up

# ifconfig $ext_ingress up # if the interace is not up bad things happen

# Forward all ingress traffic to the IFB device
tc filter add dev $ext parent ffff: protocol ip u32 match ip sport $port 0xffff action mirred egress redirect dev $ext_ingress

# Create an EGRESS filter on the IFB device
tc qdisc add dev $ext_ingress root handle 1: htb default 11

# Add root class HTB with rate limiting
tc class add dev $ext_ingress parent 1: classid 1:1 htb rate $ext_down
tc class add dev $ext_ingress parent 1:1 classid 1:11 htb rate $ext_down prio 0 quantum 1514

# Add FQ_CODEL qdisc with ECN support (if you want ecn)
tc qdisc add dev $ext_ingress parent 1:11 fq_codel quantum 300 ecn

# EGRESS

# Add FQ_CODEL to EGRESS on external interface
tc qdisc add dev $ext root handle 1: htb

tc filter add dev $ext protocol ip parent 1: prio 1 u32 match ip dport $port 0xffff flowid 1:11

# Add root class HTB with rate limiting
tc class add dev $ext parent 1: classid 1:1 htb rate $ext_up
tc class add dev $ext parent 1:1 classid 1:11 htb rate $ext_up prio 0 quantum 1514

# Note: You can apply a packet limit here and on ingress if you are memory constrained - e.g
# for low bandwidths and machines with < 64MB of ram, limit 1000 is good, otherwise no point

# Add FQ_CODEL qdisc without ECN support - on egress it's generally better to just drop the packet
# but feel free to enable it if you want.

tc qdisc add dev $ext parent 1:11 fq_codel quantum 300 noecn
