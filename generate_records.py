#!/usr/bin/python

# Generate input files for use by a jjnl publisher

import sys
import os
import shutil
import datetime
import uuid

sys_metadata_template="""<?xml version="1.0" encoding="UTF-8" standalone="no"?><JALRecord xmlns="http://www.dod.mil/jalop-1.0/systemMetadata" JID="UUID-tmpId"><JALDataType>tmpType</JALDataType><RecordID>tmpId</RecordID><Hostname>test.jalop.com</Hostname><HostUUID>34c90268-57ba-4d4c-a602-bdb30251ec77</HostUUID><Timestamp>tmpTimestamp</Timestamp><ProcessID>0</ProcessID><User name="root">0</User><SecurityLabel>unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023</SecurityLabel><ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:SignedInfo>
<ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments"/>
<ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
<ds:Reference URI="#xpointer(id('UUID-tmpId'))">
<ds:Transforms>
<ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
</ds:Transforms>
<ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
<ds:DigestValue>ZRSVbNJrTLcAtsEIi2FAD0ksZcJ3ZS3sC8rnRUIhvhA=</ds:DigestValue>
</ds:Reference>
</ds:SignedInfo>
<ds:SignatureValue>mg9UoZ2g6JxSkfkIHMkPDihFqoPAzmppOGTTKPyPHwSUqhFHj+B0yMvPfzb4+g4+
O4hrbaU+AOiqTeYLDxMX3g==</ds:SignatureValue>
<ds:KeyInfo>
<ds:KeyValue>
<ds:RSAKeyValue>
<ds:Modulus>3PRI+qegjHCd70xtRMPzknUDqY6iH93XJwfuGqXguiEB8n3dxaZu1ZNzMe1BHpGje2RPaRr5EXBK
AXMPnw6MXQ==</ds:Modulus>
<ds:Exponent>AQAB</ds:Exponent>
</ds:RSAKeyValue>
</ds:KeyValue>
<ds:X509Data>
<ds:X509SubjectName>C=US, ST=MD, L=Columbia, CN=www.tresys.com</ds:X509SubjectName>
<ds:X509IssuerSerial>
<ds:X509IssuerName>C=US, ST=MD, L=Columbia, CN=www.tresys.com</ds:X509IssuerName>
<ds:X509SerialNumber>17415892367561384562</ds:X509SerialNumber>
</ds:X509IssuerSerial>
<ds:X509Certificate>MIICszCCAhygAwIBAgIJAPGxrdW+7uJyMA0GCSqGSIb3DQEBBQUAMEYxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJNRDERMA8GA1UEBxMIQ29sdW1iaWExFzAVBgNVBAMTDnd3dy50cmVzeXMuY29tMB4XDTExMDcwNzE3Mjc0MloXDTIxMDcwNDE3Mjc0MlowRjELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAk1EMREwDwYDVQQHEwhDb2x1bWJpYTEXMBUGA1UEAxMOd3d3LnRyZXN5cy5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALOzU51HAVI0n3TKPlwxAaRLWvjgswX+CeTYPO0J2PFky16SQ5FYadlsogOZK/9faE4v2pnURersHrzNNfLBljAA+AOaVH+zdAd61ynC/5gWnM53ME3gRliNsa5xKvsz6X9Kce3wEQlTOFh9Z3jVBEo1Pmq+ANK7CQG8ghfuo5A1AgMBAAGjgagwgaUwHQYDVR0OBBYEFLbpMRFjFtaJ+bLk9T4gzlI1yNpOMHYGA1UdIwRvMG2AFLbpMRFjFtaJ+bLk9T4gzlI1yNpOoUqkSDBGMQswCQYDVQQGEwJVUzELMAkGA1UECBMCTUQxETAPBgNVBAcTCENvbHVtYmlhMRcwFQYDVQQDEw53d3cudHJlc3lzLmNvbYIJAPGxrdW+7uJyMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEAVl9/nYw9NqmCqbnHnvyGGTHsOGB/JeIGO5ffg/KuuNwYuYFh6ddOPSs7BeoFDtg6qWZ9FR8uKyW4xctz+gK2ZXD8F/E2zMPZTSGx/gJ+hJAQ+/xJpZKhu9x2hiZRan0VecDhfZ49F2Ki/K2Oi8x3uvtb7m+FOnu1vON1fFPDAAs=</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</ds:Signature><Manifest xmlns="http://www.w3.org/2000/09/xmldsig#"><Reference URI="jalop:payload"><DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/><DigestValue>44yEl95X8QDgaK8LTVrc3FUKBSsfQW0UkxeLdv11dDQ=</DigestValue></Reference></Manifest></JALRecord>"""

def get_current_timestamp():
	return datetime.datetime.now().isoformat()

def generate_sys_metadata(sysMetaFilePath, recordType):
	global sys_metadata_template

	#Sets the dynamic sys metadata values
	updated_sys_meta = sys_metadata_template.replace("tmpType", recordType)
	updated_sys_meta = updated_sys_meta.replace("tmpTimestamp", str(get_current_timestamp()))

	newUuid = str(uuid.uuid4())
	updated_sys_meta = updated_sys_meta.replace("tmpId", newUuid);

	#Writes sys metadata to file
	with open(sysMetaFilePath, "w") as sys_meta_file:
		sys_meta_file.write(updated_sys_meta)


if (len(sys.argv) != 6):
	print ("Usage: "+sys.argv[0]+" [type] [target directory] [count] [appmeta] [payload]")
	exit(0)

count = int(sys.argv[3])

if (count <=0):
	print ("count must be greater than 0")
	exit(0)

if (not os.path.exists(sys.argv[2])):
	print ("target dir must exist")
	exit(0)

if (sys.argv[1] != "log" and sys.argv[1] != "audit" and sys.argv[1] != "journal"):
	print ("Not a valid type")
	exit(0)

if (not os.path.exists(sys.argv[4])):
	print ("App meta does not exist")
	exit(0)
if (not os.path.exists(sys.argv[5])):
	print ("Payload does not exist")
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
	generate_sys_metadata(os.path.join(path,"sys_metadata.xml"), sys.argv[1])
	shutil.copy(sys.argv[4],os.path.join(path,"app_metadata.xml"))
	shutil.copy(sys.argv[5],os.path.join(path,"payload"))

	count-=1
	serialId+=1
