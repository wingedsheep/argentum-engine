package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sun-Spider, Nimble Webber (SPM #154) — {3}{W/U} Legendary Creature —
 * Spider Human Hero, 3/2.
 *
 *   During your turn, Sun-Spider has flying.
 *   When Sun-Spider enters, search your library for an Aura or Equipment card, reveal it,
 *   put it into your hand, then shuffle.
 *
 * Exercises the time-restricted static flying (present on your turn, absent on an opponent's turn —
 * same shape as Spider-Girl, Legacy Hero and Shocker, Unshakable) and the ETB tutor that pulls an
 * Aura or Equipment card from the library into hand (only those two subtypes are eligible).
 */
class SunSpiderNimbleWebberScenarioTest : ScenarioTestBase() {

    init {
        context("Sun-Spider, Nimble Webber") {

            test("has flying during its controller's turn but not during an opponent's turn") {
                val onTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sun-Spider, Nimble Webber")
                    .withActivePlayer(1)
                    .build()

                val spiderOnTurn = onTurn.findPermanent("Sun-Spider, Nimble Webber")!!
                withClue("3/2 with flying on your turn") {
                    onTurn.state.projectedState.getPower(spiderOnTurn) shouldBe 3
                    onTurn.state.projectedState.getToughness(spiderOnTurn) shouldBe 2
                    onTurn.state.projectedState.hasKeyword(spiderOnTurn, Keyword.FLYING) shouldBe true
                }

                val offTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sun-Spider, Nimble Webber")
                    .withActivePlayer(2)
                    .build()

                val spiderOffTurn = offTurn.findPermanent("Sun-Spider, Nimble Webber")!!
                withClue("no flying during an opponent's turn") {
                    offTurn.state.projectedState.hasKeyword(spiderOffTurn, Keyword.FLYING) shouldBe false
                }
            }

            test("ETB tutors an Aura or Equipment card from the library into hand and shuffles") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sun-Spider, Nimble Webber")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Pacifism")      // Enchantment — Aura (eligible)
                    .withCardInLibrary(1, "Bonesplitter")  // Artifact — Equipment (eligible)
                    .withCardInLibrary(1, "Grizzly Bears") // creature decoy (not eligible)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun libraryCard(name: String): EntityId? =
                    game.state.getLibrary(game.player1Id).firstOrNull { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == name
                    }

                val pacifism = libraryCard("Pacifism")!!
                val bonesplitter = libraryCard("Bonesplitter")!!
                val bears = libraryCard("Grizzly Bears")!!

                game.castSpell(1, "Sun-Spider, Nimble Webber").error shouldBe null
                game.resolveStack() // Sun-Spider enters → ETB search pauses on a card selection

                val decision = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision for the ETB tutor; got ${game.state.pendingDecision}")

                withClue("only the Aura and the Equipment are eligible to find") {
                    decision.options.contains(pacifism) shouldBe true
                    decision.options.contains(bonesplitter) shouldBe true
                    decision.options.contains(bears) shouldBe false
                }

                game.selectCards(listOf(pacifism))
                game.resolveStack()

                withClue("Pacifism was put into player 1's hand") {
                    game.state.getHand(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Pacifism"
                    } shouldBe true
                }
                withClue("Pacifism is no longer in the library") {
                    libraryCard("Pacifism") shouldBe null
                }
                withClue("the un-chosen cards stay in the library (it was shuffled, not emptied)") {
                    libraryCard("Bonesplitter") shouldNotBe null
                    libraryCard("Grizzly Bears") shouldNotBe null
                }
            }
        }
    }
}
