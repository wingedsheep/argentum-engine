package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.OffenderAtLarge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Offender at Large — {4}{R} 5/4 Giant Rogue with Disguise {4}{R} and "when this creature enters or
 * is turned face up, up to one target creature gets +2/+0 until end of turn."
 *
 * One ability, two disjoint routes (CR 702.168d): the pump fires once on entry for a hard-cast
 * copy and once on the flip for a disguised one — never twice. "Up to one target" is what makes
 * the flip safe on an empty board: the ability still resolves with nothing chosen instead of being
 * removed from the stack for want of a legal target.
 */
class OffenderAtLargeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OffenderAtLarge))
        return driver
    }

    test("hard-cast: the entry trigger pumps a chosen creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val card = driver.putCardInHand(player, "Offender at Large")
        driver.giveMana(player, Color.RED, 5)

        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player, listOf(bear)).error shouldBe null
        driver.bothPass()

        val offender = driver.findPermanent(player, "Offender at Large")
        offender.shouldNotBeNull()
        val projected = driver.state.projectedState
        projected.getPower(offender) shouldBe 5
        projected.getToughness(offender) shouldBe 4
        projected.getPower(bear) shouldBe 4
        projected.getToughness(bear) shouldBe 2
    }

    test("the disguise flip fires the same ability, and 'up to one' allows no target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Offender at Large")
        driver.giveColorlessMana(player, 3)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()

        val offender = driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }

        driver.giveMana(player, Color.RED, 5)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = offender,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null

        // The flip fires the ability. The only creature around is the Offender itself; decline it.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player, emptyList()).error shouldBe null
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(offender) shouldBe 5
        projected.getToughness(offender) shouldBe 4
    }
})
