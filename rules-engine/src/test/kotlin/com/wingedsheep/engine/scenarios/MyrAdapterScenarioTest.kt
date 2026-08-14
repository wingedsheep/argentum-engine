package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Myr Adapter (MRD #210) — 1/1 for {3}, "gets +1/+1 for each Equipment attached to it."
 *
 * Guards the two ways the bonus is easy to get wrong: it must count Equipment *only* (an Aura hung
 * on the Adapter must not feed it), and it must stack additively with whatever the Equipment itself
 * grants rather than replacing it.
 */
class MyrAdapterScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Myr Adapter") {

            test("bare, it is a 1/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Myr Adapter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adapter = game.findPermanent("Myr Adapter")!!
                val projected = projector.project(game.state)
                projected.getPower(adapter) shouldBe 1
                projected.getToughness(adapter) shouldBe 1
            }

            test("one Equipment gives +1/+1 on top of the Equipment's own bonus") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Myr Adapter")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardAttachedTo(1, "Bonesplitter", "Myr Adapter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adapter = game.findPermanent("Myr Adapter")!!
                val projected = projector.project(game.state)
                withClue("1/1 base, +2/+0 from Bonesplitter, +1/+1 from the Adapter's own ability") {
                    projected.getPower(adapter) shouldBe 4
                    projected.getToughness(adapter) shouldBe 2
                }
            }

            test("two Equipment give +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Myr Adapter")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Vulshok Battlegear")
                    .withCardAttachedTo(1, "Bonesplitter", "Myr Adapter")
                    .withCardAttachedTo(1, "Vulshok Battlegear", "Myr Adapter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adapter = game.findPermanent("Myr Adapter")!!
                val projected = projector.project(game.state)
                withClue("1/1 base, +2/+0 and +3/+3 from the Equipment, +2/+2 for the two of them") {
                    projected.getPower(adapter) shouldBe 8
                    projected.getToughness(adapter) shouldBe 6
                }
            }

            test("an Aura attached to it does not count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Myr Adapter")
                    .withCardOnBattlefield(1, "Arrest")
                    .withCardAttachedTo(1, "Arrest", "Myr Adapter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adapter = game.findPermanent("Myr Adapter")!!
                val projected = projector.project(game.state)
                withClue("the ability counts Equipment, not every attachment") {
                    projected.getPower(adapter) shouldBe 1
                    projected.getToughness(adapter) shouldBe 1
                }
            }
        }
    }
}
