package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Seraph of New Capenna // Seraph of New Phyrexia (MOM #36).
 *
 *   Front (2/2 flying) — "{4}{B/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/3 flying) — "Whenever this creature attacks, you may sacrifice another creature or
 *                         artifact. If you do, this creature gets +2/+1 until end of turn."
 */
class SeraphOfNewCapennaScenarioTest : ScenarioTestBase() {

    private fun transformedGame(): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Seraph of New Capenna")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val seraph = game.findPermanent("Seraph of New Capenna")!!
        val abilityId = cardRegistry.getCard("Seraph of New Capenna")!!.activatedAbilities.first().id
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = seraph, abilityId = abilityId)
        ).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game to seraph
    }

    init {
        context("Seraph of New Capenna") {

            test("transforms by paying 2 life for {B/P} into a 3/3 flying Seraph of New Phyrexia") {
                val (game, seraph) = transformedGame()

                game.getLifeTotal(1) shouldBe 18
                game.state.getEntity(seraph)!!.get<CardComponent>()!!.name shouldBe "Seraph of New Phyrexia"
                game.state.projectedState.getPower(seraph) shouldBe 3
                game.state.projectedState.getToughness(seraph) shouldBe 3
                game.state.projectedState.hasKeyword(seraph, Keyword.FLYING) shouldBe true
            }

            test("attacking and sacrificing another creature gives +2/+1 until end of turn") {
                val (game, seraph) = transformedGame()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Seraph of New Phyrexia" to 2)).error shouldBe null

                var guard = 0
                while (guard++ < 6) {
                    when (val decision = game.getPendingDecision()) {
                        is YesNoDecision -> game.answerYesNo(true).error shouldBe null
                        is SelectCardsDecision -> {
                            withClue("the Seraph itself is not an option (\"another\")") {
                                decision.options.contains(seraph) shouldBe false
                            }
                            game.selectCards(listOf(bears)).error shouldBe null
                        }
                        null -> if (game.state.stack.isNotEmpty()) game.passPriority() else break
                        else -> break
                    }
                }

                withClue("Grizzly Bears was sacrificed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                game.state.projectedState.getPower(seraph) shouldBe 5
                game.state.projectedState.getToughness(seraph) shouldBe 4
            }

            test("declining the sacrifice leaves it a 3/3") {
                val (game, seraph) = transformedGame()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Seraph of New Phyrexia" to 2)).error shouldBe null

                var guard = 0
                while (guard++ < 6) {
                    when (game.getPendingDecision()) {
                        is YesNoDecision -> game.answerYesNo(false).error shouldBe null
                        is SelectCardsDecision -> game.skipSelection().error shouldBe null
                        null -> if (game.state.stack.isNotEmpty()) game.passPriority() else break
                        else -> break
                    }
                }

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.projectedState.getPower(seraph) shouldBe 3
                game.state.projectedState.getToughness(seraph) shouldBe 3
            }
        }
    }
}
