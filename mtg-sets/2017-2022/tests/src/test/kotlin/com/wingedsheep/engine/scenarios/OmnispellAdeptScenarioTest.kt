package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.grn.cards.OmnispellAdept
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Omnispell Adept (GRN #49).
 *
 * Focus: an instant is cast from hand with no mana for its own cost, a sorcery is cast outside a
 * main phase (the ruling), and a creature card can't be chosen.
 */
class OmnispellAdeptScenarioTest : ScenarioTestBase() {

    init {
        context("Omnispell Adept") {

            fun board(): TestGame = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Omnispell Adept")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Mind Rot")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun activate(game: TestGame) {
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Omnispell Adept")!!,
                        abilityId = OmnispellAdept.script.activatedAbilities.single().id
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()
            }

            test("casts an instant from hand without paying its mana cost") {
                val game = board()
                activate(game)

                (game.state.pendingDecision is SelectCardsDecision) shouldBe true
                game.selectCards(listOf(game.findCardsInHand(1, "Lightning Bolt").single())).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInHand(1, "Lightning Bolt") shouldBe false
                game.isInHand(1, "Mind Rot") shouldBe true
            }

            test("casts a sorcery outside a main phase") {
                val game = board()
                game.advanceToPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                activate(game)

                game.selectCards(listOf(game.findCardsInHand(1, "Mind Rot").single())).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Mind Rot") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
            }

            test("a creature card can't be chosen, and the cast may be declined") {
                val game = board()
                activate(game)

                game.selectCards(listOf(game.findCardsInHand(1, "Grizzly Bears").single())).outcome shouldNotBe Outcome.Done
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.isInHand(1, "Mind Rot") shouldBe true
            }
        }
    }
}
