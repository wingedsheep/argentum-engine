package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Conch Horn (FEM #83) — "{1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from
 * your hand on top of your library."
 *
 * Covers the draw-two / put-one-back loop plus its *information* half: the card you put on top of
 * your own library stays known to you, and to nobody else.
 */
class ConchHornScenarioTest : ScenarioTestBase() {

    private val conchHornAbilityId =
        cardRegistry.getCard("Conch Horn")!!.activatedAbilities.first().id

    init {
        context("Conch Horn") {

            test("draws two and puts the chosen card back on top of the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Conch Horn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Plains")   // top — drawn
                    .withCardInLibrary(1, "Swamp")    // 2nd — drawn
                    .withCardInLibrary(1, "Mountain") // stays put
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun library(): List<EntityId> =
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))

                fun libraryNames(): List<String> =
                    library().mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

                val bolt = game.state.getZone(ZoneKey(game.player1Id, Zone.HAND))
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt" }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Conch Horn")!!,
                        abilityId = conchHornAbilityId,
                    )
                )
                withClue("Activating Conch Horn should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                // Both drawn cards are in hand; choose the pre-existing Lightning Bolt to put back.
                withClue("Drew two cards") {
                    game.isInHand(1, "Plains") shouldBe true
                    game.isInHand(1, "Swamp") shouldBe true
                }
                game.selectCards(listOf(bolt))
                game.resolveStack()

                withClue("The chosen card is on top of the library") {
                    libraryNames() shouldBe listOf("Lightning Bolt", "Mountain")
                }
            }

            /**
             * The information half of the put-back: a card you tuck on top of your own library is
             * knowledge you keep (CR 401.2 bars looking *through* a library; it does not erase what
             * you just placed), and it is yours alone — the opponent saw a card go back, not which.
             * The audience is decided once, in `LibraryRevealUtils.placementAudience`.
             */
            test("the card put back on top stays visible to the player who put it there") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Conch Horn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolt = game.state.getZone(ZoneKey(game.player1Id, Zone.HAND))
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt" }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Conch Horn")!!,
                        abilityId = conchHornAbilityId,
                    )
                )
                game.resolveStack()
                game.selectCards(listOf(bolt))
                game.resolveStack()

                // The put-back card is legitimately known to the player who put it there: the
                // client view carries its details, at index 0 of that library zone (which is what
                // the deck pile renders face up).
                val ownerView = stateTransformer.transform(game.state, viewingPlayerId = game.player1Id)
                withClue("The player who put it there still sees the card") {
                    ownerView.cards.keys shouldContain bolt
                    ownerView.cards[bolt]!!.name shouldBe "Lightning Bolt"
                }
                val ownerLibrary = ownerView.zones.first {
                    it.zoneId.ownerId == game.player1Id && it.zoneId.zoneType == Zone.LIBRARY
                }
                withClue("...at position 0, where the deck pile renders it") {
                    ownerLibrary.cardIds.first() shouldBe bolt
                }

                // ...and to nobody else. The opponent only knows a card was put back, not which.
                val opponentView = stateTransformer.transform(game.state, viewingPlayerId = game.player2Id)
                withClue("The opponent's view of that library stays opaque") {
                    opponentView.cards.keys shouldNotContain bolt
                }
            }
        }
    }
}
