package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eternal Thirst (M15 #95) — {1}{B} Enchantment — Aura.
 *
 * "Enchanted creature has lifelink and 'Whenever a creature an opponent controls dies, put a +1/+1
 *  counter on this creature.'"
 *
 * Proves the granted trigger reads from the host's side: the counter lands on the enchanted
 * creature, and "an opponent" is an opponent of the *host's* controller — so on an opposing
 * creature it grows when the Aura controller's creatures die, not the host's own side's.
 */
class EternalThirstScenarioTest : ScenarioTestBase() {

    init {
        context("Eternal Thirst") {

            test("host has lifelink and grows only when an opponent's creature dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Eternal Thirst", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Doom Blade")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val goblin = game.findPermanent("Raging Goblin")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true

                game.castSpell(1, "Doom Blade", goblin).error shouldBe null
                game.resolveStack()

                withClue("the Bears' controller's own Goblin died — no counter") {
                    game.isInGraveyard(1, "Raging Goblin") shouldBe true
                    game.state.projectedState.getPower(bears) shouldBe 2
                }

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                withClue("the opponent's Hill Giant died — the enchanted Bears got a +1/+1 counter") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("on an opposing creature, 'an opponent' is relative to the host's controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Eternal Thirst", "Hill Giant")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val goblin = game.findPermanent("Raging Goblin")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Doom Blade", goblin).error shouldBe null
                game.resolveStack()

                withClue("the host's own side lost a creature — no counter on the Giant") {
                    game.isInGraveyard(2, "Raging Goblin") shouldBe true
                    game.state.projectedState.getPower(giant) shouldBe 3
                }

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                withClue("the Aura controller's Bears died — an opponent of the Giant's controller") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.state.projectedState.getPower(giant) shouldBe 4
                }
            }
        }
    }
}
