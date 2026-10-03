package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MoggMob
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mogg Mob (MH3 #127): "Sacrifice this creature: It deals 3 damage divided as you choose among
 * one, two, or three targets."
 */
class MoggMobScenarioTest : ScenarioTestBase() {

    private val ability = MoggMob.activatedAbilities[0].id

    init {
        context("Mogg Mob") {

            test("offers a 3-damage division among up to three targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mogg Mob")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.getLegalActions(1).single { (it.action as? ActivateAbility)?.abilityId == ability }
                action.requiresDamageDistribution shouldBe true
                action.totalDamageToDistribute shouldBe 3
                action.targetCount shouldBe 3
            }

            test("sacrifice: the announced division is dealt to a creature and a player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mogg Mob")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mob = game.findPermanent("Mogg Mob")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val startLife = game.getLifeTotal(2)

                game.execute(
                    ActivateAbility(
                        game.player1Id, mob, ability,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player2Id)),
                        damageDistribution = mapOf(bears to 2, game.player2Id to 1)
                    )
                ).error shouldBe null

                withClue("the Mob is sacrificed as the cost") {
                    game.findPermanent("Mogg Mob") shouldBe null
                }
                game.resolveStack()

                withClue("2 kills the bears, 1 hits the opponent, though the source is gone") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe startLife - 1
                }
            }

            test("a single target takes all 3 damage; a division not totalling 3 is rejected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mogg Mob")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mob = game.findPermanent("Mogg Mob")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val startLife = game.getLifeTotal(2)

                game.execute(
                    ActivateAbility(
                        game.player1Id, mob, ability,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player2Id)),
                        damageDistribution = mapOf(bears to 2, game.player2Id to 2)
                    )
                ).error shouldNotBe null
                game.findPermanent("Mogg Mob") shouldBe mob

                game.execute(
                    ActivateAbility(game.player1Id, mob, ability, targets = listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe startLife - 3
                game.findPermanent("Grizzly Bears") shouldBe bears
            }
        }
    }
}
