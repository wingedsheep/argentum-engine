package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thornfist Striker {2}{G} 3/3 Elf Druid — Ward {1}.
 *
 * "Infusion — Creatures you control get +1/+0 and have trample as long as you gained life this
 * turn."
 *
 * The Infusion lord is two `ConditionalStaticAbility` blocks (ModifyStats +1/+0 and GrantKeyword
 * trample) both gated on `Conditions.YouGainedLifeThisTurn`: off until life is gained this turn,
 * then both turn on for every creature the controller controls (including Thornfist Striker
 * itself).
 */
class ThornfistStrikerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun hasTrample(game: TestGame, id: EntityId): Boolean =
        projector.getProjectedKeywords(game.state, id).contains(Keyword.TRAMPLE)

    init {
        // A simple life-gain spell to turn the Infusion condition on.
        cardRegistry.register(
            CardDefinition.instant(
                name = "Healing Salve Test",
                manaCost = ManaCost.parse("{W}"),
                oracleText = "You gain 3 life.",
                script = CardScript.spell(effect = GainLifeEffect(3, EffectTarget.Controller)),
            )
        )

        context("Thornfist Striker — Infusion lord") {

            test("no life gained: no buff and no trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thornfist Striker") // 3/3
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2, also controlled
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val striker = game.findPermanent("Thornfist Striker")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("No life gained → Striker stays 3/3 with no trample") {
                    projector.getProjectedPower(game.state, striker) shouldBe 3
                    projector.getProjectedToughness(game.state, striker) shouldBe 3
                    hasTrample(game, striker) shouldBe false
                }
                withClue("No life gained → Grizzly Bears stays 2/2 with no trample") {
                    projector.getProjectedPower(game.state, bears) shouldBe 2
                    hasTrample(game, bears) shouldBe false
                }
            }

            test("after gaining life: all your creatures get +1/+0 and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thornfist Striker") // 3/3
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2
                    .withCardInHand(1, "Healing Salve Test")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val striker = game.findPermanent("Thornfist Striker")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Healing Salve Test").error shouldBe null
                game.resolveStack()

                withClue("Life gained → Striker is 4/3 with trample") {
                    projector.getProjectedPower(game.state, striker) shouldBe 4
                    projector.getProjectedToughness(game.state, striker) shouldBe 3
                    hasTrample(game, striker) shouldBe true
                }
                withClue("Life gained → Grizzly Bears is 3/2 with trample (lord hits all your creatures)") {
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 2
                    hasTrample(game, bears) shouldBe true
                }
            }

            test("opponent's creatures are unaffected even after you gain life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thornfist Striker")
                    .withCardOnBattlefield(2, "Grizzly Bears") // opponent's
                    .withCardInHand(1, "Healing Salve Test")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oppBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Healing Salve Test").error shouldBe null
                game.resolveStack()

                withClue("Opponent's Grizzly Bears stays 2/2 with no trample") {
                    projector.getProjectedPower(game.state, oppBears) shouldBe 2
                    hasTrample(game, oppBears) shouldBe false
                }
            }
        }
    }
}
