package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kitsune Riftwalker (CHK #29) — {1}{W}{W} Creature — Fox Wizard 2/1.
 *
 *   Protection from Spirits and from Arcane
 *
 * Pins both halves: an Arcane spell (a spell subtype, read off the card's type line at cast time)
 * can't target it, a Spirit's damage is prevented and a Spirit can't block it — while a spell that
 * is neither still can target it.
 */
class KitsuneRiftwalkerScenarioTest : ScenarioTestBase() {

    init {
        context("Kitsune Riftwalker") {

            test("an Arcane spell cannot target it") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kitsune Riftwalker")
                    .withCardInHand(2, "Glacial Ray")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val riftwalker = game.findPermanent("Kitsune Riftwalker").shouldNotBeNull()
                withClue("Glacial Ray is Instant — Arcane; protection from Arcane forbids the target") {
                    game.castSpell(2, "Glacial Ray", riftwalker).error shouldNotBe null
                }
            }

            test("a non-Arcane spell can still target it") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kitsune Riftwalker")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val riftwalker = game.findPermanent("Kitsune Riftwalker").shouldNotBeNull()
                game.castSpell(2, "Lightning Bolt", riftwalker).error shouldBe null
                game.resolveStack()
                game.findPermanent("Kitsune Riftwalker") shouldBe null
            }

            test("damage from a Spirit source is prevented") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Earthshaker")
                    .withCardOnBattlefield(1, "Kitsune Riftwalker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lava Spike")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Lava Spike (Arcane) fires Earthshaker (Spirit): 2 damage to each creature without flying.
                game.castSpellTargetingPlayer(2, "Lava Spike", 1).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears has no protection — the sweep kills it") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                val riftwalker = game.findPermanent("Kitsune Riftwalker")
                withClue("Earthshaker is a Spirit, so its damage to the Riftwalker is prevented") {
                    riftwalker.shouldNotBeNull()
                    (game.state.getEntity(riftwalker)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
            }

            test("a Spirit cannot block it") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kitsune Riftwalker")
                    .withCardOnBattlefield(2, "Lantern Kami")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kitsune Riftwalker" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("Lantern Kami is a Spirit — protection from Spirits forbids the block") {
                    game.declareBlockers(mapOf("Lantern Kami" to listOf("Kitsune Riftwalker"))).error shouldNotBe null
                }
            }
        }
    }
}
