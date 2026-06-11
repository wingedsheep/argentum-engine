package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.DonatelloGadgetMaster
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Donatello, Gadget Master (TMT #35) — "Whenever Donatello deals combat damage
 * to a player, create a token that's a copy of target artifact you control."
 */
class DonatelloGadgetMasterTest : FunSpec({

    val gadget = card("Test Gadget") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
    }

    test("combat damage to a player makes a token copy of a target artifact you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DonatelloGadgetMaster, gadget))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val don = driver.putCreatureOnBattlefield(player, "Donatello, Gadget Master")
        driver.removeSummoningSickness(don)
        driver.putCreatureOnBattlefield(player, "Test Gadget")

        fun gadgets() = driver.getPermanents(player).count {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Test Gadget"
        }
        gadgets() shouldBe 1

        val artifact = driver.getPermanents(player).first {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Test Gadget"
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(don), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, emptyMap())
        // Combat damage fires the trigger, which pauses to choose the artifact to copy.
        var guard = 0
        while (driver.state.step != Step.POSTCOMBAT_MAIN && guard++ < 40) {
            val holder = driver.state.priorityPlayerId
            if (driver.pendingDecision is com.wingedsheep.engine.core.ChooseTargetsDecision) {
                driver.submitTargetSelection(player, listOf(artifact))
            } else if (driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else if (holder != null) {
                driver.passPriority(holder)
            } else {
                break
            }
        }

        gadgets() shouldBe 2 // original + token copy
    }
})
