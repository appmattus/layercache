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

package com.appmattus.layercache.samples.battery

import android.os.Parcel
import android.os.Parcelable
import com.appmattus.battery.ChargingStatus
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<ChargingStatus, ChargingStatusParceler>()
data class BatteryState(
    val batteryLevel: Int = -1,
    val chargingStatus: ChargingStatus = ChargingStatus(ChargingStatus.Status.Unavailable)
) : Parcelable

private object ChargingStatusParceler : Parceler<ChargingStatus> {
    override fun create(parcel: Parcel) = ChargingStatus(ChargingStatus.Status.values()[parcel.readInt()])

    override fun ChargingStatus.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
    }
}
