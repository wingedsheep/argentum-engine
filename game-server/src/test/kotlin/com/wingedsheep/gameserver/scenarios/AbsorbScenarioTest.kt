package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Absorb.
 *
 * Absorb ({W}{U}{U}, Instant):
 *   "Counter target spell. You gain 3 life."
 *
 * Exercises the `CounterSpell then GainLife(3)` composition — the life gain goes to Absorb's
 * caster, not the countered spell's controller.
 */
class AbsorbScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Test Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 2, 2)
        )

        context("Absorb effect") {
            test("counters a spell and the caster gains 3 life") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Absorb")
                    .withCardInHand(2, "Test Bear")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearResult = game.castSpell(2, "Test Bear")
                withClue("Test Bear should be cast: ${bearResult.error}") {
                    bearResult.error shouldBe null
                }
                game.execute(PassPriority(game.player2Id))

                val counterResult = game.castSpellTargetingStackSpell(1, "Absorb", "Test Bear")
                withClue("Absorb should be cast: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Test Bear should be countered (in Opponent's graveyard)") {
                    game.isOnBattlefield("Test Bear") shouldBe false
                    game.isInGraveyard(2, "Test Bear") shouldBe true
                }
                withClue("Absorb's caster gains 3 life: 20 -> 23") {
                    game.getLifeTotal(1) shouldBe 23
                }
                withClue("Opponent's life is unchanged") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
