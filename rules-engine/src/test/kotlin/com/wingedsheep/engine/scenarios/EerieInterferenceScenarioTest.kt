package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eerie Interference (WOE #12) — {2}{W} Instant.
 * "Prevent all damage that would be dealt to you and creatures you control this turn by creatures."
 *
 * Pins the two things that make this card more than a Fog, both of which needed the recipient-group
 * shield to grow: the recipient set includes *you* (a player, which a `GroupFilter` can never match
 * — [com.wingedsheep.sdk.scripting.effects.PreventDamageEffect.recipientGroupIncludesController]),
 * and the shield is narrowed to creature sources, so a burn spell still connects.
 *
 * Prodigal Sorcerer ("{T}: deals 1 damage to any target") is the creature source and Lightning Bolt
 * the noncreature control, which also proves the shield is not combat-only.
 */
class EerieInterferenceScenarioTest : ScenarioTestBase() {

    private val pingAbilityId by lazy {
        cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id
    }

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Eerie Interference")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.autoPayIfAsked() {
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        }

        fun TestGame.ping(target: ChosenTarget) {
            val result = execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = findPermanent("Prodigal Sorcerer")!!,
                    abilityId = pingAbilityId,
                    targets = listOf(target)
                )
            )
            withClue("ping activation failed: ${result.error}") { result.error shouldBe null }
            autoPayIfAsked()
            resolveStack()
        }

        fun TestGame.markedDamage(id: EntityId): Int =
            state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        fun TestGame.castInterference() {
            withClue("cast should succeed") {
                castSpell(1, "Eerie Interference").error shouldBe null
            }
            resolveStack()
        }

        test("prevents a creature's noncombat damage to you (the player)") {
            val game = board()
            game.castInterference()

            val before = game.getLifeTotal(1)
            game.ping(ChosenTarget.Player(game.player1Id))
            withClue("\"dealt to you ... by creatures\" — the player is a protected recipient") {
                game.getLifeTotal(1) shouldBe before
            }
        }

        test("prevents a creature's noncombat damage to a creature you control") {
            val game = board()
            game.castInterference()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.ping(ChosenTarget.Permanent(bears))
            game.markedDamage(bears) shouldBe 0
        }

        test("does not prevent damage from a noncreature source") {
            val game = board()
            game.castInterference()

            val before = game.getLifeTotal(1)
            withClue("bolt should be castable") {
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            }
            game.resolveStack()
            withClue("Lightning Bolt is not a creature source — the shield must not swallow it") {
                game.getLifeTotal(1) shouldBe before - 3
            }
        }

        test("does not protect the opponent's creatures from your creature") {
            val game = board()
            game.castInterference()

            val theirs = game.findPermanent("Centaur Courser")!!
            game.ping(ChosenTarget.Permanent(theirs))
            withClue("only you and your creatures are shielded") {
                game.markedDamage(theirs) shouldBe 1
            }
        }
    }
}
