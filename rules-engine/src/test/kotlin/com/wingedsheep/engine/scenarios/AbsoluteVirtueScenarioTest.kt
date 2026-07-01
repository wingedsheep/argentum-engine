package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.AbsoluteVirtue
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Absolute Virtue (FIN) — {6}{W}{U} Legendary Creature 8/8.
 *
 * "This spell can't be countered. / Flying / You have protection from each of your opponents."
 *
 * The protection clause is the continuous, static player-level grant
 * ([com.wingedsheep.sdk.scripting.GrantProtectionToController] with
 * [com.wingedsheep.sdk.scripting.ProtectionScope.EachOpponent]): while Absolute Virtue is on the
 * battlefield its controller can't be dealt damage by, nor targeted by, sources their opponents
 * control. Crucially it's "each opponent", not "everything" — the controller's own sources still
 * affect them — and it ends the moment the permanent leaves the battlefield.
 */
class AbsoluteVirtueScenarioTest : ScenarioTestBase() {

    // "Deal 3 damage to each player." A non-targeted symmetric damage source.
    private val burnEachPlayer = card("Burn Each Player") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.Each)) }
    }

    init {
        cardRegistry.register(AbsoluteVirtue)
        cardRegistry.register(burnEachPlayer)

        context("protection from each opponent — damage") {

            test("an opponent's source can't damage the protected controller, but still hits the opponent") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Absolute Virtue", summoningSickness = false)
                    .withCardInHand(2, "Burn Each Player")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val controllerBefore = game.getLifeTotal(1)
                val opponentBefore = game.getLifeTotal(2)

                game.castSpell(2, "Burn Each Player")
                game.resolveStack()

                withClue("protected controller takes no damage from the opponent's source") {
                    game.getLifeTotal(1) shouldBe controllerBefore
                }
                withClue("the opponent (source's controller) still takes the full 3") {
                    game.getLifeTotal(2) shouldBe opponentBefore - 3
                }
            }

            test("control: the controller's OWN source still damages them (each opponent, not everything)") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Absolute Virtue", summoningSickness = false)
                    .withCardInHand(1, "Burn Each Player")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val controllerBefore = game.getLifeTotal(1)

                game.castSpell(1, "Burn Each Player")
                game.resolveStack()

                withClue("protection is only from opponents' sources — the controller's own burn still hits") {
                    game.getLifeTotal(1) shouldBe controllerBefore - 3
                }
            }
        }

        context("protection from each opponent — combat") {

            test("an opponent's creature can attack the protected controller, but its combat damage is prevented") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Absolute Virtue", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val controllerBefore = game.getLifeTotal(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("protected controller's life is unchanged — the combat damage is prevented") {
                    game.getLifeTotal(1) shouldBe controllerBefore
                }
            }
        }

        context("protection from each opponent — targeting source-of-truth") {

            test("the controller is protected from an opponent's source but not their own") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Absolute Virtue", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val virtueId = game.findPermanent("Absolute Virtue")!!
                val opponentSource = game.findPermanent("Grizzly Bears")!!

                withClue("opponent-controlled source is blocked against the protected controller") {
                    PlayerProtectionRules.isProtectedFromSource(
                        game.state, game.player1Id, opponentSource, game.player2Id
                    ) shouldBe true
                }
                withClue("the controller's own source (Absolute Virtue) is not blocked") {
                    PlayerProtectionRules.isProtectedFromSource(
                        game.state, game.player1Id, virtueId, game.player1Id
                    ) shouldBe false
                }
            }

            test("control: with no Absolute Virtue on the battlefield, the controller has no protection") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentSource = game.findPermanent("Grizzly Bears")!!

                withClue("the protection is sourced from the permanent — absent it, none applies") {
                    PlayerProtectionRules.isProtectedFromSource(
                        game.state, game.player1Id, opponentSource, game.player2Id
                    ) shouldBe false
                }
            }
        }
    }
}
