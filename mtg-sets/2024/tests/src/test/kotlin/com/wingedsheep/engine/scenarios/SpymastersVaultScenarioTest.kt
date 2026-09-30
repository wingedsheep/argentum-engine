package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SpymastersVault
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spymaster's Vault (MH3) — "{B}, {T}: Target creature you control connives X, where X is the
 * number of creatures that died this turn."
 *
 * X counts every player's creatures, it is connive N (CR 701.50d), and with nothing dead it is a
 * connive 0 that draws and discards nothing (CR 701.50e).
 */
class SpymastersVaultScenarioTest : ScenarioTestBase() {

    private val conniveAbilityId = SpymastersVault.activatedAbilities.last().id

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Spymaster's Vault")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Giant Growth")
        .withCardInHand(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.addDeaths(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CreaturesDiedThisTurnComponent(count)) }
    }

    private fun TestGame.activateOn(creature: EntityId) = execute(
        ActivateAbility(
            player1Id, findPermanent("Spymaster's Vault")!!, conniveAbilityId,
            targets = listOf(ChosenTarget.Permanent(creature))
        )
    )

    private fun TestGame.plusOnes(creature: EntityId): Int =
        state.getEntity(creature)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.handCard(name: String): EntityId = state.getHand(player1Id).first {
        state.getEntity(it)?.get<CardComponent>()?.name == name
    }

    init {
        context("Spymaster's Vault — connive X, X = creatures that died this turn") {

            test("enters tapped without a Swamp, untapped with one") {
                val noSwamp = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spymaster's Vault")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                noSwamp.execute(PlayLand(noSwamp.player1Id, noSwamp.handCard("Spymaster's Vault"))).error shouldBe null
                noSwamp.state.getEntity(noSwamp.findPermanent("Spymaster's Vault")!!)!!.has<TappedComponent>() shouldBe true

                val withSwamp = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spymaster's Vault")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                withSwamp.execute(PlayLand(withSwamp.player1Id, withSwamp.handCard("Spymaster's Vault"))).error shouldBe null
                withSwamp.state.getEntity(withSwamp.findPermanent("Spymaster's Vault")!!)!!.has<TappedComponent>() shouldBe false
            }

            test("a real death counts: Bolt the opponent's Hill Giant, then connive 1") {
                val game = board()
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()
                // Hill Giant is 3/3; Bolt kills it.
                game.isOnBattlefield("Hill Giant") shouldBe false

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)
                game.activateOn(bears).error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.minSelections shouldBe 1
                game.selectCards(listOf(game.handCard("Giant Growth")))
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                withClue("Giant Growth is nonland -> one counter") { game.plusOnes(bears) shouldBe 1 }
            }

            test("deaths on both sides sum: connive 2, one counter per nonland discard") {
                val game = board()
                game.addDeaths(game.player1Id, 1)
                game.addDeaths(game.player2Id, 1)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.activateOn(bears).error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.minSelections shouldBe 2
                decision.maxSelections shouldBe 2
                game.selectCards(listOf(game.handCard("Giant Growth"), game.handCard("Forest")))
                game.resolveStack()

                game.isInGraveyard(1, "Giant Growth") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                withClue("one nonland, one land -> one counter") { game.plusOnes(bears) shouldBe 1 }
            }

            test("no creature died: connive 0 draws and discards nothing (CR 701.50e)") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.activateOn(bears).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe handBefore
                game.plusOnes(bears) shouldBe 0
            }

            test("can't target a creature an opponent controls") {
                val game = board()
                game.addDeaths(game.player2Id, 1)
                val giant = game.findPermanent("Hill Giant")!!
                (game.activateOn(giant).error != null) shouldBe true
            }
        }
    }
}
