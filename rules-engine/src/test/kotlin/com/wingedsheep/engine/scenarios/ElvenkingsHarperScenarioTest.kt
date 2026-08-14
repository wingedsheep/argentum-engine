package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elvenking's Harper (HOB #38) — {1}{U} Creature — Elf Bard 2/2.
 * "{4}{U}: Target creature can't be blocked this turn."
 *
 * The unblockable grant is checked where it matters — at declare blockers, where a block that
 * would otherwise be legal has to be rejected.
 */
class ElvenkingsHarperScenarioTest : ScenarioTestBase() {

    init {
        context("Elvenking's Harper") {

            test("the ability makes the target unblockable, and the block is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvenking's Harper")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harper = game.findPermanent("Elvenking's Harper")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val unblockable = cardRegistry.requireCard("Elvenking's Harper").activatedAbilities.single().id

                game.state.projectedState.hasKeyword(courser, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = harper, abilityId = unblockable,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the target picked up the can't-be-blocked flag") {
                    game.state.projectedState.hasKeyword(courser, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("a 2/2 blocking a 3/3 is normally legal — the grant is what forbids it") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                        .error shouldNotBe null
                }
            }

            test("without the ability the same block is legal (control)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                    .error shouldBe null
            }

            test("the ability is unaffordable on four lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvenking's Harper")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harper = game.findPermanent("Elvenking's Harper")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val unblockable = cardRegistry.requireCard("Elvenking's Harper").activatedAbilities.single().id

                withClue("{4}{U} needs five mana") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id, sourceId = harper, abilityId = unblockable,
                            targets = listOf(ChosenTarget.Permanent(courser))
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
