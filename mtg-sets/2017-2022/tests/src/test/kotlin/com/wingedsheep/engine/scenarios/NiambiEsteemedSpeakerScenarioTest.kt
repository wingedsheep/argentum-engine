package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.NiambiEsteemedSpeaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Niambi, Esteemed Speaker — "When Niambi enters, you may return another target creature you
 * control to its owner's hand. If you do, you gain life equal to that creature's mana value."
 *
 * Pins the may-wrapped pipeline: the targeted creature's mana value is read before the bounce, the
 * life gain only happens when the player accepts (the "you may" is answered at targeting, where
 * choosing no target declines), and Niambi can't target herself.
 */
class NiambiEsteemedSpeakerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + NiambiEsteemedSpeaker)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castNiambi(d: GameTestDriver, target: EntityId, accept: Boolean) {
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        val niambi = d.putCardInHand(d.player1, NiambiEsteemedSpeaker.name)
        d.castSpell(d.player1, niambi).outcome shouldBe Outcome.Done
        var guard = 0
        var offeredDecline = false
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val pd = d.pendingDecision) {
                is ChooseTargetsDecision -> {
                    // The lone "you may" is answered at targeting: a target is yes, none is decline.
                    offeredDecline = pd.targetRequirements.single().minTargets == 0
                    val legal = pd.legalTargets.getValue(0)
                    legal shouldContain target
                    withClue("Niambi can't target herself") {
                        legal shouldNotContain d.findPermanent(d.player1, NiambiEsteemedSpeaker.name)!!
                    }
                    val chosen = if (accept) listOf(target) else emptyList()
                    d.submitTargetSelection(pd.playerId, chosen).error shouldBe null
                }
                null -> d.bothPass()
                else -> error("unexpected decision $pd")
            }
        }
        withClue("the return is optional") { offeredDecline shouldBe true }
    }

    test("returning the creature gains life equal to its mana value") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        castNiambi(d, courser, accept = true)

        d.getHand(d.player1).map { d.getCardName(it) } shouldContain "Centaur Courser"
        d.findPermanent(d.player1, "Centaur Courser") shouldBe null
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("declining leaves the creature and gains no life") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        castNiambi(d, courser, accept = false)

        d.findPermanent(d.player1, "Centaur Courser") shouldBe courser
        d.getLifeTotal(d.player1) shouldBe 20
    }
})
