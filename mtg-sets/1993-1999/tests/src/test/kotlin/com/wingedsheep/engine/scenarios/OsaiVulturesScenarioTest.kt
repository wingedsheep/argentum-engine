package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Osai Vultures (LEG #29) — "At the beginning of each end step, if a creature died this turn, put a
 * carrion counter on this creature. Remove two carrion counters from this creature: This creature
 * gets +1/+1 until end of turn."
 */
class OsaiVulturesScenarioTest : ScenarioTestBase() {

    init {
        context("Osai Vultures") {

            fun carrion(game: TestGame): Int =
                game.state.getEntity(game.findPermanent("Osai Vultures")!!)
                    ?.get<CountersComponent>()?.getCount(CounterType.CARRION) ?: 0

            test("gets a carrion counter at any end step, but only when a creature died that turn") {
                val builder = scenario()
                    .withPlayers("Vultures Player", "Opponent")
                    .withCardOnBattlefield(1, "Osai Vultures")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Mountain")
                }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("nothing died on the Vultures player's turn, so no counter") { carrion(game) shouldBe 0 }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("a creature died on the opponent's turn, so its end step adds a counter") {
                    carrion(game) shouldBe 1
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("the death was last turn; this end step adds nothing") { carrion(game) shouldBe 1 }
            }

            test("removing two carrion counters gives +1/+1 until end of turn") {
                val builder = scenario()
                    .withPlayers("Vultures Player", "Opponent")
                    .withCardOnBattlefield(1, "Osai Vultures")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Mountain")
                }
                val game = builder.build()
                val vultures = game.findPermanent("Osai Vultures")!!
                game.state = game.state.updateEntity(vultures) {
                    it.with(CountersComponent(mapOf(CounterType.CARRION to 3)))
                }
                val abilityId = cardRegistry.getCard("Osai Vultures")!!.script.activatedAbilities[0].id
                fun activate() = game.execute(ActivateAbility(game.player1Id, vultures, abilityId))

                activate().error shouldBe null
                game.resolveStack()
                withClue("two counters paid, one left; the Vultures are 2/2") {
                    carrion(game) shouldBe 1
                    game.state.projectedState.getPower(vultures) shouldBe 2
                    game.state.projectedState.getToughness(vultures) shouldBe 2
                }

                withClue("one counter can't pay the cost") { activate().error shouldNotBe null }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the pump ends with the turn") {
                    game.state.projectedState.getPower(vultures) shouldBe 1
                    game.state.projectedState.getToughness(vultures) shouldBe 1
                }
            }
        }
    }
}
