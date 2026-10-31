package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Thunderkin Awakener (M20 #162) — {1}{R} Creature — Elemental Shaman, 1/2.
 *
 *   Haste
 *   Whenever this creature attacks, choose target Elemental creature card in your graveyard with
 *   toughness less than this creature's toughness. Return that card to the battlefield tapped and
 *   attacking. Sacrifice it at the beginning of the next end step.
 *
 * Covers the source-relative toughness filter (strictly less, Elemental creature cards only), the
 * tapped-and-attacking return, and the end-step sacrifice of the returned creature.
 */
class ThunderkinAwakenerScenarioTest : ScenarioTestBase() {

    init {
        context("Thunderkin Awakener") {

            test("returns an Elemental with lesser toughness attacking, then sacrifices it at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thunderkin Awakener", summoningSickness = false)
                    .withCardInGraveyard(1, "Lightning Elemental") // 4/1 Elemental — legal
                    .withCardInGraveyard(1, "Terrain Elemental")   // 3/2 Elemental — toughness not less
                    .withCardInGraveyard(1, "Raging Goblin")       // 1/1 Goblin — not an Elemental
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thunderkin Awakener" to 2)).error shouldBe null
                game.resolveStack()

                val elemental = game.findCardsInGraveyard(1, "Lightning Elemental").single()
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only the Elemental creature card with toughness below 2 is a legal target") {
                    decision.legalTargets[0]!! shouldContainExactly listOf(elemental)
                }
                game.selectTargets(listOf(elemental))
                game.resolveStack()

                withClue("Lightning Elemental returns tapped and attacking") {
                    game.findPermanent("Lightning Elemental") shouldBe elemental
                    game.state.getEntity(elemental)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(elemental)?.has<AttackingComponent>() shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("both attackers dealt combat damage") {
                    game.getLifeTotal(2) shouldBe 15
                }
                withClue("the returned creature is sacrificed at the beginning of the end step") {
                    game.isOnBattlefield("Lightning Elemental") shouldBe false
                    game.isInGraveyard(1, "Lightning Elemental") shouldBe true
                    game.isOnBattlefield("Thunderkin Awakener") shouldBe true
                }
            }

            test("no Elemental with lesser toughness means nothing returns") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thunderkin Awakener", summoningSickness = false)
                    .withCardInGraveyard(1, "Terrain Elemental")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thunderkin Awakener" to 2)).error shouldBe null
                game.resolveStack()

                withClue("a 3/2 Elemental's toughness is not less than the Awakener's 2") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.isInGraveyard(1, "Terrain Elemental") shouldBe true
                }
            }
        }
    }
}
