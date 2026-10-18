package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenPlayer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Takklemaggot — {2}{B}{B} Enchantment — Aura. Enchant creature.
 * At the beginning of the upkeep of enchanted creature's controller, put a -0/-1 counter on that
 * creature. When enchanted creature dies, that creature's controller chooses a creature that this
 * card could enchant. If the player does, return this card to the battlefield under your control
 * attached to that creature. If they don't, return it under your control as a non-Aura enchantment
 * that loses "enchant creature" and gains "At the beginning of that player's upkeep, this
 * enchantment deals 1 damage to that player."
 *
 * Rulings pinned: the dead creature's controller chooses (not the Aura's controller), among any
 * player's creatures, untargeted but only ones it could legally enchant (no protection from black);
 * the Aura's controller keeps it; with no legal creature it returns as a non-Aura that hurts the
 * chooser — who may be the Aura's own controller.
 */
class TakklemaggotScenarioTest : ScenarioTestBase() {

    /**
     * Player 1's Takklemaggot enchants [victimPlayer]'s Savannah Lions; the other player is active
     * in their end step, so passing reaches the Lions controller's upkeep, where the -0/-1 counter
     * kills the 1/1 and the dies trigger resolves up to the host choice.
     */
    private fun killLions(victimPlayer: Int, extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(victimPlayer, "Savannah Lions")
            .withCardAttachedTo(1, "Takklemaggot", "Savannah Lions")
            .extra()
            .apply { repeat(3) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(3 - victimPlayer)
            .inPhase(Phase.ENDING, Step.END)
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        withClue("the -0/-1 counter killed the Lions") { game.isInGraveyard(victimPlayer, "Savannah Lions") shouldBe true }
        return game
    }

    init {
        context("Takklemaggot") {
            test("the dead creature's controller chooses any player's creature; it returns attached, under your control") {
                val game = killLions(2) {
                    withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                }
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the Lions' controller makes the choice, not the Aura's controller") {
                    decision.playerId shouldBe game.player2Id
                }
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears))

                val takk = game.findPermanent("Takklemaggot")!!
                game.state.getEntity(takk)?.get<AttachedToComponent>()?.targetId shouldBe bears
                game.state.projectedState.getController(takk) shouldBe game.player1Id
                game.state.projectedState.hasSubtype(takk, "Aura") shouldBe true
            }

            test("a creature with protection from black can't be chosen") {
                val game = killLions(2) {
                    withCardOnBattlefield(1, "Voice of Grace").withCardOnBattlefield(2, "Hill Giant")
                }
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.findPermanent("Hill Giant")!!)
            }

            test("with no creature it could enchant, it returns as a non-Aura that hurts the chooser each upkeep") {
                val game = killLions(2) { withCardOnBattlefield(1, "Voice of Grace") }
                withClue("nothing to choose, so no prompt") { game.hasPendingDecision() shouldBe false }

                val takk = game.findPermanent("Takklemaggot")
                withClue("it is back, an unattached non-Aura enchantment under my control") {
                    takk shouldNotBe null
                    game.state.getEntity(takk!!)?.get<AttachedToComponent>() shouldBe null
                    game.state.projectedState.hasSubtype(takk, "Aura") shouldBe false
                    game.state.projectedState.hasType(takk, "ENCHANTMENT") shouldBe true
                    game.state.projectedState.getController(takk) shouldBe game.player1Id
                    game.state.getEntity(takk)?.chosenPlayer() shouldBe game.player2Id
                }
                game.checkStateBasedActions()
                withClue("the unattached-Aura rule doesn't bury a non-Aura") { game.isOnBattlefield("Takklemaggot") shouldBe true }

                // My turn: my upkeep is not "that player's".
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.state.stack.size shouldBe 0
                game.getLifeTotal(1) shouldBe 20

                // Player 2's next upkeep: 1 damage to player 2.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 20
            }

            test("when its controller's own creature dies with no new host, that player is its controller") {
                val game = killLions(1)
                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("Takklemaggot") shouldBe true

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.state.stack.size shouldBe 0

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 19
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
