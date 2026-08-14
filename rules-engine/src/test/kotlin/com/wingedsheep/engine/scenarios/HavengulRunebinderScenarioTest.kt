package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.HavengulRunebinder
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Havengul Runebinder (DKA #39) — "Create a 2/2 black Zombie creature token, **then** put a +1/+1
 * counter on each Zombie creature you control." The ordering matters: the token exists by the time
 * counters are handed out, so it gets one too and ends as a 3/3.
 */
class HavengulRunebinderScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = HavengulRunebinder.activatedAbilities.first().id

        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("creates a Zombie and pumps every Zombie you control, the new token included") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Havengul Runebinder", summoningSickness = false)
                .withCardOnBattlefield(1, "Diregraf Ghoul")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val runebinder = game.findPermanent("Havengul Runebinder")!!
            val bfBefore = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).size

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = runebinder,
                    abilityId = abilityId,
                ),
            )
            withClue("${result.error}") { result.error shouldBe null }

            repeat(4) {
                val d = game.state.pendingDecision as? SelectCardsDecision ?: return@repeat
                game.selectCards(d.options.take(d.minSelections))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("exactly one token was created") {
                game.state.getZone(game.player1Id, Zone.BATTLEFIELD).size shouldBe bfBefore + 1
            }
            withClue("the creature card was exiled from the graveyard as a cost") {
                game.state.getZone(game.player1Id, Zone.GRAVEYARD).isEmpty() shouldBe true
                game.isInExile(1, "Hill Giant") shouldBe true
            }

            val ghoul = game.findPermanent("Diregraf Ghoul")
            ghoul shouldNotBe null
            counters(game, ghoul!!) shouldBe 1

            val token = game.findPermanent("Zombie Token")
            token shouldNotBe null
            withClue("the token is a Zombie you control by the time counters land, so it is 3/3") {
                counters(game, token!!) shouldBe 1
                game.state.projectedState.getPower(token) shouldBe 3
                game.state.projectedState.getToughness(token) shouldBe 3
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("a non-Zombie creature you control is untouched") {
                counters(game, bears) shouldBe 0
            }
        }
    }
}
