package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Glittering Stockpile (SNC #107) — {2}{R} Artifact — Treasure.
 *
 *   {T}: Add {R}. Put a stash counter on this artifact.
 *   {T}, Sacrifice this artifact: Add X mana of any one color, where X is the number of stash
 *   counters on this artifact.
 *
 * Covers the counter-building mana ability and the cash-out, whose X must read the stash counters
 * the Stockpile had as it was sacrificed (last-known information).
 */
class GlitteringStockpileScenarioTest : ScenarioTestBase() {

    private fun abilityIds() =
        cardRegistry.requireCard("Glittering Stockpile").script.activatedAbilities.map { it.id }

    private fun newGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Glittering Stockpile")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("tapping adds {R} and puts a stash counter on it") {
            val game = newGame()
            val stockpile = game.findPermanent("Glittering Stockpile").shouldNotBeNull()

            game.execute(ActivateAbility(game.player1Id, stockpile, abilityIds()[0])).error shouldBe null

            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
            pool?.getAmount(Color.RED) shouldBe 1
            withClue("one stash counter after one activation") {
                game.state.getEntity(stockpile)?.get<CountersComponent>()?.getCount(CounterType.STASH) shouldBe 1
            }
        }

        test("sacrificing it adds X mana of one chosen color, X = its stash counters") {
            val game = newGame()
            val stockpile = game.findPermanent("Glittering Stockpile").shouldNotBeNull()
            game.state = game.state.updateEntity(stockpile) { c ->
                c.with(CountersComponent(mapOf(CounterType.STASH to 3)))
            }

            game.execute(
                ActivateAbility(game.player1Id, stockpile, abilityIds()[1], manaColorChoice = Color.GREEN)
            ).error shouldBe null

            withClue("the Stockpile was sacrificed as part of the cost") {
                game.isInGraveyard(1, "Glittering Stockpile") shouldBe true
            }
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
            withClue("three stash counters on the sacrificed Stockpile yield three green mana") {
                pool?.getAmount(Color.GREEN) shouldBe 3
            }
        }
    }
}
