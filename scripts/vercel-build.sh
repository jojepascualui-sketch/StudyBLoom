#!/usr/bin/env bash
# Vercel build command for StudyBloom.
# Vercel's build image has no Java, so this downloads JDK 21 (about 20 s),
# writes the PUBLIC env vars into .env for Maven, and builds dist/.
set -euo pipefail

JDK_DIR="$PWD/.jdk"

if ! java -version 2>&1 | grep -qE 'version "(2[1-9]|[3-9][0-9])'; then
  if [ ! -x "$JDK_DIR/bin/java" ]; then
    case "$(uname -m)" in
      aarch64|arm64) ARCH=aarch64 ;;
      *) ARCH=x64 ;;
    esac
    echo "Downloading Temurin JDK 21 ($ARCH)..."
    mkdir -p "$JDK_DIR"
    curl -fsSL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/$ARCH/jdk/hotspot/normal/eclipse" \
      | tar -xz -C "$JDK_DIR" --strip-components=1
  fi
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
java -version

# On Vercel, public config comes from Project Settings > Environment Variables.
# Only these four PUBLIC values are written; secret keys are never read here.
if [ -n "${VERCEL:-}" ]; then
  {
    echo "SUPABASE_URL=${SUPABASE_URL:-}"
    echo "SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY:-}"
    echo "STRIPE_PAYMENT_LINK_PRO=${STRIPE_PAYMENT_LINK_PRO:-}"
    echo "STRIPE_PAYMENT_LINK_MAX=${STRIPE_PAYMENT_LINK_MAX:-}"
  } > .env
fi

chmod +x ./mvnw
./mvnw -B -q clean package
echo "Built dist/ ($(du -sh dist | cut -f1))"
