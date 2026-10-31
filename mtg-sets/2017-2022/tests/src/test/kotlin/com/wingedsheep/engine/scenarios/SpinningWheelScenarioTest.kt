package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Spinning Wheel (ELD #234) — {3} Artifact.
 *
 * "{T}: Add one mana of any color. / {5}, {T}: Tap target creature."
 */
class SpinningWheelScenarioTest : ScenarioTestBase() {

    init {
        context("Spinning Wheel") {

            test("taps for one mana of the chosen color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Spinning Wheel")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wheel = game.findPermanent("Spinning Wheel")!!
                val manaAbility = cardRegistry.getCard("Spinning Wheel")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wheel,
                        abilityId = manaAbility,
                        manaColorChoice = Color.BLUE
                    )
                ).error shouldBe null

                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.blue shouldBe 1
                game.state.getEntity(wheel)!!.has<TappedComponent>() shouldBe true
            }

            test("{5}, {T}: taps target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Spinning Wheel")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wheel = game.findPermanent("Spinning Wheel")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val tapAbility = cardRegistry.getCard("Spinning Wheel")!!.activatedAbilities[1].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wheel,
                        abilityId = tapAbility,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the opponent's creature is tapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the Wheel tapped as part of the cost") {
                    game.state.getEntity(wheel)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
