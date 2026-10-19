package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Silhouette — {1}{U} Instant.
 * "Choose target creature. If a spell or ability that targets that creature would cause a source
 * to deal damage to that creature this turn, prevent that damage."
 *
 * Pinned: a targeted spell's damage is prevented; so is an ability's that targets the creature,
 * though the ability's creature is the source; an untargeted sweeper and combat damage get through.
 */
class SilhouetteScenarioTest : ScenarioTestBase() {

    /** Player 1, active in their main phase, holds Silhouette plus [hand] and controls Grizzly Bears. */
    private fun board(vararg hand: String, sorcerer: Boolean = false) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Silhouette")
        .apply { hand.forEach { withCardInHand(1, it) } }
        .apply { if (sorcerer) withCardOnBattlefield(1, "Prodigal Sorcerer") }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.silhouetteOnBears() {
        castSpell(1, "Silhouette", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
    }

    init {
        context("Silhouette") {
            test("prevents damage from a spell that targets the creature") {
                val game = board("Lightning Bolt")
                game.silhouetteOnBears()
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt's damage to the chosen creature is prevented") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("prevents damage from an ability that targets the creature, though another object is the source") {
                val game = board(sorcerer = true)
                game.silhouetteOnBears()
                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val pingId = cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities.first().id
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(game.player1Id, sorcerer, pingId, listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("no damage is marked on the chosen creature") {
                    (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
            }

            test("doesn't prevent damage from a spell that doesn't target it") {
                val game = board("Pyroclasm")
                game.silhouetteOnBears()
                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("Pyroclasm targets nothing, so its damage is dealt") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("the shielded creature's badge names Silhouette and what it prevents") {
                val game = board()
                game.silhouetteOnBears()
                val badge = game.getClientState(1).cards.getValue(game.findPermanent("Grizzly Bears")!!)
                    .activeEffects.single { it.name == "Silhouette" }
                badge.description!! shouldContain "damage from spells and abilities that target it is prevented"
            }

            test("doesn't prevent combat damage") {
                val game = board()
                game.silhouetteOnBears()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the blocker's combat damage isn't caused by a targeting spell or ability") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
