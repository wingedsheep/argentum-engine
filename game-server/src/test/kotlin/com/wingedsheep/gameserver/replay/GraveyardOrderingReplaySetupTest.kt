package com.wingedsheep.gameserver.replay

import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Format
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class GraveyardOrderingReplaySetupTest : FunSpec({
    test("new replays retain owner ordering and old setups retain insertion order") {
        val setup = ReplaySetup(seed = 1L, format = Format.Standard, attackMode = AttackMode.MULTIPLE,
            players = emptyList(), seatRoster = emptyList(), preserveGraveyardOrder = true)
        val encoded = Json.encodeToJsonElement(ReplaySetup.serializer(), setup)
        Json.decodeFromJsonElement(ReplaySetup.serializer(), encoded).preserveGraveyardOrder shouldBe true
        val oldSetup = JsonObject(encoded.jsonObject - "preserveGraveyardOrder")
        Json.decodeFromJsonElement(ReplaySetup.serializer(), oldSetup).preserveGraveyardOrder shouldBe false
    }
})
