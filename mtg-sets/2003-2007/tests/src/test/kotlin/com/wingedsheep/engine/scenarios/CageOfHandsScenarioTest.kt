package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CageOfHands
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cage of Hands (CHK) — "Enchant creature / Enchanted creature can't attack or block. / {1}{W}:
 * Return this Aura to its owner's hand."
 */
class CageOfHandsScenarioTest : FunSpec({

    val bounceId = CageOfHands.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CageOfHands)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("the enchanted creature can't attack") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        d.removeSummoningSickness(lions)
        // A second, free attacker so the combat phase isn't skipped for want of attackers.
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        val cage = d.putCardInHand(p1, "Cage of Hands")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, cage, listOf(lions)).error shouldBe null
        d.bothPass()
        d.findPermanent(p1, "Cage of Hands").shouldNotBeNull()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("a caged creature can't be declared as an attacker") {
            d.declareAttackers(p1, listOf(lions), p2).error shouldNotBe null
        }
        d.declareAttackers(p1, listOf(bears), p2).error shouldBe null
    }

    test("the enchanted creature can't block") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val attacker = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(attacker)
        val blocker = d.putCreatureOnBattlefield(p2, "Savannah Lions")
        val cage = d.putCardInHand(p1, "Cage of Hands")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, cage, listOf(blocker)).error shouldBe null
        d.bothPass()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(attacker), p2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(p2, mapOf(blocker to listOf(attacker))).error shouldNotBe null
    }

    test("{1}{W} returns the Aura to its owner's hand") {
        val d = driver()
        val p1 = d.player1
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        val cage = d.putCardInHand(p1, "Cage of Hands")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, cage, listOf(lions)).error shouldBe null
        d.bothPass()
        val cagePermanent = d.findPermanent(p1, "Cage of Hands").shouldNotBeNull()

        d.giveMana(p1, Color.WHITE, 2)
        d.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = cagePermanent,
                abilityId = bounceId,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.findPermanent(p1, "Cage of Hands") shouldBe null
        d.getHand(p1) shouldContain cagePermanent
    }
})
