#!/bin/sh

openssl aes-256-cbc -K $encrypted_fc716ecc5668_key -iv $encrypted_fc716ecc5668_iv -in keys.tar.enc -out keys.tar -d
tar xvf keys.tar;
chmod 600 travis-deploy-key;
cp travis-deploy-key ~/.ssh/id_rsa;

git config --global user.email "developer@47deg.com"
git config --global user.name "47Deg (Travis CI)"
git config --global push.default simple
