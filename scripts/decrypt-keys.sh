#!/bin/sh

openssl aes-256-cbc -K $encrypted_86276aecec54_key -iv $encrypted_86276aecec54_iv -in secring.gpg.enc -out secring.gpg -d;
chmod 600 secring.gpg;
eval `ssh-agent -s`;
ssh-add secring.gpg;