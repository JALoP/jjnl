#!/usr/bin/python

# Generate input files for use by a jjnl publisher

import sys
import os
import shutil

if (len(sys.argv) != 7):
	print "Usage: "+sys.argv[0]+" [type] [target directory] [count] [sysmeta] [appmeta] [payload]" 
	exit(0)

count = int(sys.argv[3])

if (count <=0):
	print "count must be greater than 0"
	exit(0)

if (not os.path.exists(sys.argv[2])):
	print "target dir must exist"
	exit(0)

if (sys.argv[1] != "log" and sys.argv[1] != "audit" and sys.argv[1] != "journal"):
	print "Not a valid type"
	exit(0)

if (not os.path.exists(sys.argv[4])):
	print "Sys meta does not exist"
	exit(0)
if (not os.path.exists(sys.argv[5])):
	print "App meta does not exist"
	exit(0)
if (not os.path.exists(sys.argv[6])):
	print "Payload does not exist"
	exit(0)

def serialIdToString(serialId):
	digits = len(str(serialId))
	string = ""
	for i in range(10-digits):
		string+="0"
	string+=str(serialId)
	return string

# Will need to be changed with serialId removal
serialId = 1 # Apparently valid serialIds start at 1

# Fast forward to correct serialId
while (os.path.exists(os.path.join(sys.argv[2],sys.argv[1],serialIdToString(serialId)))):
	serialId+=1


while (count > 0):
	path = os.path.join(sys.argv[2], sys.argv[1], serialIdToString(serialId))
	os.makedirs(path);
	shutil.copy(sys.argv[4],os.path.join(path,"sys_metadata.xml"))
	shutil.copy(sys.argv[5],os.path.join(path,"app_metadata.xml"))
	shutil.copy(sys.argv[6],os.path.join(path,"payload"))

	count-=1
	serialId+=1	
