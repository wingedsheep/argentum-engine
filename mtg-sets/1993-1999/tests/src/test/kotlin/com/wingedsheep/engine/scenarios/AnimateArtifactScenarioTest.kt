package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Animate Artifact (LEA #48):
 *
 *   {3}{U} Enchantment — Aura
 *   "Enchant artifact
 *    As long as enchanted artifact isn't a creature, it's an artifact creature with power and
 *    toughness each equal to its mana value."
 *
 * The group static is scoped by `attachedToBySource()`, so only the enchanted artifact animates,
 * and its "isn't a creature" filter stays locked from Layer 4 into Layer 7b.
 */
class AnimateArtifactScenarioTest : ScenarioTestBase() {

    private val cogwheelEngine = card("Cogwheel Engine") {
        manaCost = "{3}"
        typeLine = "Artifact"
    }

    private val ironIdol = card("Iron Idol") {
        manaCost = "{5}"
        typeLine = "Artifact"
    }

    private val brassSoldier = card("Brass Soldier") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Soldier"
        power = 1
        toughness = 4
    }

    init {
        cardRegistry.register(cogwheelEngine)
        cardRegistry.register(ironIdol)
        cardRegistry.register(brassSoldier)

        context("Animate Artifact") {

            test("only the enchanted noncreature artifact becomes an MV/MV artifact creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Iron Idol")
                    .withCardOnBattlefield(2, "Cogwheel Engine")
                    .withCardAttachedTo(1, "Animate Artifact", "Iron Idol")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val idol = game.findPermanent("Iron Idol")!!
                val engine = game.findPermanent("Cogwheel Engine")!!
                val projected = game.state.projectedState

                withClue("the enchanted {5} artifact is a 5/5 artifact creature") {
                    projected.isCreature(idol) shouldBe true
                    projected.getPower(idol) shouldBe 5
                    projected.getToughness(idol) shouldBe 5
                }
                withClue("the Aura's controller doesn't take it — the opponent still controls it") {
                    projected.getController(idol) shouldBe game.player2Id
                }
                withClue("an artifact the Aura isn't attached to stays a noncreature artifact") {
                    projected.isCreature(engine) shouldBe false
                }
            }

            test("an enchanted artifact that is already a creature keeps its printed power and toughness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Brass Soldier")
                    .withCardAttachedTo(1, "Animate Artifact", "Brass Soldier")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soldier = game.findPermanent("Brass Soldier")!!
                val projected = game.state.projectedState

                projected.getPower(soldier) shouldBe 1
                projected.getToughness(soldier) shouldBe 4
            }
        }
    }
}
