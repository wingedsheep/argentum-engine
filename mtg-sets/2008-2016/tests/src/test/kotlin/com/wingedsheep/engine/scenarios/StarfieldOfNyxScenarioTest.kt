package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Starfield of Nyx (ORI #33) — "As long as you control five or more enchantments, each other
 * non-Aura enchantment you control is a creature in addition to its other types and has base power
 * and base toughness each equal to its mana value."
 *
 * The animation is a pair of group statics (Layer 4 type, Layer 7b base P/T) gated on a
 * battlefield count, so being right depends on how the projector runs them: Starfield counts
 * toward the five but never animates itself, and one enchantment short of five nothing animates.
 */
class StarfieldOfNyxScenarioTest : ScenarioTestBase() {

    // A plain {1}{W}{W} enchantment: mana value 3, no abilities of its own.
    private val starlitIdol = card("Starlit Idol") {
        manaCost = "{1}{W}{W}"
        typeLine = "Enchantment"
    }

    init {
        cardRegistry.register(starlitIdol)

        context("Starfield of Nyx") {

            test("with five enchantments, each other non-Aura enchantment is an MV/MV creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Starfield of Nyx")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val starfield = game.findPermanent("Starfield of Nyx")!!
                val idol = game.findPermanent("Starlit Idol")!!

                withClue("an Idol becomes a creature with base P/T equal to its mana value (3)") {
                    projected.isCreature(idol) shouldBe true
                    projected.getPower(idol) shouldBe 3
                    projected.getToughness(idol) shouldBe 3
                }
                withClue("Starfield counts toward the five but does not animate itself") {
                    projected.isCreature(starfield) shouldBe false
                }
            }

            test("with only four enchantments nothing is animated") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Starfield of Nyx")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val idol = game.findPermanent("Starlit Idol")!!
                game.state.projectedState.isCreature(idol) shouldBe false
            }

            test("an opponent's enchantments are not animated by your Starfield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Starfield of Nyx")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(1, "Starlit Idol")
                    .withCardOnBattlefield(2, "Starlit Idol")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val opponentIdol = game.state.getBattlefield().first { id ->
                    game.state.getEntity(id)
                        ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Starlit Idol" &&
                        projected.getController(id) == game.player2Id
                }
                projected.isCreature(opponentIdol) shouldBe false
            }
        }
    }
}
