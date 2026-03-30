#!/usr/bin/env python3
"""Test script for quantum mock encryptor"""

import requests
import json
import sys

BASE_URL = "http://localhost:8101/scyh-server/v101"


def test_endpoint(name, method, endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    try:
        if method == "GET":
            resp = requests.get(url, timeout=10)
        else:
            resp = requests.post(url, json=data, timeout=30)

        if resp.status_code == 200:
            result = resp.json()
            if result.get("code") == 0:
                print(f"✅ {name}: OK")
                return True
            else:
                print(f"❌ {name}: {result.get('msg', 'error')}")
                return False
        else:
            print(f"❌ {name}: HTTP {resp.status_code}")
            return False
    except Exception as e:
        print(f"❌ {name}: {e}")
        return False


def main():
    print("=" * 50)
    print("Testing Quantum Mock Encryptor")
    print("=" * 50)

    results = []

    # 1. Random
    results.append(test_endpoint("genRandom", "POST", "/genRandom?length=16"))

    # 2. SM4 Encrypt
    results.append(
        test_endpoint(
            "SM4 Encrypt",
            "POST",
            "/symAlgEnc",
            {
                "algorithm": "SM4/ECB/NoPadding",
                "data": "0123456789abcdef0123456789abcdef",
                "keyData": "0123456789abcdef0123456789abcdef",
            },
        )
    )

    # 3. SM3 Hash
    results.append(
        test_endpoint(
            "SM3 Hash", "POST", "/hash", {"algorithm": "SM3", "data": "68656c6c6f"}
        )
    )

    # 4. HMAC
    results.append(
        test_endpoint(
            "HMAC", "POST", "/hmac", {"data": "68656c6c6f", "key": "0123456789abcdef"}
        )
    )

    # 5. SM2 KeyGen
    results.append(test_endpoint("SM2 KeyGen", "POST", "/genEccKeyPair"))

    # 6. Kyber512 KeyGen
    results.append(
        test_endpoint(
            "Kyber512 KeyGen", "POST", "/genPqcKeyPair", {"algorithm": "kyber512"}
        )
    )

    # 7. Dilithium2 KeyGen
    results.append(
        test_endpoint(
            "Dilithium2 KeyGen", "POST", "/genPqcKeyPair", {"algorithm": "dilithium2"}
        )
    )

    print("=" * 50)
    passed = sum(results)
    total = len(results)
    print(f"Results: {passed}/{total} passed")
    print("=" * 50)

    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
