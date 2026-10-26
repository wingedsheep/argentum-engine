package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Oko, Thief of Crowns (ELD #197, loyalty 4).
 *
 *   +1: Target artifact or creature loses all abilities and becomes a green Elk creature with
 *       base power and toughness 3/3.
 *   −5: Exchange control of target artifact or creature you control and target creature an
 *       opponent controls with power 3 or less.
 */
class OkoThiefOfCrownsScenarioTest : ScenarioTestBase() {

    private val okoName = "Oko, Thief of Crowns"

    private fun ability(index: Int) = cardRegistry.getCard(okoName)!!.script.activatedAbilities[index]

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun activate(game: TestGame, oko: EntityId, index: Int, targets: List<EntityId>) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = oko,
                abilityId = ability(index).id,
                targets = targets.map { ChosenTarget.Permanent(it) }
            )
        ).error shouldBe null
        game.resolveStack()
    }

    init {
        test("+1 turns a flying creature into a vanilla green 3/3 Elk, permanently") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val angel = game.findPermanent("Serra Angel")!!

            activate(game, oko, 1, listOf(angel))
            loyalty(game, oko) shouldBe 5

            fun assertElk() {
                val projected = game.state.projectedState
                projected.isCreature(angel) shouldBe true
                projected.getPower(angel) shouldBe 3
                projected.getToughness(angel) shouldBe 3
                projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
                projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
                projected.getSubtypes(angel) shouldBe setOf("Elk")
                projected.getColors(angel) shouldBe setOf("GREEN")
            }
            assertElk()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the +1 lasts indefinitely") {
                game.state.activePlayerId shouldBe game.player2Id
                assertElk()
            }
        }

        test("+1 on a noncreature artifact makes it a 3/3 Elk creature that is no longer an artifact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(1, "Millstone")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val millstone = game.findPermanent("Millstone")!!

            activate(game, oko, 1, listOf(millstone))

            val projected = game.state.projectedState
            projected.isCreature(millstone) shouldBe true
            projected.hasType(millstone, "ARTIFACT") shouldBe false
            projected.getPower(millstone) shouldBe 3
            projected.getToughness(millstone) shouldBe 3
        }

        test("−5 exchanges control of your artifact and an opponent's small creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(1, "Millstone")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val millstone = game.findPermanent("Millstone")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(oko) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
            }

            activate(game, oko, 2, listOf(millstone, bears))

            loyalty(game, oko) shouldBe 1
            val projected = game.state.projectedState
            projected.getController(bears) shouldBe game.player1Id
            projected.getController(millstone) shouldBe game.player2Id
        }
    }
}
