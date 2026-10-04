package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.descriptors.elementNames

/**
 * A delta has to carry every field of [ClientGameState] that can change: a client that applies
 * deltas otherwise keeps the stale value until its next full update.
 */
class StateDeltaTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        return driver
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    test("every mutable snapshot field is carried by the delta") {
        // Fields a delta carries in another shape: cards as added/updated/removed, zones as the
        // changed ones, the log as its new entries. The viewer never changes on a connection,
        // and the attack mode is fixed for the whole game; clients retain both from the snapshot.
        val carriedAs = mapOf("cards" to "addedCards", "zones" to "updatedZones", "gameLog" to "newLogEntries")
        val fixed = setOf("viewingPlayerId", "attackMode")
        val deltaFields = StateDelta.serializer().descriptor.elementNames.toSet()

        ClientGameState.serializer().descriptor.elementNames
            .filterNot { it in fixed || (carriedAs[it] ?: it) in deltaFields }
            .shouldBeEmpty()
    }

    test("both engine flags behind the Void condition reach the delta") {
        val driver = createDriver()
        val viewer = driver.player1
        val before = transformer(driver).transform(driver.state, viewer)

        for (changed in listOf(
            driver.state.copy(spellWarpedThisTurn = true),
            driver.state.copy(nonlandPermanentLeftBattlefieldThisTurn = true),
        )) {
            val after = transformer(driver).transform(changed, viewer)
            StateDiffCalculator.computeDelta(before, after).voidActive shouldBe true
            StateDiffCalculator.computeDelta(after, before).voidActive shouldBe false
        }
        StateDiffCalculator.computeDelta(before, before).voidActive shouldBe null
    }

    test("a change to the viewer's yields reaches the delta, including clearing the last one") {
        val driver = createDriver()
        val before = transformer(driver).transform(driver.state, driver.player1)
        val yields = listOf(ClientYield("Soul Warden#ALA-25", "ability_42", "Soul Warden", wholeGame = true))
        val after = before.copy(activeYields = yields)

        StateDiffCalculator.computeDelta(before, after).activeYields shouldBe yields
        StateDiffCalculator.computeDelta(after, before).activeYields shouldBe emptyList()
        StateDiffCalculator.computeDelta(after, after).activeYields shouldBe null
    }
})
