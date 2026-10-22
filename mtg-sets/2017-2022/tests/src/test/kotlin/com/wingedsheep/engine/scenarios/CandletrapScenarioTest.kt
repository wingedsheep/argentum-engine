package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Candletrap (MID #9) — {W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature has defender.
 *   Prevent all combat damage that would be dealt by enchanted creature.
 *   Coven — {2}{W}, Sacrifice this Aura: Exile enchanted creature. Activate only if you control
 *   three or more creatures with different powers.
 *
 * The risky bit: the Aura is sacrificed as a cost, so "enchanted creature" must still resolve
 * to the creature it was attached to after the Aura has left the battlefield.
 */
class CandletrapScenarioTest : ScenarioTestBase() {

    private val exileAbilityId =
        cardRegistry.getCard("Candletrap")!!.activatedAbilities.first().id

    init {
        context("Candletrap") {

            test("coven ability can't be activated without three different powers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Candletrap")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Candletrap", targetId = giant)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val aura = game.findPermanent("Candletrap")
                aura.shouldNotBeNull()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura,
                        abilityId = exileAbilityId,
                    )
                )
                withClue("Only powers 1 and 2 are controlled; coven should block activation") {
                    activation.error shouldNotBe null
                }
                game.isOnBattlefield("Candletrap") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("with coven, sacrificing Candletrap exiles the creature it enchanted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Candletrap")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opposingBears = game.state.getBattlefield()
                    .first { id -> game.state.projectedState.getController(id) == game.player2Id }
                val cast = game.castSpell(1, "Candletrap", targetId = opposingBears)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val aura = game.findPermanent("Candletrap")
                aura.shouldNotBeNull()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura,
                        abilityId = exileAbilityId,
                    )
                )
                withClue("Activation should succeed: ${activation.error}") { activation.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()

                // Sacrificed as a cost, before the ability resolves.
                game.state.getGraveyard(game.player1Id).contains(aura) shouldBe true

                game.resolveStack()

                game.state.getExile(game.player2Id).contains(opposingBears) shouldBe true
                withClue("Player1's own Grizzly Bears must stay put") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
