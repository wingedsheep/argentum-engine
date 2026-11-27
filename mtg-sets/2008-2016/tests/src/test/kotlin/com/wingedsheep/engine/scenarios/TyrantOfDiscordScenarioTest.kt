package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.TyrantOfDiscord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Tyrant of Discord (AVR #162): "When this creature enters, target opponent chooses a permanent
 * they control at random and sacrifices it. If a nonland permanent is sacrificed this way, repeat
 * this process."
 *
 * The pick is random, so each board is built so the outcome is fixed: an all-nonland board is
 * sacrificed until empty, an all-land board loses exactly one land, and a mixed board always ends
 * with its lone land sacrificed (the loop only stops on a land or an empty board). Your own
 * permanents are never touched.
 */
class TyrantOfDiscordScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TyrantOfDiscord))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.castTyrantAndResolve(you: EntityId, opponent: EntityId) {
        giveMana(you, Color.RED, 3)
        giveColorlessMana(you, 4)
        castSpell(you, putCardInHand(you, "Tyrant of Discord")).error shouldBe null
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 30) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision -> submitTargetSelection(you, listOf(opponent)).error shouldBe null
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        state.stack.isEmpty() shouldBe true
    }

    fun GameTestDriver.controlledBy(player: EntityId): List<EntityId> =
        state.getBattlefield().filter { state.projectedState.getController(it) == player }

    test("nonland permanents keep being sacrificed until the opponent has none left") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val theirs = List(3) { driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") }
        val mine = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        driver.castTyrantAndResolve(you, opponent)

        driver.controlledBy(opponent).size shouldBe 0
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)) shouldContainAll theirs
        driver.state.getBattlefield() shouldContain mine
    }

    test("sacrificing a land ends the process") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        repeat(3) { driver.putLandOnBattlefield(opponent, "Forest") }

        driver.castTyrantAndResolve(you, opponent)

        driver.controlledBy(opponent).size shouldBe 2
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).size shouldBe 1
    }

    test("on a mixed board the loop always runs until the land is sacrificed") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val forest = driver.putLandOnBattlefield(opponent, "Forest")
        repeat(3) { driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") }

        driver.castTyrantAndResolve(you, opponent)

        val graveyard = driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD))
        graveyard shouldContain forest
        // The Forest was the last thing sacrificed: every Bears still in play was never picked.
        (graveyard.size + driver.controlledBy(opponent).size) shouldBe 4
    }
})
