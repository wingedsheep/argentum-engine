package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.SerumSovereign
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Serum Sovereign (ONE) — {4}{U} Creature — Phyrexian Sphinx, 4/4.
 *
 * "Flying
 *  Whenever you cast a noncreature spell, put an oil counter on this creature.
 *  {U}, Remove an oil counter from this creature: Draw a card, then scry 2."
 */
class SerumSovereignScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = SerumSovereign.activatedAbilities.first().id

        fun TestGame.oil(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

        fun TestGame.name(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

        fun TestGame.activate(sovereign: EntityId): ExecutionResult {
            val result = execute(ActivateAbility(playerId = player1Id, sourceId = sovereign, abilityId = abilityId))
            if (result.error == null && getPendingDecision() is SelectManaSourcesDecision) {
                submitManaSourcesAutoPay()
            }
            return result
        }

        context("Serum Sovereign") {

            test("has flying; a noncreature spell adds an oil counter, a creature spell does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serum Sovereign")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Serum Sovereign")!!
                game.state.projectedState.hasKeyword(sovereign, Keyword.FLYING) shouldBe true
                game.oil(sovereign) shouldBe 0

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                withClue("casting a noncreature spell puts an oil counter on it") { game.oil(sovereign) shouldBe 1 }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("casting a creature spell adds nothing") { game.oil(sovereign) shouldBe 1 }
            }

            test("an opponent's noncreature spell does not add an oil counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serum Sovereign")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Serum Sovereign")!!
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.oil(sovereign) shouldBe 0
            }

            test("the ability can't be activated without an oil counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serum Sovereign")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Serum Sovereign")!!
                game.activate(sovereign).error shouldNotBe null
            }

            test("{U}, remove an oil counter: draw a card, then scry 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serum Sovereign")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Serum Sovereign")!!
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.oil(sovereign) shouldBe 1

                val handBefore = game.handSize(1)
                val topCard = game.state.getLibrary(game.player1Id).first()
                game.name(topCard) shouldBe "Grizzly Bears"

                game.activate(sovereign).error shouldBe null
                withClue("the oil counter is removed as a cost") { game.oil(sovereign) shouldBe 0 }
                game.resolveStack()

                withClue("drew the top card before scrying") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.state.getHand(game.player1Id).contains(topCard) shouldBe true
                }

                // Scry 2 looks at the next two cards (Hill Giant, Savannah Lions); bottom Hill Giant.
                val scry = game.getPendingDecision()
                scry.shouldBeInstanceOf<SelectCardsDecision>()
                scry.options.map { game.name(it) }.toSet() shouldBe setOf("Hill Giant", "Savannah Lions")
                val giant = scry.options.first { game.name(it) == "Hill Giant" }
                game.selectCards(listOf(giant)).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

                game.getPendingDecision() shouldBe null
                val library = game.state.getLibrary(game.player1Id)
                library.map { game.name(it) } shouldBe listOf("Savannah Lions", "Forest", "Hill Giant")
            }
        }
    }
}
