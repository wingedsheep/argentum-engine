package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * A recorded kicked cast still replays kicked after the `wasKicked` → `declaredCostSlot` rename.
 *
 * Bargain (CR 702.166) put a second mechanic on the optional-additional-cost rail, so `CastSpell`'s
 * boolean became a `ChoiceSlot`. `persistenceJson` ignores unknown keys, so without a migration the
 * legacy key would be dropped and the slot would default to `null` — the cast re-simulates unkicked
 * and the whole rest of the replay is a game nobody played. That failure is silent by construction,
 * which is why it gets a test rather than a comment.
 */
class ReplayLegacyKickerDecodeTest : FunSpec({

    /** A v1-shaped record whose single action carries the pre-rename boolean. */
    fun legacyRecord(wasKicked: Boolean): String {
        val template = CompactReplay(
            gameId = "legacy-game",
            players = listOf(ReplayPlayerInfo("p1", "A"), ReplayPlayerInfo("p2", "B")),
            startedAt = "2024-01-01T00:00:00Z",
            endedAt = "2024-01-01T00:10:00Z",
            winnerName = "A",
            setup = ReplaySetup(
                seed = 7L,
                format = Format.Standard,
                attackMode = AttackMode.MULTIPLE,
                players = listOf(
                    ReplayPlayerSetup(playerId = "p1", name = "A", deck = Deck(cards = listOf("Forest"))),
                    ReplayPlayerSetup(playerId = "p2", name = "B", deck = Deck(cards = listOf("Forest"))),
                ),
                seatRoster = emptyList(),
            ),
            actions = listOf(CastSpell(playerId = EntityId("p1"), cardId = EntityId("e12"))),
        )

        // Round-trip through JSON, then swap the new key back out for the old one — the exact shape
        // a record written before this rename has on disk.
        val root = persistenceJson.encodeToString(CompactReplay.serializer(), template)
            .let { persistenceJson.parseToJsonElement(it).jsonObject }
        val legacyAction = buildJsonObject {
            root["actions"]!!.jsonArray.single().jsonObject
                .filterKeys { it != "declaredCostSlot" }
                .forEach { (k, v) -> put(k, v) }
            put("wasKicked", JsonPrimitive(wasKicked))
        }
        val legacyRoot = JsonObject(root + ("actions" to buildJsonArray { add(legacyAction) }))
        return ReplayCodec.encodeText(
            persistenceJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), legacyRoot)
        )
    }

    test("a legacy wasKicked=true cast decodes as a KICKED declaration") {
        val decoded = ReplayCodec.decode(legacyRecord(wasKicked = true))
        (decoded.actions.single() as CastSpell).declaredCostSlot shouldBe ChoiceSlot.KICKED
    }

    test("a legacy wasKicked=false cast decodes as no declaration") {
        val decoded = ReplayCodec.decode(legacyRecord(wasKicked = false))
        (decoded.actions.single() as CastSpell).declaredCostSlot shouldBe null
    }

    test("a current record round-trips untouched") {
        val replay = CompactReplay(
            gameId = "current-game",
            players = listOf(ReplayPlayerInfo("p1", "A")),
            startedAt = "2024-01-01T00:00:00Z",
            endedAt = "2024-01-01T00:10:00Z",
            winnerName = null,
            setup = ReplaySetup(
                seed = 7L,
                format = Format.Standard,
                attackMode = AttackMode.MULTIPLE,
                players = listOf(
                    ReplayPlayerSetup(playerId = "p1", name = "A", deck = Deck(cards = listOf("Forest"))),
                ),
                seatRoster = emptyList(),
            ),
            actions = listOf(
                CastSpell(playerId = EntityId("p1"), cardId = EntityId("e12"), declaredCostSlot = ChoiceSlot.BARGAINED),
            ),
        )

        val decoded = ReplayCodec.decode(ReplayCodec.encode(replay))
        (decoded.actions.single() as CastSpell).declaredCostSlot shouldBe ChoiceSlot.BARGAINED
    }
})
