package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.ChandraFireArtisan
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chandra, Fire Artisan (WAR #119).
 *
 *   Whenever one or more loyalty counters are removed from Chandra, she deals that much damage to
 *   target opponent or planeswalker.
 *   +1: Exile the top card of your library. You may play it this turn.
 *   −7: Exile the top seven cards of your library. You may play them this turn.
 *
 * The trigger sees damage dealt to her and her own −7 cost, and still fires once she has died to
 * the counters she lost (rulings).
 */
class ChandraFireArtisanScenarioTest : ScenarioTestBase() {

    init {
        test("damage dealt to Chandra comes back as that much damage to the opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Fire Artisan")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Fire Artisan")!!
            game.castSpell(1, "Lightning Bolt", chandra).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("three loyalty counters came off") { loyalty(game, chandra) shouldBe 1 }
            withClue("Chandra dealt 3 to the opponent") { game.getLifeTotal(2) shouldBe 17 }
        }

        test("+1 adds loyalty without triggering, exiles the top card and makes it playable") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Fire Artisan")
                .withCardInLibrary(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Fire Artisan")!!
            activate(game, chandra, index = 0)
            game.resolveStack()

            loyalty(game, chandra) shouldBe 5
            game.getLifeTotal(2) shouldBe 20
            castableFromExile(game, "Shock") shouldBe true
        }

        test("−7 from exactly seven kills her, but she still deals seven to the opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Fire Artisan")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Fire Artisan")!!
            setLoyalty(game, chandra, 7)
            activate(game, chandra, index = 1)
            if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("paying −7 left no loyalty, so she died") { game.isInGraveyard(1, "Chandra, Fire Artisan") shouldBe true }
            withClue("her trigger resolved for the seven counters removed") { game.getLifeTotal(2) shouldBe 13 }
            withClue("the exiled cards are playable this turn") { castableFromExile(game, "Shock") shouldBe true }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ChandraFireArtisan.activatedAbilities[index].id,
            )
        ).error shouldBe null
    }

    private fun castableFromExile(game: TestGame, cardName: String): Boolean =
        game.getLegalActions(1).any {
            it.actionType == "CastSpell" && it.description.contains(cardName) && it.sourceZone == "EXILE"
        }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
