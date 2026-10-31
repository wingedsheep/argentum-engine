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
 * Scenario tests for Oko, the Trickster (ELD #309, loyalty 4).
 *
 *   +1: Put two +1/+1 counters on up to one target creature you control.
 *   0: Until end of turn, Oko becomes a copy of target creature you control. Prevent all damage
 *      that would be dealt to him this turn.
 *   −7: Until end of turn, each creature you control has base power and toughness 10/10 and
 *       gains trample.
 *
 * The 0 is the interesting one: a planeswalker becoming a copy of a creature stops being a
 * planeswalker (keeping his loyalty counters), is shielded from damage, and reverts at cleanup.
 */
class OkoTheTricksterScenarioTest : ScenarioTestBase() {

    private val okoName = "Oko, the Trickster"

    private fun ability(index: Int) = cardRegistry.getCard(okoName)!!.script.activatedAbilities[index]

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

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
        test("+1 puts two +1/+1 counters on the target, and may target nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, oko, 0, listOf(bears))
            loyalty(game, oko) shouldBe 5
            plusOnes(game, bears) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 4
        }

        test("+1 with no target still adds loyalty") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!

            activate(game, oko, 0, emptyList())
            loyalty(game, oko) shouldBe 5
        }

        test("0 makes Oko a copy of the creature, shields him from damage, and reverts at end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, oko, 1, listOf(bears))
            loyalty(game, oko) shouldBe 4

            val projected = game.state.projectedState
            projected.isCreature(oko) shouldBe true
            projected.isPlaneswalker(oko) shouldBe false
            projected.getPower(oko) shouldBe 2
            projected.getToughness(oko) shouldBe 2

            game.castSpell(1, "Lightning Bolt", oko).error shouldBe null
            game.resolveStack()
            withClue("3 damage to a 2/2 copy is prevented, so Oko survives") {
                game.state.getBattlefield().contains(oko) shouldBe true
                loyalty(game, oko) shouldBe 4
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the copy wears off at cleanup") {
                val after = game.state.projectedState
                after.isPlaneswalker(oko) shouldBe true
                after.isCreature(oko) shouldBe false
                loyalty(game, oko) shouldBe 4
            }
        }

        test("−7 makes each creature you control a 10/10 trampler; counters still apply on top") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, okoName)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oko = game.findPermanent(okoName)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val angel = game.findPermanent("Serra Angel")!!
            game.state = game.state.updateEntity(oko) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 8))
            }
            game.state = game.state.updateEntity(bears) { c ->
                c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            activate(game, oko, 2, emptyList())

            loyalty(game, oko) shouldBe 1
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 11
            projected.getToughness(bears) shouldBe 11
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            withClue("an opponent's creature is untouched") {
                projected.getPower(angel) shouldBe 4
                projected.hasKeyword(angel, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
