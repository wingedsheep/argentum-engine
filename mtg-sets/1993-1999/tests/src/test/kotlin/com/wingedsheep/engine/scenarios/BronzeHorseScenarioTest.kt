package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bronze Horse — {7} Artifact Creature — Horse 4/4.
 * "Trample. As long as you control another creature, prevent all damage that would be dealt to
 * this creature by spells that target it."
 *
 * Pinned: a targeting spell's damage is prevented while another creature is around, and dealt
 * when the Horse stands alone; an untargeted spell and an ability that targets it still hurt.
 */
class BronzeHorseScenarioTest : ScenarioTestBase() {

    private fun board(vararg hand: String, companion: Boolean = true, sorcerer: Boolean = false) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Bronze Horse")
        .apply { if (companion) withCardOnBattlefield(1, "Grizzly Bears") }
        .apply { if (sorcerer) withCardOnBattlefield(1, "Prodigal Sorcerer") }
        .withLandsOnBattlefield(1, "Mountain", 2)
        .apply { hand.forEach { withCardInHand(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.horseDamage(): Int =
        state.getEntity(findPermanent("Bronze Horse")!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        context("Bronze Horse") {
            test("prevents damage from a spell that targets it while you control another creature") {
                val game = board("Lightning Bolt")
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Bronze Horse")!!).error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt's damage to the Horse is prevented") {
                    game.horseDamage() shouldBe 0
                }
            }

            test("doesn't prevent it when the Horse is your only creature") {
                val game = board("Lightning Bolt", companion = false)
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Bronze Horse")!!).error shouldBe null
                game.resolveStack()

                withClue("without another creature the shield is off") {
                    game.horseDamage() shouldBe 3
                }
            }

            test("doesn't prevent damage from a spell that doesn't target it") {
                val game = board("Pyroclasm")
                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("Pyroclasm targets nothing, so its damage is dealt") {
                    game.horseDamage() shouldBe 2
                }
            }

            test("doesn't prevent damage from an ability that targets it") {
                val game = board(sorcerer = true)
                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val pingId = cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities.first().id
                val horse = game.findPermanent("Bronze Horse")!!
                game.execute(
                    ActivateAbility(game.player1Id, sorcerer, pingId, listOf(ChosenTarget.Permanent(horse)))
                ).error shouldBe null
                game.resolveStack()

                withClue("an ability isn't a spell, so the ping is dealt") {
                    game.horseDamage() shouldBe 1
                }
            }
        }
    }
}
