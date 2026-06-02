#!/bin/bash
ESC="\x1b"
TL="${ESC}[38;2;124;192;227m"
TR="${ESC}[38;2;243;168;105m"
BL="${ESC}[38;2;21;101;116m"
BR="${ESC}[38;2;15;74;86m"
CL="${ESC}[38;2;19;96;112m"
CR="${ESC}[38;2;32;120;134m"
TITLE="${ESC}[1;37m"
SUB="${ESC}[0;37m"
RST="${ESC}[0m"

echo -e "                      ${TL}/${TR}\\${RST}" > banner.txt
echo -e "                     ${TL}//${TR}\\\\${RST}" >> banner.txt
echo -e "                    ${TL}//  ${TR}\\\\${RST}" >> banner.txt
echo -e "                   ${TL}// ${CL}/\\${TR} \\\\${RST}" >> banner.txt
echo -e "                  ${TL}// ${CL}/  \\${TR} \\\\${RST}" >> banner.txt
echo -e "                 ${TL}// ${CL}/____\\${TR} \\\\${RST}" >> banner.txt
echo -e "                 ${BL}\\\\ ${CR}\\    /${BR} //${RST}" >> banner.txt
echo -e "                  ${BL}\\\\ ${CR}\\  /${BR} //${RST}" >> banner.txt
echo -e "                   ${BL}\\\\ ${CR}\\/${BR} //${RST}" >> banner.txt
echo -e "                    ${BL}\\\\  ${BR}//${RST}" >> banner.txt
echo -e "                     ${BL}\\\\${BR}//${RST}" >> banner.txt
echo -e "                      ${BL}\\${BR}/${RST}" >> banner.txt
echo -e "" >> banner.txt
echo -e "                ${TITLE}T E S S E R A${RST}" >> banner.txt
echo -e "    ${SUB}The foundational tiles of enterprise architecture${RST}" >> banner.txt
echo -e "" >> banner.txt
