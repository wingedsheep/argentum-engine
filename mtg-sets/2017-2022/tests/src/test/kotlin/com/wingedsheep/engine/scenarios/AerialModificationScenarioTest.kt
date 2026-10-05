package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Aerial Modification (AER #1) — {4}{W} Enchantment — Aura.
 *
 *   Enchant creature or Vehicle
 *   As long as enchanted permanent is a Vehicle, it's a creature in addition to its other types.
 *   Enchanted creature gets +2/+2 and has flying.
 *
 * Renegade Freighter is an uncrewed 4/3 Vehicle with no flying of its own, so it shows the
 * animation, the +2/+2 and the granted flying all at once; Grizzly Bears shows the plain
 * creature case; Disenchant on the Aura shows the animation ending with it.
 */
class AerialModificationScenarioTest : ScenarioTestBase() {

    init {
        context("Aerial Modification") {

            test("cast on an uncrewed Vehicle, it becomes a 6/5 flying creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Aerial Modification")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Renegade Freighter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val freighter = game.findPermanent("Renegade Freighter")!!
                withClue("an uncrewed Vehicle is not a creature") {
                    game.state.projectedState.isCreature(freighter) shouldBe false
                }

                game.castSpell(1, "Aerial Modification", targetId = freighter).error shouldBe null
                game.resolveStack()

                val aura = game.findPermanent("Aerial Modification")!!
                withClue("the Aura legally enchants the Vehicle") {
                    game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe freighter
                }
                withClue("the enchanted Vehicle is a creature, 4/3 + 2/2 = 6/5, with flying") {
                    val projected = game.state.projectedState
                    projected.isCreature(freighter) shouldBe true
                    projected.hasType(freighter, "ARTIFACT") shouldBe true
                    projected.getPower(freighter) shouldBe 6
                    projected.getToughness(freighter) shouldBe 5
                    projected.hasKeyword(freighter, Keyword.FLYING) shouldBe true
                }
            }

            test("on a non-Vehicle creature it only grants +2/+2 and flying") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Aerial Modification", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("2/2 + 2/2 = 4/4 with flying") {
                    val projected = game.state.projectedState
                    projected.isCreature(bears) shouldBe true
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                    projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
            }

            test("when the Aura leaves, the Vehicle stops being a creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Renegade Freighter")
                    .withCardAttachedTo(1, "Aerial Modification", "Renegade Freighter")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val freighter = game.findPermanent("Renegade Freighter")!!
                val aura = game.findPermanent("Aerial Modification")!!
                withClue("animated while enchanted") {
                    game.state.projectedState.isCreature(freighter) shouldBe true
                }

                game.castSpell(1, "Disenchant", targetId = aura).error shouldBe null
                game.resolveStack()

                withClue("the Aura is destroyed") {
                    game.isInGraveyard(1, "Aerial Modification") shouldBe true
                }
                withClue("the Vehicle is a noncreature artifact again, without flying") {
                    val projected = game.state.projectedState
                    game.isOnBattlefield("Renegade Freighter") shouldBe true
                    projected.isCreature(freighter) shouldBe false
                    projected.hasKeyword(freighter, Keyword.FLYING) shouldBe false
                }
            }
        }
    }
}
