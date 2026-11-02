package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m19.cards.LilianaTheNecromancer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Liliana, the Necromancer (M19 #291).
 *
 * The −7's reanimation is untargeted and chosen after the destruction, so a creature it just
 * destroyed can come back under your control alongside a card from another graveyard; with no
 * targets chosen it still reanimates.
 */
class LilianaTheNecromancerScenarioTest : ScenarioTestBase() {

    private val minusSeven = LilianaTheNecromancer.activatedAbilities[2].id

    init {
        context("Liliana, the Necromancer −7") {

            test("destroys the target, then returns it and another graveyard creature under your control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, the Necromancer")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val liliana = game.findPermanent("Liliana, the Necromancer")!!
                seedLoyalty(game, liliana, 7)
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(game.player1Id, liliana, minusSeven, targets = listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.maxSelections shouldBe 2
                val deadGiant = graveyardCard(game, game.player2Id, "Hill Giant")
                val bears = graveyardCard(game, game.player1Id, "Grizzly Bears")
                decision.options shouldContainAll listOf(deadGiant, bears)

                game.selectCards(listOf(deadGiant, bears)).error shouldBe null
                game.resolveStack()

                val returnedGiant = game.findPermanent("Hill Giant")!!
                val returnedBears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getController(returnedGiant) shouldBe game.player1Id
                game.state.projectedState.getController(returnedBears) shouldBe game.player1Id
            }

            test("with no targets chosen it still puts a creature card onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, the Necromancer")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val liliana = game.findPermanent("Liliana, the Necromancer")!!
                seedLoyalty(game, liliana, 7)

                game.execute(ActivateAbility(game.player1Id, liliana, minusSeven)).error shouldBe null
                game.resolveStack()

                game.selectCards(listOf(graveyardCard(game, game.player2Id, "Hill Giant"))).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getController(giant) shouldBe game.player1Id
            }
        }
    }

    private fun graveyardCard(game: TestGame, owner: EntityId, name: String): EntityId =
        game.state.getZone(ZoneKey(owner, Zone.GRAVEYARD)).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder skips the "enters with its starting loyalty" step, so seed it.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
