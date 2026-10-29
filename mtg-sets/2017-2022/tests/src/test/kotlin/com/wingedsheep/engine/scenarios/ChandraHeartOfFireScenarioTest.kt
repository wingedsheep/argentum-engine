package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.ChandraHeartOfFire
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chandra, Heart of Fire (M21 #135).
 *
 *   +1: Discard your hand, then exile the top three cards of your library. Until end of turn, you
 *       may play cards exiled this way.
 *   −9: Search your graveyard and library for any number of red instant and/or sorcery cards,
 *       exile them, then shuffle. You may cast them this turn. Add six {R}.
 *
 * The −9 is the composition worth proving: one search over two zones, only red instants/sorceries
 * offered, exactly the chosen cards exiled and made castable, and the six red mana.
 */
class ChandraHeartOfFireScenarioTest : ScenarioTestBase() {

    init {
        context("the impulse +1") {

            test("discards the hand, exiles the top three, and makes them playable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Heart of Fire")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Heart of Fire")!!
                activate(game, chandra, index = 0)
                game.resolveStack()

                withClue("the whole hand was discarded") {
                    game.handSize(1) shouldBe 0
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("three cards were exiled, one left in the library") {
                    game.librarySize(1) shouldBe 1
                }
                withClue("Shock, exiled whichever end of the library is the top, is castable from exile this turn") {
                    castableFromExile(game, "Shock") shouldBe true
                }
                withClue("+1 took Chandra from 5 to 6") { loyalty(game, chandra) shouldBe 6 }
            }
        }

        context("the −9") {

            test("exiles the chosen red instants from graveyard and library, makes them castable, adds six {R}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Heart of Fire")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Counterspell")
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Heart of Fire")!!
                setLoyalty(game, chandra, 9)
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val shock = game.findCardsInLibrary(1, "Shock").single()

                activate(game, chandra, index = 2)
                game.resolveStack()
                withClue("the search asks which red instants/sorceries to exile") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bolt, shock)).error shouldBe null
                game.resolveStack()

                withClue("both chosen cards were exiled from their zones") {
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.isInExile(1, "Shock") shouldBe true
                }
                withClue("non-red and non-instant/sorcery cards stay where they were") {
                    game.isInGraveyard(1, "Counterspell") shouldBe true
                    game.librarySize(1) shouldBe 1
                }
                withClue("six red mana landed in the pool") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 6
                }
                withClue("the exiled spells may be cast this turn") {
                    castableFromExile(game, "Lightning Bolt") shouldBe true
                    castableFromExile(game, "Shock") shouldBe true
                }
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ChandraHeartOfFire.activatedAbilities[index].id,
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
