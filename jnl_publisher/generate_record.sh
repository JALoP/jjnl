#!/bin/bash
pwd_saved=$(pwd)
script_dir=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "${pwd_saved}"

PAYLOAD_SIZE=$1
dd if=/dev/zero of="${script_dir}/payload.txt" count=1024 bs=$PAYLOAD_SIZE || exit 1

cp -f "${script_dir}/sys_and_app_metadata.txt" "${script_dir}/jal_record.txt" || exit 1
cat "${script_dir}/payload.txt" >> "${script_dir}/jal_record.txt" || exit 1
echo "BREAK" >> "${script_dir}/jal_record.txt" || exit 1
rm -f "${script_dir}/payload.txt"

cd $script_dir
exit 0
