package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rtr.cards.DeadbridgeGoliath
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scavenge {4}{G}{G}: the card exiles itself as a cost, and the counters equal its power read
 * from exile at resolution.
 */
class DeadbridgeGoliathScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = DeadbridgeGoliath.activatedAbilities.first().id

        test("scavenge exiles the card and puts five +1/+1 counters on the target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Deadbridge Goliath")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val goliath = game.state.getZone(game.player1Id, Zone.GRAVEYARD).first()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = goliath,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, bears)),
                ),
            )
            withClue("${result.error}") { result.error shouldBe null }
            // Exiled as part of the cost, before the ability resolves.
            game.state.getZone(game.player1Id, Zone.GRAVEYARD).contains(goliath) shouldBe false
            game.state.getZone(game.player1Id, Zone.EXILE).contains(goliath) shouldBe true

            game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
        }

        test("scavenge can't be activated at instant speed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Deadbridge Goliath")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val goliath = game.state.getZone(game.player1Id, Zone.GRAVEYARD).first()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = goliath,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, bears)),
                ),
            )
            result.error shouldNotBe null
            game.state.getZone(game.player1Id, Zone.GRAVEYARD).contains(goliath) shouldBe true
        }
    }
}
