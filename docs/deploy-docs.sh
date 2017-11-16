#!/bin/bash
set -euo pipefail

echo "Preparing docs"

echo "-> Creating venv..."
cd docs
python3 -m venv VENV
VENV/bin/pip install -r requirements.txt

echo "-> Building docs..."
VENV/bin/mkdocs build -v --clean

echo "-> Git config..."
git config user.name "Circle CI"
git config user.email "ci@saltyrtc.org"
git remote add gh-token "https://${GH_TOKEN}@github.com/saltyrtc/saltyrtc-client-java.git"
git fetch gh-token && git fetch gh-token gh-pages:gh-pages

echo "-> Publish docs to Github pages"
VENV/bin/mkdocs gh-deploy -v --clean --remote-name gh-token
