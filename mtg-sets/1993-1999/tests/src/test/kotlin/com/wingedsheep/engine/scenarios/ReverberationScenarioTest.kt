package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reverberation — {2}{U}{U} Instant.
 *  - All damage that would be dealt this turn by target sorcery spell is dealt to that spell's
 *    controller instead.
 *
 * Pinned: a burn spell aimed at you hits its caster, a sweeper's damage to every creature and
 * player lands on its caster (damage already aimed at the caster stays put), and other sources are
 * unaffected.
 */
class ReverberationScenarioTest : ScenarioTestBase() {

    init {
        context("Reverberation") {
            test("Lava Axe aimed at you deals its 5 damage to its caster instead") {
                val game = scenario()
                    .withPlayers("Caster", "Reverberator")
                    .withCardInHand(1, "Lava Axe")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(2, "Reverberation")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Reverberation", "Lava Axe").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 15
            }

            test("every bit of Earthquake's damage lands on its caster, including what was already aimed at them") {
                val game = scenario()
                    .withPlayers("Caster", "Reverberator")
                    .withCardInHand(1, "Earthquake")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Reverberation")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Earthquake", 1).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Reverberation", "Earthquake").error shouldBe null
                game.resolveStack()

                withClue("1 to each of two creatures and each of two players, all dealt to the caster") {
                    game.getLifeTotal(1) shouldBe 16
                }
                game.getLifeTotal(2) shouldBe 20
                val lions = game.findPermanent("Savannah Lions")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(lions)?.get<DamageComponent>()?.amount shouldBe null
                game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe null
            }

            test("other sources' damage is unaffected") {
                val game = scenario()
                    .withPlayers("Caster", "Reverberator")
                    .withCardInHand(1, "Lava Axe")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInHand(2, "Reverberation")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Reverberation", "Lava Axe").error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 15
                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
