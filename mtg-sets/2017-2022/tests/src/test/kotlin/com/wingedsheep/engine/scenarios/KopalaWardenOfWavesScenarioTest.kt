package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kopala, Warden of Waves (Ixalan, {1}{U}{U} Legendary Creature — Merfolk Wizard).
 *
 *   "Spells your opponents cast that target a Merfolk you control cost {2} more to cast.
 *    Abilities your opponents activate that target a Merfolk you control cost {2} more to activate."
 */
class KopalaWardenOfWavesScenarioTest : ScenarioTestBase() {

    init {
        fun board(spell: String? = null, sorcerer: Boolean = false, mountains: Int) = scenario()
            .withPlayers("Kopala", "Opponent")
            .withCardOnBattlefield(1, "Kopala, Warden of Waves")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .let { if (spell != null) it.withCardInHand(2, spell) else it }
            .let { if (sorcerer) it.withCardOnBattlefield(2, "Prodigal Sorcerer") else it }
            .withLandsOnBattlefield(2, "Mountain", mountains)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("an opponent's Shock targeting Kopala costs {2} more") {
            val short = board(spell = "Shock", mountains = 2)
            withClue("two Mountains can't pay {2}{R}") {
                short.castSpell(2, "Shock", short.findPermanent("Kopala, Warden of Waves")!!).error shouldNotBe null
            }

            val game = board(spell = "Shock", mountains = 3)
            val kopala = game.findPermanent("Kopala, Warden of Waves")!!
            game.castSpell(2, "Shock", kopala).error shouldBe null
            game.resolveStack()
            game.findPermanent("Kopala, Warden of Waves") shouldBe null
        }

        test("an opponent's Shock targeting a non-Merfolk costs its printed {R}") {
            val game = board(spell = "Shock", mountains = 1)
            game.castSpell(2, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
        }

        test("an opponent's {T} ability targeting Kopala gains a {2} cost") {
            fun ping(game: TestGame, targetName: String) = game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = game.findPermanent("Prodigal Sorcerer")!!,
                    abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent(targetName)!!)),
                )
            ).also { if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay() }

            withClue("no mana: the taxed {2}, {T} can't be paid") {
                ping(board(sorcerer = true, mountains = 0), "Kopala, Warden of Waves").error shouldNotBe null
            }
            withClue("the untaxed {T} still pings a non-Merfolk") {
                ping(board(sorcerer = true, mountains = 0), "Grizzly Bears").error shouldBe null
            }
            ping(board(sorcerer = true, mountains = 2), "Kopala, Warden of Waves").error shouldBe null
        }
    }
}
