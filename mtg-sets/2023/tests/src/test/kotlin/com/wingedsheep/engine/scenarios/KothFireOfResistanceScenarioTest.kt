package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Koth, Fire of Resistance (ONE #138, {2}{R}{R}, Loyalty 4).
 *
 *   +2: Search your library for a basic Mountain card, reveal it, put it into your hand, then shuffle.
 *   −3: Koth deals damage to target creature equal to the number of Mountains you control.
 *   −7: You get an emblem with "Whenever a Mountain you control enters, this emblem deals 4 damage
 *       to any target."
 */
class KothFireOfResistanceScenarioTest : ScenarioTestBase() {

    init {
        test("+2 finds only a basic Mountain and puts it into hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Koth, Fire of Resistance")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val koth = game.findPermanent("Koth, Fire of Resistance")!!
            setLoyalty(game, koth, 4)
            val mountain = game.findCardsInLibrary(1, "Mountain").single()

            activate(game, koth, index = 0)
            game.resolveStack()

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the Forest isn't a basic Mountain") {
                decision.options shouldBe listOf(mountain)
            }
            game.selectCards(listOf(mountain))
            game.resolveStack()

            game.isInHand(1, "Mountain") shouldBe true
            game.isInHand(1, "Forest") shouldBe false
            loyalty(game, koth) shouldBe 6
        }

        test("−3 deals damage equal to the Mountains you control, ignoring other lands") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Koth, Fire of Resistance")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val koth = game.findPermanent("Koth, Fire of Resistance")!!
            setLoyalty(game, koth, 4)
            val wurm = game.findPermanent("Craw Wurm")!!

            activate(game, koth, index = 1, targets = listOf(ChosenTarget.Permanent(wurm)))
            game.resolveStack()

            withClue("three Mountains you control → 3 damage to the 6/4") {
                game.state.getEntity(wurm)!!.get<DamageComponent>()!!.amount shouldBe 3
            }
            loyalty(game, koth) shouldBe 1
        }

        test("−7 emblem outlives Koth and deals 4 when a Mountain you control enters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Koth, Fire of Resistance")
                .withCardInHand(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val koth = game.findPermanent("Koth, Fire of Resistance")!!
            setLoyalty(game, koth, 7)

            activate(game, koth, index = 2)
            game.resolveStack()

            withClue("Koth died at 0 loyalty; the emblem is global") {
                game.findPermanent("Koth, Fire of Resistance") shouldBe null
                game.state.globalGrantedTriggeredAbilities.size shouldBe 1
            }

            val mountain = game.findCardsInHand(1, "Mountain").single()
            game.execute(PlayLand(game.player1Id, mountain)).error shouldBe null
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Koth, Fire of Resistance")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
