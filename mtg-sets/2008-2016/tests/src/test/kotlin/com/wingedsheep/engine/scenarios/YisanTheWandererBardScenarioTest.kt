package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.YisanTheWandererBard
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yisan, the Wanderer Bard (M15 #209) — {2}{G} Legendary Creature — Human Rogue Bard 2/3.
 *
 *   {2}{G}, {T}, Put a verse counter on Yisan: Search your library for a creature card with mana
 *   value equal to the number of verse counters on Yisan, put it onto the battlefield, then shuffle.
 *
 * The verse counter is part of the cost, so the count read on resolution already includes it; if
 * Yisan is gone by then, its last-known verse counters decide (2014-07-18 ruling).
 */
class YisanTheWandererBardScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.yisan() = findPermanent("Yisan, the Wanderer Bard")!!

    private fun ScenarioTestBase.TestGame.verseCounters(): Int =
        state.getEntity(yisan())?.get<CountersComponent>()?.getCount(CounterType.VERSE) ?: 0

    private fun ScenarioTestBase.TestGame.activateYisan() {
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = yisan(),
                abilityId = YisanTheWandererBard.activatedAbilities.single().id
            )
        ).error shouldBe null
    }

    private fun ScenarioTestBase.TestGame.offeredCards() =
        (getPendingDecision() as SelectCardsDecision).options

    private fun builder() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Yisan, the Wanderer Bard")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Llanowar Elves")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("the verse counter is a cost, so the first activation finds a mana value 1 creature, the second a mana value 2 one") {
            val game = builder().build()

            game.activateYisan()
            withClue("the cost puts the verse counter on before the ability resolves") {
                game.verseCounters() shouldBe 1
            }
            game.resolveStack()

            withClue("only the mana value 1 creature is offered") {
                game.offeredCards() shouldBe game.findCardsInLibrary(1, "Llanowar Elves")
            }
            game.selectCards(game.findCardsInLibrary(1, "Llanowar Elves"))
            game.resolveStack()
            game.isOnBattlefield("Llanowar Elves") shouldBe true

            game.state = game.state.updateEntity(game.yisan()) { it.without<TappedComponent>() }
            game.activateYisan()
            game.verseCounters() shouldBe 2
            game.resolveStack()

            withClue("with two verse counters only the mana value 2 creature is offered") {
                game.offeredCards() shouldBe game.findCardsInLibrary(1, "Grizzly Bears")
            }
            game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears"))
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
        }

        test("if Yisan leaves the battlefield in response, its last-known verse counters decide") {
            val game = builder()
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInHand(2, "Lightning Bolt")
                .build()

            game.activateYisan()
            game.passPriority()
            game.castSpell(2, "Lightning Bolt", game.yisan()).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Yisan, the Wanderer Bard") shouldBe true
            withClue("the counter paid as the cost still counts: only the mana value 1 creature is offered") {
                game.offeredCards() shouldBe game.findCardsInLibrary(1, "Llanowar Elves")
            }
            game.selectCards(game.findCardsInLibrary(1, "Llanowar Elves"))
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
        }
    }
}
