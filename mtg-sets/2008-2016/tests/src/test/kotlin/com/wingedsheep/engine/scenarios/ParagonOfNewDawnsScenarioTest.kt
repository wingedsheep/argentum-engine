package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.ParagonOfNewDawns
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Paragon of New Dawns: other creatures you control of its colour get +1/+1, and its tap ability grants
 * vigilance to another target creature of that colour you control.
 */
class ParagonOfNewDawnsScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Paragon of New Dawns")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("only your other creatures of its colour get +1/+1") {
            val game = board()
            val projected = game.state.projectedState
            val (mine, theirs) = game.findPermanents("Savannah Lions").partition {
                game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
            }
            val base = projected.getPower(theirs.single())!!
            withClue("your Savannah Lions is pumped, the opponent's is not") {
                projected.getPower(mine.single()) shouldBe base + 1
            }
            withClue("the off-colour creature and the Paragon itself are unchanged") {
                projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
                projected.getPower(game.findPermanent("Paragon of New Dawns")!!) shouldBe 2
            }
        }

        test("tap ability grants vigilance to another creature of its colour, not an off-colour one") {
            val game = board()
            val paragon = game.findPermanent("Paragon of New Dawns")!!
            val mine = game.findPermanents("Savannah Lions").single {
                game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
            }
            val abilityId = ParagonOfNewDawns.activatedAbilities.single().id

            withClue("an off-colour creature is not a legal target") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = paragon, abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                    )
                ).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = paragon, abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(mine))
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(mine, Keyword.VIGILANCE) shouldBe true
        }
    }
}
