package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Player-level protection — the **D**amage half of DEBT (CR 702.16e): a player carrying a
 * PlayerProtectionComponent (The One Ring's "protection from everything until your next turn")
 * has damage from a matching source prevented, not just targeting blocked.
 *
 * The targeting half is exercised elsewhere (a protected player can't be the target of a spell);
 * this guards the damage-prevention branch in DamageUtils against a *non-targeted* source, where
 * the targeting block never runs. "Deal 3 damage to each player" hits both players from one
 * source: the protected player takes none (prevented — protection from everything covers any
 * source, including its controller's own per CR 702.16e), while the unprotected opponent takes
 * the full 3, proving the source really deals damage and only the protected player is spared.
 *
 * Inline cards, no set dependency.
 */
class PlayerProtectionDamageScenarioTest : ScenarioTestBase() {

    // "You get protection from everything until your next turn." (The One Ring's grant, on its own.)
    private val grantProtection = card("Grant Player Protection") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.GrantPlayerProtection() }
    }

    // "Deal 3 damage to each player." A non-targeted symmetric damage source.
    private val burnEachPlayer = card("Burn Each Player") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.Each)) }
    }

    init {
        cardRegistry.register(grantProtection)
        cardRegistry.register(burnEachPlayer)

        context("player protection prevents non-targeted damage") {

            test("a player with protection from everything takes no damage while the unprotected opponent does") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grant Player Protection")
                    .withCardInHand(1, "Burn Each Player")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p1Before = game.getLifeTotal(1)
                val p2Before = game.getLifeTotal(2)

                // Player 1 gains protection from everything until their next turn.
                game.castSpell(1, "Grant Player Protection")
                game.resolveStack()

                // The same player resolves a "deal 3 to each player" source at sorcery speed.
                game.castSpell(1, "Burn Each Player")
                game.resolveStack()

                withClue("Protected player takes no damage — the matching source's damage is prevented") {
                    game.getLifeTotal(1) shouldBe p1Before
                }
                withClue("Unprotected opponent still takes the full 3 from the same source") {
                    game.getLifeTotal(2) shouldBe p2Before - 3
                }
            }
        }

        context("player protection prevents combat damage") {

            // The reported bug: The One Ring grants "protection from everything", yet the protected
            // player still took combat damage. Combat damage is applied by CombatDamageManager (not
            // DamageUtils.dealDamageToTarget), so the prevention lives in PlayerProtectionModifier.
            test("a creature can attack a protected player, but its combat damage is prevented") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // The defending player carries The One Ring's protection from everything.
                game.state = game.state.updateEntity(game.player1Id) { c ->
                    c.with(PlayerProtectionComponent.of(listOf(ProtectionScope.Everything)))
                }

                val defenderBefore = game.getLifeTotal(1)

                // Creatures can still attack a protected player (CR 702.16e ruling).
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("protected player's life is unchanged — the combat damage is prevented") {
                    game.getLifeTotal(1) shouldBe defenderBefore
                }
            }

            test("control: without protection the same attack deals its 2 combat damage") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val defenderBefore = game.getLifeTotal(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("unprotected player takes Grizzly Bears' 2 combat damage") {
                    game.getLifeTotal(1) shouldBe defenderBefore - 2
                }
            }
        }
    }
}
