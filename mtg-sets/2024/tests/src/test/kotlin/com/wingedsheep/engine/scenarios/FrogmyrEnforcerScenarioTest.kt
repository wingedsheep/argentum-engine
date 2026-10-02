package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Frogmyr Enforcer (MH3 #120) — {7} Artifact Creature — Frog Myr 4/4
 *
 *   Prototype {3}{R} — 2/2
 *   Affinity for artifacts
 *
 * Prototyped it is a red 2/2 with mana value 4, and affinity still discounts that cost (the card's
 * ruling). Off the battlefield it is a colorless 4/4 with mana value 7 again.
 */
class FrogmyrEnforcerScenarioTest : ScenarioTestBase() {
    init {
        context("Frogmyr Enforcer") {
            test("cast prototyped with affinity, it is a red 2/2; in the graveyard it is a colorless seven-drop") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Frogmyr Enforcer")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("{3}{R} less one for Ornithopter fits three Mountains") {
                    game.castSpellPrototyped(1, "Frogmyr Enforcer").error shouldBe null
                }
                game.resolveStack()

                val frog = game.findPermanent("Frogmyr Enforcer")!!
                val projected = game.state.projectedState
                projected.getPower(frog) shouldBe 2
                projected.getToughness(frog) shouldBe 2
                projected.hasColor(frog, Color.RED) shouldBe true
                game.state.getEntity(frog)!!.get<CardComponent>()!!.manaValue shouldBe 4

                game.passPriority()
                game.castSpell(2, "Shock", frog).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Frogmyr Enforcer") shouldBe true
                val card = game.findCardsInGraveyard(1, "Frogmyr Enforcer").single()
                val printed = game.state.getEntity(card)!!.get<CardComponent>()!!
                printed.manaValue shouldBe 7
                printed.colors shouldBe emptySet()
            }

            test("cast normally it is a colorless 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Frogmyr Enforcer")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Frogmyr Enforcer").error shouldBe null
                game.resolveStack()

                val frog = game.findPermanent("Frogmyr Enforcer")
                frog shouldNotBe null
                game.state.projectedState.getPower(frog!!) shouldBe 4
                game.state.projectedState.getColors(frog) shouldBe emptySet()
            }
        }
    }
}
