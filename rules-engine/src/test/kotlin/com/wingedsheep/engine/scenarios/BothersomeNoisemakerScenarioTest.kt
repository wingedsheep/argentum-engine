package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bothersome Noisemaker (HOB) — {1}{R} Creature — Goblin Bard 2/2.
 *
 * "Whenever you cast a noncreature spell, amass Goblins 1."
 *
 * The trigger is on *casting*, so it fires while the spell is still on the stack — before that
 * spell resolves. Creature spells must not trigger it, and neither must an opponent's noncreature
 * spell.
 */
class BothersomeNoisemakerScenarioTest : ScenarioTestBase() {

    init {
        context("Bothersome Noisemaker") {

            test("it is a 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bothersome Noisemaker")
                    .build()

                val noisemaker = game.findPermanent("Bothersome Noisemaker")!!
                game.state.projectedState.getPower(noisemaker) shouldBe 2
                game.state.projectedState.getToughness(noisemaker) shouldBe 2
            }

            test("casting a noncreature spell amasses Goblins 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bothersome Noisemaker")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val army = game.findPermanent("Goblin Army")
                    ?: error("casting a noncreature spell should have amassed")
                game.state.projectedState.getPower(army) shouldBe 1
                withClue("the Bolt still resolved") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("casting a creature spell does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bothersome Noisemaker")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger reads 'noncreature spell'") {
                    game.findAllPermanents("Goblin Army").size shouldBe 0
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("an opponent's noncreature spell does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bothersome Noisemaker")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger reads 'whenever you cast'") {
                    game.findAllPermanents("Goblin Army").size shouldBe 0
                }
                game.getLifeTotal(1) shouldBe 17
            }
        }
    }
}
