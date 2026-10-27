package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class AscendantSpiritScenarioTest : ScenarioTestBase() {
    private val abilities = cardRegistry.getCard("Ascendant Spirit")!!.activatedAbilities
    private val warriorAbility = abilities[0].id
    private val angelAbility = abilities[1].id
    private val drawAbility = abilities[2].id

    private fun board(snowLands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ascendant Spirit")
        .withLandsOnBattlefield(1, "Snow-Covered Island", snowLands)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate(abilityId: AbilityId) {
        execute(ActivateAbility(player1Id, findPermanent("Ascendant Spirit")!!, abilityId)).error shouldBe null
        resolveStack()
    }

    init {
        test("upgrade abilities do nothing when the creature lacks the required type") {
            val game = board(snowLands = 7)
            val spirit = game.findPermanent("Ascendant Spirit")!!

            game.activate(angelAbility)
            game.activate(drawAbility)

            val projected = game.state.projectedState
            projected.getSubtypes(spirit) shouldBe setOf("Spirit")
            projected.getPower(spirit) shouldBe 1
            projected.getToughness(spirit) shouldBe 1
            game.state.getEntity(spirit)?.get<CountersComponent>() shouldBe null
        }

        test("full chain ends as a flying Angel whose stacked draw triggers fire separately") {
            val game = board(snowLands = 13)
            val spirit = game.findPermanent("Ascendant Spirit")!!

            game.activate(warriorAbility)
            withClue("first ability: Spirit Warrior 2/3") {
                game.state.projectedState.getSubtypes(spirit) shouldBe setOf("Spirit", "Warrior")
                game.state.projectedState.getPower(spirit) shouldBe 2
                game.state.projectedState.getToughness(spirit) shouldBe 3
            }

            game.activate(angelAbility)
            withClue("second ability: flying Spirit Warrior Angel 4/4") {
                game.state.projectedState.getSubtypes(spirit) shouldBe setOf("Spirit", "Warrior", "Angel")
                game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
                game.state.projectedState.getPower(spirit) shouldBe 4
                game.state.projectedState.getToughness(spirit) shouldBe 4
            }

            game.activate(drawAbility)
            game.activate(drawAbility)
            withClue("third ability twice: four +1/+1 counters on a 4/4 base") {
                game.state.getEntity(spirit)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
                game.state.projectedState.getPower(spirit) shouldBe 8
                game.state.projectedState.getToughness(spirit) shouldBe 8
            }

            val handBefore = game.state.getHand(game.player1Id).size
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Ascendant Spirit" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("both granted instances trigger on combat damage to a player") {
                game.getLifeTotal(2) shouldBe 12
                game.state.getHand(game.player1Id).size shouldBe handBefore + 2
            }
        }
    }
}
