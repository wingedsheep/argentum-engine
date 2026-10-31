package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.FlaxenIntruder
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flaxen Intruder — {G} Creature — Human Berserker 1/2
 * Whenever this creature deals combat damage to a player, you may sacrifice it. When you do,
 * destroy target artifact or enchantment.
 *
 * Pins the CR 603.12 reflexive shape: the sacrifice is optional, the target is chosen only after
 * it, and an Intruder that has left the battlefield before its trigger resolves can't be
 * sacrificed — so there is no prompt and nothing is destroyed.
 */
class FlaxenIntruderScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + FlaxenIntruder)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun attackUnblocked(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.bothPass()
        d.declareNoBlockers(d.player2)
    }

    /** Resolve everything, answering the may with [yes]; returns how many yes/no prompts appeared. */
    fun settle(d: GameTestDriver, yes: Boolean, pick: EntityId? = null): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 30 && (d.state.stack.isNotEmpty() || d.pendingDecision != null ||
                d.state.step != Step.POSTCOMBAT_MAIN)) {
            when (val pd = d.pendingDecision) {
                is YesNoDecision -> { prompts++; d.submitYesNo(pd.playerId, yes).error shouldBe null }
                is ChooseTargetsDecision -> d.submitTargetSelection(pd.playerId, listOf(pick!!)).error shouldBe null
                null -> d.bothPass()
                else -> error("unexpected decision $pd")
            }
        }
        return prompts
    }

    test("sacrificing it destroys the chosen artifact or enchantment") {
        val d = driver()
        val enchantment = d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val intruder = d.putCreatureOnBattlefield(d.player1, FlaxenIntruder.name).also(d::removeSummoningSickness)
        attackUnblocked(d, intruder)

        settle(d, yes = true, pick = enchantment) shouldBe 1

        d.getLifeTotal(d.player2) shouldBe 19
        d.findPermanent(d.player1, FlaxenIntruder.name) shouldBe null
        d.getGraveyardCardNames(d.player1) shouldContain FlaxenIntruder.name
        d.getGraveyardCardNames(d.player2) shouldContain "Test Enchantment"
    }

    test("declining keeps the Intruder and destroys nothing") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val intruder = d.putCreatureOnBattlefield(d.player1, FlaxenIntruder.name).also(d::removeSummoningSickness)
        attackUnblocked(d, intruder)

        settle(d, yes = false) shouldBe 1

        d.findPermanent(d.player1, FlaxenIntruder.name) shouldNotBe null
        d.findPermanent(d.player2, "Test Enchantment") shouldNotBe null
    }

    test("an Intruder that left the battlefield before its trigger resolves can't be sacrificed") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val intruder = d.putCreatureOnBattlefield(d.player1, FlaxenIntruder.name).also(d::removeSummoningSickness)
        attackUnblocked(d, intruder)

        var guard = 0
        while (guard++ < 10 && d.state.stack.isEmpty()) d.bothPass()
        d.state.stack.isEmpty() shouldBe false
        d.replaceState(d.zones.moveToZone(d.state, intruder, Zone.HAND).state)

        settle(d, yes = true) shouldBe 0

        d.state.getHand(d.player1) shouldContain intruder
        d.findPermanent(d.player2, "Test Enchantment") shouldNotBe null
        d.getGraveyardCardNames(d.player2) shouldNotContain "Test Enchantment"
    }
})
