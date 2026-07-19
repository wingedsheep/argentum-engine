package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.OjerKaslemDeepestGrowth
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ojer Kaslem, Deepest Growth // Temple of Cultivation (LCI #204).
 *
 * Covers the three behaviours of the card:
 *  1. **Dies → return tapped + transformed** — the shared
 *     [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromZoneTransformedEffect]`(tapped = true)`
 *     wired to [com.wingedsheep.sdk.dsl.Triggers.Dies]. When the God dies, the same entity returns
 *     to the battlefield **tapped** as its back face, Temple of Cultivation.
 *  2. **Combat-damage reveal** — deals N combat damage → reveal N → put up to one creature and up
 *     to one land onto the battlefield (the "and/or") → bottom the rest in a random order.
 *  3. **Transform-back gate** — the land's `{2}{G}, {T}` transform is only activatable while you
 *     control ten or more permanents (CR: activate-only-if condition).
 */
class OjerKaslemDeepestGrowthScenarioTest : ScenarioTestBase() {

    init {
        fun optionNamed(game: TestGame, decision: SelectCardsDecision, name: String): EntityId =
            decision.options.first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

        context("Ojer Kaslem, Deepest Growth") {

            test("when it dies it returns to the battlefield tapped as Temple of Cultivation") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Kaslem, Deepest Growth", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2) // {R}{R} for two Bolts
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaslem = game.findPermanent("Ojer Kaslem, Deepest Growth")!!

                // 6 damage to a 6/5 kills it (SBA). The dies-trigger returns it transformed + tapped.
                repeat(2) {
                    game.castSpell(1, "Lightning Bolt", targetId = kaslem).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) {
                        game.submitManaSourcesAutoPay()
                    }
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Temple of Cultivation") == null && guard++ < 10) {
                    game.resolveStack()
                }

                withClue("same entity returned to the battlefield as the back face") {
                    game.findPermanent("Temple of Cultivation") shouldBe kaslem
                    game.state.getEntity(kaslem)!!.get<CardComponent>()!!.name shouldBe "Temple of Cultivation"
                }
                withClue("it returned tapped") {
                    game.state.getEntity(kaslem)!!.get<TappedComponent>() shouldNotBe null
                }
            }

            test("combat damage reveals N and puts a creature and a land onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Kaslem, Deepest Growth", summoningSickness = false)
                    // Exactly six cards in library so reveal-6 sees all of them; one creature, one
                    // land, four non-creature-non-land fillers.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ojer Kaslem, Deepest Growth" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // First select: the creature.
                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) game.resolveStack()
                val creatureDecision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision for the creature; got ${game.getPendingDecision()}")
                game.submitDecision(
                    CardsSelectedResponse(creatureDecision.id, listOf(optionNamed(game, creatureDecision, "Grizzly Bears")))
                )

                // Second select: the land.
                guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) game.resolveStack()
                val landDecision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision for the land; got ${game.getPendingDecision()}")
                game.submitDecision(
                    CardsSelectedResponse(landDecision.id, listOf(optionNamed(game, landDecision, "Forest")))
                )
                game.resolveStack()

                withClue("the chosen creature and land entered the battlefield") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                    game.findPermanent("Forest").shouldNotBeNull()
                }
                withClue("the remaining four cards were bottomed") {
                    game.librarySize(1) shouldBe 4
                }
            }
        }

        context("Temple of Cultivation — transform-back gate") {

            val transformAbilityId = OjerKaslemDeepestGrowth.backFace!!
                .activatedAbilities.first { !it.isManaAbility }.id

            test("cannot transform back while controlling fewer than ten permanents") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Cultivation")
                    .withLandsOnBattlefield(1, "Forest", 4) // enough mana; only 5 permanents total (<10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Cultivation")!!
                val result = game.execute(ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId))

                withClue("activation is illegal under the ten-permanent gate") {
                    result.error shouldNotBe null
                }
                withClue("it stays a land — no transform happened") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Temple of Cultivation"
                }
            }

            test("transforms back into Ojer Kaslem while controlling ten or more permanents") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Cultivation")
                    .withLandsOnBattlefield(1, "Forest", 9) // Temple + 9 Forests = 10 permanents
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Cultivation")!!
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the land flipped to its front face") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Ojer Kaslem, Deepest Growth"
                }
            }
        }
    }
}
