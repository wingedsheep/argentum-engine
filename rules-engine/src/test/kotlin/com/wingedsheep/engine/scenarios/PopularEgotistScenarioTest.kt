package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.PopularEgotist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Popular Egotist. */
class PopularEgotistScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Popular Egotist — sacrificing a permanent drains a target opponent for 1") {
        val driver = createDriver()
        driver.registerCard(PopularEgotist)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val egotist = driver.putCreatureOnBattlefield(me, "Popular Egotist")
        driver.removeSummoningSickness(egotist)
        // One fodder creature (NOT another Egotist, so only one drain trigger fires) to sacrifice
        // for the activated ability's "Sacrifice another creature or enchantment" cost.
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(fodder)

        val abilityId = PopularEgotist.activatedAbilities.first().id
        driver.giveMana(me, Color.BLACK, 2)

        // Activate {1}{B}, Sacrifice another creature: with one fodder the sacrifice is deterministic.
        // The drain trigger ("whenever you sacrifice a permanent") targets the opponent.
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = egotist,
                abilityId = abilityId,
                targets = emptyList()
            )
        ).isSuccess shouldBe true

        // Resolve until the drain trigger needs a target, then point it at the opponent.
        var guard = 0
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) {
            if (driver.pendingDecision != null) {
                driver.submitTargetSelection(me, listOf(opp))
            } else {
                driver.bothPass()
            }
            if (guard++ > 30) break
        }

        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 21

        // The Egotist itself is now tapped and indestructible (EOT); the fodder is gone.
        driver.state.getBattlefield().contains(fodder) shouldBe false
    }
})
