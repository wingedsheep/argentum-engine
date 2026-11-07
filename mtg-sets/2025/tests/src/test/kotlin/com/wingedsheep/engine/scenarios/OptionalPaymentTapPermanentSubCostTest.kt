package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A mana source whose ability also costs "Tap an untapped permanent you control" (Gene Pollinator)
 * must charge that extra tap when a player hand-picks it to pay an optional "you may pay {1}".
 * Only the ward / counter-unless payment used to ask for the extra permanent; the plain may-pay
 * resumer and the targeted may-pay trigger resumer tapped the source alone, so the player got the
 * mana for half the cost.
 */
class OptionalPaymentTapPermanentSubCostTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        test("paying Wooden Sphere's {1} with Gene Pollinator asks for and taps the other permanent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wooden Sphere")
                .withCardOnBattlefield(1, "Gene Pollinator")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sphere = game.findPermanent("Wooden Sphere")!!
            val pollinator = game.findPermanent("Gene Pollinator")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null

            val manaDecision = game.getPendingDecision()
            manaDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
            manaDecision.availableSources.first { it.entityId == pollinator }
                .requiresTappingAnotherPermanent shouldBe true

            game.submitManaSourcesDecision(selectedSources = listOf(pollinator)).error shouldBe null

            val tapChoice = game.getPendingDecision()
            tapChoice.shouldBeInstanceOf<SelectCardsDecision>()
            tapChoice.options shouldContain sphere
            tapChoice.options shouldNotContain pollinator

            game.selectCards(listOf(sphere)).error shouldBe null
            game.resolveStack()

            game.isTapped(pollinator) shouldBe true
            game.isTapped(sphere) shouldBe true
            game.getLifeTotal(1) shouldBe 21
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("Gene Pollinator can't pay the optional {1} when no other untapped permanent is left") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wooden Sphere", tapped = true)
                .withCardOnBattlefield(1, "Gene Pollinator")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pollinator = game.findPermanent("Gene Pollinator")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            // Nothing can pay {1}: the trigger either skips the offer or the source is refused.
            val pending = game.getPendingDecision()
            if (pending != null) {
                game.answerYesNo(true).error shouldBe null
                val manaDecision = game.getPendingDecision()
                if (manaDecision is SelectManaSourcesDecision) {
                    manaDecision.availableSources.map { it.entityId } shouldNotContain pollinator
                    game.submitManaSourcesDecision(selectedSources = listOf(pollinator)).error shouldNotBe null
                }
            }

            game.isTapped(pollinator) shouldBe false
            game.getLifeTotal(1) shouldBe 20
        }

        test("paying Lightning Rift's {1} with Gene Pollinator taps the other permanent on resolution") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Lightning Rift")
                .withCardOnBattlefield(1, "Gene Pollinator")
                .withCardInHand(1, "Tranquil Thicket")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rift = game.findPermanent("Lightning Rift")!!
            val pollinator = game.findPermanent("Gene Pollinator")!!

            game.cycleCard(1, "Tranquil Thicket").error shouldBe null

            // The Rift's target is chosen as its trigger goes on the stack (CR 603.3d) …
            val targeting = game.getPendingDecision()
            targeting.shouldNotBeNull()
            targeting.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.submitDecision(
                TargetsResponse(targeting.id, mapOf(0 to listOf(game.player2Id)))
            ).error shouldBe null

            // … and the optional {1} is paid only as it resolves.
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null

            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesDecision(selectedSources = listOf(pollinator)).error shouldBe null

            val tapChoice = game.getPendingDecision()
            tapChoice.shouldBeInstanceOf<SelectCardsDecision>()
            tapChoice.options shouldContain rift
            game.selectCards(listOf(rift)).error shouldBe null
            game.resolveStack()

            game.isTapped(pollinator) shouldBe true
            game.isTapped(rift) shouldBe true
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
