/*
 * Copyright 2021 Appmattus Limited
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

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

@Suppress("MagicNumber")
internal fun KeyPair.toCertificate(): X509Certificate? {

    val from = Date()
    val to = Date(from.time + 365L * 1000L * 24L * 60L * 60L)

    val serialNumber = BigInteger(64, SecureRandom())
    val owner = X500Name("cn=Unknown")

    val sigAlgId: AlgorithmIdentifier = DefaultSignatureAlgorithmIdentifierFinder().find("MD5withRSA")
    val digAlgId = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val privateKeyAsymKeyParam = PrivateKeyFactory.createKey(private.encoded)

    val sigGen = BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam)

    val holder = X509v3CertificateBuilder(
        owner,
        serialNumber,
        from,
        to,
        owner,
        SubjectPublicKeyInfo.getInstance(public.encoded)
    ).build(sigGen)

    return JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
}
