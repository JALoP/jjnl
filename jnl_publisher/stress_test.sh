#!/bin/bash
pwd_saved=$(pwd)
script_dir=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "${pwd_saved}"

PAYLOAD_SIZE=$1
echo "Creating payload of size: $PAYLOAD_SIZE kb"
dd if=/dev/zero of="${script_dir}/payload.txt" count=1024 bs=$PAYLOAD_SIZE

echo "Creating JAL record from generated payload"
cp -f "${script_dir}/sys_and_app_metadata.txt" "${script_dir}/jal_record.txt"
cat "${script_dir}/payload.txt" >> "${script_dir}/jal_record.txt"
echo "BREAK" >> "${script_dir}/jal_record.txt"

echo "Launching stress test publisher with generated jal record and payload size."
cd $script_dir

PAYLOAD_SIZE=$(( 1024 * PAYLOAD_SIZE ))
./a.out $PAYLOAD_SIZE