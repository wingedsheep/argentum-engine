package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RakdosTheMuscle
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rakdos, the Muscle (OTJ mythic Demon Mercenary), {2}{B}{B}{R}, 6/5.
 *
 * Flying, trample.
 * Whenever you sacrifice another creature, exile cards equal to its mana value from the top of
 * target player's library. Until your next end step, you may play those cards, and mana of any
 * type can be spent to cast those spells.
 * Sacrifice another creature: Rakdos gains indestructible until end of turn. Tap it. Activate only
 * once each turn.
 *
 * Proves the activated ability sacrifices a creature (firing the trigger), grants indestructible,
 * and taps Rakdos; and that the trigger impulse-exiles cards equal to the sacrificed creature's
 * mana value from a target player's library.
 */
class RakdosTheMuscleScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RakdosTheMuscle)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sac ability: Rakdos gains indestructible and taps; trigger exiles MV cards from target player's library") {
        val driver = createDriver()
        val rakdos = driver.putCreatureOnBattlefield(driver.player1, "Rakdos, the Muscle")
        // Centaur Courser is {2}{G} → mana value 3.
        val fodder = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(rakdos)

        val abilityId = driver.cardRegistry.requireCard("Rakdos, the Muscle").activatedAbilities.first().id
        val exileBefore = driver.getExile(driver.player2).size

        // Activate "Sacrifice another creature: ..." sacrificing the Centaur Courser.
        val activation = driver.submit(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = rakdos,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )
        activation.error shouldBe null

        // Resolve the ability and the sacrifice trigger; the trigger pauses to choose a target player.
        var guard = 0
        while (driver.pendingDecision !is ChooseTargetsDecision && guard++ < 20) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
        val decision = driver.pendingDecision as? ChooseTargetsDecision
            ?: error("Expected a ChooseTargetsDecision for the target player (have ${driver.pendingDecision})")
        driver.submitTargetSelection(decision.playerId, listOf(driver.player2))

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // The activated ability resolved: Rakdos gained indestructible and is tapped.
        driver.state.projectedState.hasKeyword(rakdos, Keyword.INDESTRUCTIBLE) shouldBe true
        driver.isTapped(rakdos) shouldBe true

        // 3 cards (Centaur Courser's mana value) were exiled from player2's library.
        (driver.getExile(driver.player2).size - exileBefore) shouldBe 3
    }
})
