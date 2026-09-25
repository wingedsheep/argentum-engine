package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.SoratamiSeer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe

/**
 * Soratami Seer (CHK) — "{4}, Return two lands you control to their owner's hand: Discard all the
 * cards in your hand, then draw that many cards."
 *
 * The bounced lands are paid before the ability resolves, so they are in hand for the discard and
 * count toward "that many".
 */
class SoratamiSeerScenarioTest : FunSpec({

    val abilityId = SoratamiSeer.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SoratamiSeer)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("discards the hand, bounced lands included, and draws that many") {
        val d = driver()
        val p1 = d.player1
        val seer = d.putCreatureOnBattlefield(p1, "Soratami Seer")
        val lands = listOf(d.putLandOnBattlefield(p1, "Island"), d.putLandOnBattlefield(p1, "Island"))
        val originalHand = d.getHand(p1)
        val handBefore = originalHand.size
        val libraryBefore = d.state.getLibrary(p1).size
        d.giveColorlessMana(p1, 4)

        d.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = seer,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        withClue("the lands return to hand as the cost is paid") {
            d.getHand(p1) shouldContainAll lands
        }
        d.bothPass()

        val discarded = handBefore + 2
        d.getGraveyard(p1) shouldContainAll (originalHand + lands)
        d.getHandSize(p1) shouldBe discarded
        d.getHand(p1) shouldNotContainAnyOf (originalHand + lands)
        d.state.getLibrary(p1).size shouldBe libraryBefore - discarded
    }

    test("cannot be activated while controlling fewer than two lands") {
        val d = driver()
        val p1 = d.player1
        val seer = d.putCreatureOnBattlefield(p1, "Soratami Seer")
        val land = d.putLandOnBattlefield(p1, "Island")
        d.giveColorlessMana(p1, 4)

        d.submitExpectFailure(
            ActivateAbility(
                playerId = p1,
                sourceId = seer,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(land)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.findPermanent(p1, "Island") shouldBe land
    }
})
