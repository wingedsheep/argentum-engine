package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.OrochiRanger
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Orochi Ranger (CHK) — "Whenever this creature deals combat damage to a creature, tap that creature and
 * it doesn't untap during its controller's next untap step."
 *
 * The blocker is untapped when damage is dealt (blocking doesn't tap), so its being tapped proves
 * the trigger resolved against the damaged creature; its staying tapped through the opponent's
 * untap step proves the lock is keyed to the *affected* creature's controller.
 *
 * The Ranger is a 2/1, so it usually dies in the same damage step: the trigger still fires, because
 * it looks back at the damage event rather than requiring the Ranger to survive it.
 */
class OrochiRangerScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + OrochiRanger)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("the blocker it damages is tapped and skips its controller's next untap step") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val snake = d.putCreatureOnBattlefield(me, "Orochi Ranger")
        d.removeSummoningSickness(snake)
        val blocker = d.putCreatureOnBattlefield(opp, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(snake), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(snake))).error shouldBe null
        d.isTapped(blocker) shouldBe false

        d.passPriorityUntil(Step.END)
        withClue("the damaged blocker survived and was tapped by the trigger") {
            d.findPermanent(opp, "Force of Nature").shouldNotBeNull()
            d.isTapped(blocker) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        withClue("the blocker's controller's untap step skipped it") {
            d.isTapped(blocker) shouldBe true
        }
    }

    test("combat damage to a player doesn't trigger it") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val snake = d.putCreatureOnBattlefield(me, "Orochi Ranger")
        d.removeSummoningSickness(snake)
        val bystander = d.putCreatureOnBattlefield(opp, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(snake), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.END)

        d.isTapped(bystander) shouldBe false
    }
})
