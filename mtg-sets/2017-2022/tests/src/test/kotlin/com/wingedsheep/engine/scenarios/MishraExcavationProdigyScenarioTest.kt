package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Mishra, Excavation Prodigy — the first card to filter a batch discard trigger by card type
 * ("Whenever you discard one or more artifact cards"), capped at one fire per turn.
 *
 * Covers: discarding an artifact card adds {R}{R}; discarding a non-artifact card adds nothing; and
 * a second artifact discard in the same turn adds nothing more.
 */
class MishraExcavationProdigyScenarioTest : ScenarioTestBase() {

    private val looterAbilityId by lazy {
        cardRegistry.requireCard("Mishra, Excavation Prodigy").activatedAbilities[0].id
    }

    private fun TestGame.redInPool(): Int =
        state.getEntity(player1Id)?.get<ManaPoolComponent>()?.red ?: 0

    private fun TestGame.untappedMountains(): Int =
        state.getBattlefield().count { id ->
            state.getEntity(id)?.let {
                it.get<CardComponent>()?.name == "Mountain" &&
                    !it.has<TappedComponent>()
            } ?: false
        }

    private fun TestGame.loot(mishra: EntityId, discard: EntityId) {
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = mishra,
                abilityId = looterAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(discard))
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun board() = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Mishra, Excavation Prodigy")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Ornithopter")
        .withCardInHand(1, "Bottle Gnomes")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Shock")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("discarding an artifact card adds {R}{R}") {
            val game = board()
            val mishra = game.findPermanent("Mishra, Excavation Prodigy")!!
            game.loot(mishra, game.findCardsInHand(1, "Ornithopter").single())

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.findCardsInHand(1, "Shock").size shouldBe 1
            game.redInPool() shouldBe 2
        }

        test("discarding a non-artifact card adds no mana") {
            val game = board()
            val mishra = game.findPermanent("Mishra, Excavation Prodigy")!!
            game.loot(mishra, game.findCardsInHand(1, "Lightning Bolt").single())

            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.findCardsInHand(1, "Shock").size shouldBe 1
            game.redInPool() shouldBe 0
        }

        test("the mana trigger fires only once each turn") {
            val game = board()
            val mishra = game.findPermanent("Mishra, Excavation Prodigy")!!
            game.loot(mishra, game.findCardsInHand(1, "Ornithopter").single())
            game.redInPool() shouldBe 2

            game.state = game.state.updateEntity(mishra) { it.without<TappedComponent>() }
            game.loot(mishra, game.findCardsInHand(1, "Bottle Gnomes").single())

            game.isInGraveyard(1, "Bottle Gnomes") shouldBe true
            // Two Mountains + one {R}{R} = four red sources, minus {1} per activation = 2 left.
            // A second trigger would have left 4. Either payment source (pool or Mountain) counts.
            (game.redInPool() + game.untappedMountains()) shouldBe 2
        }
    }
}
