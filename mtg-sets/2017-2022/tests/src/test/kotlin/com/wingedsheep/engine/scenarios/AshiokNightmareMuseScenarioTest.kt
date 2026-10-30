package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.thb.cards.AshiokNightmareMuse
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ashiok, Nightmare Muse (THB #208): the −3 bounces a nonland permanent and makes its *owner*
 * exile a card from their hand (never a bounced token), and the −7 casts up to three face-up
 * nonland cards opponents own in exile for free during resolution.
 */
class AshiokNightmareMuseScenarioTest : ScenarioTestBase() {

    private val ashiok = "Ashiok, Nightmare Muse"
    private val minusThree = AshiokNightmareMuse.activatedAbilities[1].id
    private val minusSeven = AshiokNightmareMuse.activatedAbilities[2].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun TestGame.names(playerId: EntityId, zone: Zone) =
        state.getZone(playerId, zone).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("−3: the permanent returns to hand and its owner chooses a card to exile from their hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, ashiok)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ashiokId = game.findPermanent(ashiok)!!
            game.setLoyalty(ashiokId, 5)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, ashiokId, minusThree, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the bounced permanent's owner makes the choice, and the returned card is eligible") {
                decision.playerId shouldBe game.player2Id
                decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldContainExactlyInAnyOrder
                    listOf("Grizzly Bears", "Hill Giant")
            }
            val giant = game.findCardsInHand(2, "Hill Giant").single()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.names(game.player2Id, Zone.EXILE) shouldBe listOf("Hill Giant")
            game.names(game.player2Id, Zone.HAND) shouldBe listOf("Grizzly Bears")
        }

        test("−7: up to three face-up nonland cards opponents own in exile are cast for free") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, ashiok)
                .withCardInExile(2, "Grizzly Bears")
                .withCardInExile(2, "Hill Giant")
                .withCardInExile(2, "Forest")
                .withCardInExile(1, "Glory Seeker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ashiokId = game.findPermanent(ashiok)!!
            game.setLoyalty(ashiokId, 7)

            game.execute(ActivateAbility(game.player1Id, ashiokId, minusSeven)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the opponent's nonland cards are offered — not their land, not your own exile") {
                decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldContainExactlyInAnyOrder
                    listOf("Grizzly Bears", "Hill Giant")
            }
            val bears = game.state.getExile(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            game.selectCards(listOf(bears)).error shouldBe null
            val giant = game.state.getExile(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
            }
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("both spells resolved under your control without any mana") {
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id
                game.state.projectedState.getController(game.findPermanent("Hill Giant")!!) shouldBe game.player1Id
            }
            game.names(game.player2Id, Zone.EXILE) shouldBe listOf("Forest")
            game.names(game.player1Id, Zone.EXILE) shouldBe listOf("Glory Seeker")
        }
    }
}
