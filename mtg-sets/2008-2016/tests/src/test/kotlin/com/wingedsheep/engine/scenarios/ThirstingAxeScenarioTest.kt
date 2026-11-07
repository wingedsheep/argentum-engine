package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thirsting Axe (EMN #202) — {3} Artifact — Equipment.
 *
 *   Equipped creature gets +4/+0.
 *   At the beginning of your end step, if equipped creature didn't deal combat damage to a creature
 *   this turn, sacrifice it.
 *   Equip {2}
 */
class ThirstingAxeScenarioTest : ScenarioTestBase() {

    private val equipAbilityId =
        cardRegistry.getCard("Thirsting Axe")!!.activatedAbilities.first { it.isEquipAbility }.id

    private fun board(axeOn: String? = "Grizzly Bears"): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (axeOn != null) builder.withCardAttachedTo(1, "Thirsting Axe", axeOn)
        else builder.withCardOnBattlefield(1, "Thirsting Axe")
        return builder.build()
    }

    private fun TestGame.attackWithBears(blockedByWall: Boolean) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        if (blockedByWall) {
            declareBlockers(mapOf("Wall of Wood" to listOf("Grizzly Bears"))).error shouldBe null
        } else {
            declareNoBlockers().error shouldBe null
        }
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    private fun TestGame.equip(creature: String) {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Thirsting Axe")!!,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(findPermanent(creature)!!)),
            )
        )
        withClue("Equip should succeed: ${result.error}") { result.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun TestGame.reachEndStep() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
    }

    init {
        test("equipped creature gets +4/+0") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 6
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("combat damage only to a player doesn't count: the equipped creature is sacrificed") {
            val game = board()
            game.attackWithBears(blockedByWall = false)
            game.getLifeTotal(2) shouldBe 14
            game.reachEndStep()
            withClue("Grizzly Bears is sacrificed at your end step") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.isOnBattlefield("Thirsting Axe") shouldBe true
        }

        test("combat damage to a blocking creature keeps the equipped creature") {
            val game = board()
            game.attackWithBears(blockedByWall = true)
            game.isOnBattlefield("Wall of Wood") shouldBe false
            game.reachEndStep()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a creature that didn't attack is sacrificed") {
            val game = board(axeOn = "Hill Giant")
            game.reachEndStep()
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("the check reads the creature equipped as the end step begins, not when the damage was dealt") {
            val game = board(axeOn = null)
            game.attackWithBears(blockedByWall = true)
            game.equip("Grizzly Bears")
            game.reachEndStep()
            withClue("Bears dealt combat damage to a creature before the Axe was attached") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("moving the Axe after combat to a creature that dealt no combat damage sacrifices that one") {
            val game = board()
            game.attackWithBears(blockedByWall = true)
            game.equip("Hill Giant")
            game.reachEndStep()
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("an unattached Axe does nothing") {
            val game = board(axeOn = null)
            game.reachEndStep()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Thirsting Axe") shouldBe true
        }
    }
}
