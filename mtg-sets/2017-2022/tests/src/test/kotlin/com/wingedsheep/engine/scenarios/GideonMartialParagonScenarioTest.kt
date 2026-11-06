package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gideon, Martial Paragon (AKH #270, {4}{W}, loyalty 5).
 *
 *   +2: Untap all creatures you control. Those creatures get +1/+1 until end of turn.
 *   −10: Creatures you control get +2/+2 until end of turn. Tap all creatures your opponents control.
 *
 * Pins the +2 ruling that every creature you control gets +1/+1, not just the ones it untapped,
 * and that the −10 pumps only your side while tapping only the opponent's.
 */
class GideonMartialParagonScenarioTest : ScenarioTestBase() {

    init {
        test("+2 untaps your creatures and pumps all of them, tapped or not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon, Martial Paragon")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gideon = game.findPermanent("Gideon, Martial Paragon")!!
            setLoyalty(game, gideon, 5)
            activate(game, gideon, index = 0)
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 3
            withClue("ruling: an already-untapped creature still gets +1/+1") {
                game.state.projectedState.getPower(giant) shouldBe 4
                game.state.projectedState.getToughness(giant) shouldBe 4
            }
            withClue("the opponent's creature is neither untapped nor pumped") {
                game.state.getEntity(lions)!!.has<TappedComponent>() shouldBe true
                game.state.projectedState.getPower(lions) shouldBe 1
            }
        }

        test("-10 pumps your creatures and taps the opponent's") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon, Martial Paragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gideon = game.findPermanent("Gideon, Martial Paragon")!!
            setLoyalty(game, gideon, 11)
            activate(game, gideon, index = 2)
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            game.state.projectedState.getPower(lions) shouldBe 1
            game.state.getEntity(lions)!!.has<TappedComponent>() shouldBe true
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Gideon, Martial Paragon")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
        ).error shouldBe null
    }

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
