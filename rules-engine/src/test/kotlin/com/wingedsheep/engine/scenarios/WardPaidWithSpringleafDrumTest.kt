package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.LongRiverLurker
import com.wingedsheep.mtg.sets.definitions.lrw.cards.SpringleafDrum
import com.wingedsheep.mtg.sets.definitions.scg.cards.Carbonize
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Springleaf Drum ({T}, Tap an untapped creature you control: Add one mana of any color)
 * is a mana ability with a composite tap-self + tap-another-creature cost. Per CR 605.3a /
 * 118.5, mana abilities can be activated whenever a player has priority *or* whenever they
 * are paying a cost — including the cost of a ward triggered ability (CR 702.21b).
 *
 * Scenario: opponent controls Long River Lurker (ward {1}). Active player has Springleaf
 * Drum + an untapped creature on the battlefield and exactly enough mana to cast Carbonize
 * targeting the Lurker. After Carbonize is cast, the player has no floating mana left;
 * Springleaf Drum is the only way to produce the {1} for ward.
 *
 * The ward payment prompt must list Springleaf Drum as an available mana source.
 */
class WardPaidWithSpringleafDrumTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LongRiverLurker, SpringleafDrum, Carbonize))
        return driver
    }

    test("Springleaf Drum appears in the ward mana selection list") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lurker = driver.putCreatureOnBattlefield(opponent, "Long River Lurker")
        val drum = driver.putPermanentOnBattlefield(activePlayer, "Springleaf Drum")
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        driver.giveMana(activePlayer, Color.RED, 3)
        val carbonize = driver.putCardInHand(activePlayer, "Carbonize")
        driver.castSpellWithTargets(
            activePlayer, carbonize, listOf(ChosenTarget.Permanent(lurker))
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        val sourceIds = decision.availableSources.map { it.entityId }
        sourceIds shouldContain drum
    }

    test("selecting Springleaf Drum pauses for a creature pick, then pays ward") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lurker = driver.putCreatureOnBattlefield(opponent, "Long River Lurker")
        val drum = driver.putPermanentOnBattlefield(activePlayer, "Springleaf Drum")
        val lions = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        driver.giveMana(activePlayer, Color.RED, 3)
        val carbonize = driver.putCardInHand(activePlayer, "Carbonize")
        driver.castSpellWithTargets(
            activePlayer, carbonize, listOf(ChosenTarget.Permanent(lurker))
        )

        driver.bothPass()

        val manaDecision = driver.pendingDecision
        manaDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        manaDecision.availableSources.first { it.entityId == drum }
            .requiresTappingAnotherPermanent shouldBe true

        // Pick Springleaf Drum manually — auto-pay shouldn't pick it, so we drive the
        // selection ourselves. The engine should pause for a follow-up creature pick.
        val tapTargetResult = driver.submitDecision(
            activePlayer,
            ManaSourcesSelectedResponse(
                decisionId = manaDecision.id,
                selectedSources = listOf(drum),
                autoPay = false
            )
        )
        tapTargetResult.isSuccess shouldBe true

        val followUp = driver.pendingDecision
        followUp.shouldBeInstanceOf<SelectCardsDecision>()
        followUp.options shouldContain lions
        followUp.options.contains(drum) shouldBe false

        // Pick the Lions to tap as Springleaf's sub-cost. Payment completes, ward resolves
        // successfully, Carbonize keeps resolving and kills the Lurker.
        val finalize = driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decisionId = followUp.id, selectedCards = listOf(lions))
        )
        finalize.isSuccess shouldBe true

        // Drain anything left on the stack.
        repeat(6) { if (driver.state.priorityPlayerId != null && driver.pendingDecision == null) driver.bothPass() }

        // Springleaf tapped, Lions tapped, ward paid → Carbonize resolved → Lurker dies.
        driver.isTapped(drum) shouldBe true
        driver.isTapped(lions) shouldBe true
        driver.findPermanent(opponent, "Long River Lurker") shouldBe null
        driver.getGraveyardCardNames(opponent).contains("Long River Lurker") shouldBe true
    }
})
