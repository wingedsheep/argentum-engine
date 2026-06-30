package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Regression for the bug where "Whenever another permanent you control leaves the battlefield"
 * (Suki, Courageous Rescuer) silently missed a leaving **token**.
 *
 * Suki's filter is [GameObjectFilter.Permanent] (carrying [CardPredicate.IsPermanent]). In
 * [TriggerMatcher.matchesZoneChangeTrigger], IsPermanent had no dedicated case and fell into the
 * generic `else` branch, which does `if (cardComponent == null) return false`. A token is swept
 * from the game by CR 704.5s state-based actions in the same pass that puts it in the graveyard,
 * so by trigger-detection time its live CardComponent is gone — the matcher returned false and the
 * trigger never fired. A nontoken permanent's card is still in the graveyard, so it worked; only
 * tokens were affected (exactly the user's "Ally Token died in combat, no new token" report).
 *
 * The fix gives IsPermanent an LKI-safe case that reads the event's last-known type line.
 */
class TriggerMatcherIsPermanentTest : FunSpec({

    // Observer mirroring Suki's trigger: "whenever another permanent you control leaves the
    // battlefield, draw a card." (Effect simplified to a draw — we assert detection, not output.)
    val permanentLeaveWatcher = card("Permanent Leave Watcher") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Enchantment"
        oracleText = "Whenever another permanent you control leaves the battlefield, draw a card."

        spell {}

        triggeredAbility {
            trigger = TriggerSpec(
                event = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Permanent.youControl(),
                    from = Zone.BATTLEFIELD,
                ),
                binding = TriggerBinding.OTHER,
            )
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + permanentLeaveWatcher)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.putPermanentOnBattlefield(driver.player1, "Permanent Leave Watcher")
        return driver
    }

    fun zoneChanges(driver: GameTestDriver, event: ZoneChangeEvent) =
        TriggerDetector(driver.cardRegistry)
            .detectTriggers(driver.state, listOf(event))
            .filter { it.ability.trigger is EventPattern.ZoneChangeEvent }

    test("fires when a TOKEN you control dies — the entity is swept, so LKI must carry the type line") {
        val driver = createDriver()

        // The token has already been removed from the game (CR 704.5s), so it is NOT in state.
        // The matcher must rely on the event's last-known info.
        val sweptToken = EntityId.generate()
        val event = ZoneChangeEvent(
            entityId = sweptToken,
            entityName = "Ally Token",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.GRAVEYARD,
            ownerId = driver.player1,
            lastKnown = EntitySnapshot(
                entityId = sweptToken,
                controllerId = driver.player1,
                typeLine = TypeLine.parse("Creature - Ally"),
                wasToken = true,
            ),
        )

        zoneChanges(driver, event) shouldHaveSize 1
    }

    test("fires when a nontoken permanent you control leaves (baseline — card still in graveyard)") {
        val driver = createDriver()

        val creature = driver.putCardInHand(driver.player1, "Grizzly Bears")
        val event = ZoneChangeEvent(
            entityId = creature,
            entityName = "Grizzly Bears",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.GRAVEYARD,
            ownerId = driver.player1,
            lastKnown = EntitySnapshot(
                entityId = creature,
                controllerId = driver.player1,
                typeLine = TypeLine.parse("Creature - Bear"),
                wasToken = false,
            ),
        )

        zoneChanges(driver, event) shouldHaveSize 1
    }
})
