/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.crypto

import play.api.{Logger, Play}

trait KeysFromConfigGCM {
  this: CompositeSymmetricCrypto =>

  val baseConfigKey: String

  override protected val currentCrypto = {
    val configKey = baseConfigKey + ".key"
    val currentEncryptionKey = Play.current.configuration.getString(configKey).getOrElse {
      Logger.error(s"Missing required configuration entry: $configKey")
      throw new SecurityException(s"Missing required configuration entry: $configKey")
    }
    aesGCMCrypto(currentEncryptionKey, "")
  }

  override protected val previousCryptos = {
        val configKey = baseConfigKey + ".previousKeys"
        val previousEncryptionKeys = Play.current.configuration.getStringSeq(configKey).getOrElse(Seq.empty)
        previousEncryptionKeys.map(k => aesGCMCrypto(k, ""))
  }

  private def aesGCMCrypto(key: String, additional: String) = {

    // Constructor initialisation - verify crypto before returning handle.
    try {
      val crypto = new AesGCMCrypto {
        override val encryptionKey = key
      }
      val encrypted = crypto.encrypt(PlainText("assert-valid-key"))
      crypto.decrypt(encrypted)
      crypto
    } catch {
      case e: Exception =>
        Logger.error(s"Invalid encryption key: $key", e);
        throw new SecurityException("Invalid encryption key", e)
    }
  }
}

case class CryptoGCMWithKeysFromConfig(baseConfigKey: String) extends CompositeSymmetricCrypto with KeysFromConfigGCM
