/*
 * Copyright 2020 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.layercache.keystore

import sun.security.x509.AlgorithmId
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateValidity
import sun.security.x509.CertificateVersion
import sun.security.x509.CertificateX509Key
import sun.security.x509.X500Name
import sun.security.x509.X509CertImpl
import sun.security.x509.X509CertInfo
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

internal fun KeyPair.toCertificate(): X509Certificate? {
    val from = Date()
    val to = Date(from.time + 365L * 1000L * 24L * 60L * 60L)
    val interval = CertificateValidity(from, to)

    val serialNumber = BigInteger(64, SecureRandom())
    val owner = X500Name("cn=Unknown")
    val sigAlgId = AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid)

    val info = X509CertInfo().apply {
        set(X509CertInfo.VALIDITY, interval)
        set(X509CertInfo.SERIAL_NUMBER, CertificateSerialNumber(serialNumber))
        set(X509CertInfo.SUBJECT, owner)
        set(X509CertInfo.ISSUER, owner)
        set(X509CertInfo.KEY, CertificateX509Key(public))
        set(X509CertInfo.VERSION, CertificateVersion(CertificateVersion.V3))
        set(X509CertInfo.ALGORITHM_ID, CertificateAlgorithmId(sigAlgId))
    }

    return X509CertImpl(info).apply {
        // Sign the cert to identify the algorithm that's used.
        sign(private, "SHA256WithRSA")
    }
}
