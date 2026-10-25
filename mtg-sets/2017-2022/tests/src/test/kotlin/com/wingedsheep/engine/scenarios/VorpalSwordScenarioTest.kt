package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vorpal Sword (AFR #124) — Equipped creature gets +2/+0 and has deathtouch.
 * {5}{B}{B}{B}: Until end of turn, this Equipment gains "Whenever equipped creature deals combat
 * damage to a player, that player loses the game." Equip {B}{B}.
 *
 * The activated ability grants an ATTACHED-bound trigger to the Equipment itself — no other card
 * grants an Equipment its own equipped-creature trigger temporarily, so this proves the granted
 * ability is picked up by the attachment trigger path and that its "that player" is the damaged one.
 */
class VorpalSwordScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Vorpal Sword")!!.script.activatedAbilities[0].id
    }

    private fun board() = scenario()
        .withPlayers("Swordbearer", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardAttachedTo(1, "Vorpal Sword", "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 8)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackUnblocked() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        passPriority()
        resolveStack()
    }

    init {
        context("Vorpal Sword") {

            test("equipped creature gets +2/+0 and deathtouch; unactivated, combat damage is just damage") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true

                game.attackUnblocked()

                game.getLifeTotal(2) shouldBe 16
                withClue("without the activation the Sword has no lose-the-game trigger") {
                    game.state.gameOver shouldBe false
                }
            }

            test("after activating, combat damage to a player makes that player lose the game") {
                val game = board()

                val activation = game.getLegalActions(1).map { it.action }.firstOrNull {
                    it is ActivateAbility && it.abilityId == abilityId
                }
                withClue("the {5}{B}{B}{B} ability is offered with eight Swamps") { activation shouldNotBe null }
                game.execute(activation!!).error shouldBe null
                game.resolveStack()

                game.attackUnblocked()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }
        }
    }
}
