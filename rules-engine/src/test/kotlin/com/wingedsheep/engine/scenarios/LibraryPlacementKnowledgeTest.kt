package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Who is allowed to keep seeing a card once it has been put into a library.
 *
 * CR 401.2 stops a player looking *through* a library; it does not erase what they just watched go
 * in. Every library entry in the engine funnels through `ZoneTransitionService`, which asks
 * [com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils.placementAudience] the single
 * question "who knows this slot?" and **replaces** the card's audience with the answer:
 *
 *  - a random position (shuffle) — nobody;
 *  - out of a public zone, or revealed on the way in — the whole table;
 *  - otherwise (out of a hand) — only the player who chose the card and saw where it landed.
 *
 * Replacing rather than merging is what stops a leak: a card the whole table knew while it sat in
 * a hand goes dark again the moment it is tucked away. That is why
 * [com.wingedsheep.engine.mechanics.RevealedInHandTracker] no longer clears reveals for cards
 * leaving a hand *for a library* — this seam has already given the authoritative answer, and the
 * tracker's old blanket clear wiped it, blinding the player to their own put-back.
 */
class LibraryPlacementKnowledgeTest : ScenarioTestBase() {

    /** A sorcery that puts a card of the caster's choice from their hand on top of their library. */
    private val tuckFromHand = card("Tuck From Hand Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "toTop",
                    selectedLabel = "Put on top of your library"
                ),
                MoveCollectionEffect(
                    from = "toTop",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Top)
                )
            )
        }
    }

    /** A sorcery that shuffles the caster's library. */
    private val shuffleUp = card("Shuffle Up Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = ShuffleLibraryEffect() }
    }

    /** Players whom [cardId] is currently revealed to. */
    private fun GameState.revealedTo(cardId: EntityId): Set<EntityId> =
        getEntity(cardId)?.get<RevealedToComponent>()?.playerIds ?: emptySet()

    private fun GameState.handCard(playerId: EntityId, name: String): EntityId =
        getHand(playerId).first { getEntity(it)?.get<CardComponent>()?.name == name }

    private fun GameState.libraryTop(playerId: EntityId): EntityId =
        getZone(ZoneKey(playerId, Zone.LIBRARY)).first()

    init {
        cardRegistry.register(tuckFromHand)
        cardRegistry.register(shuffleUp)

        context("library placement knowledge") {

            test("a card put back from hand is known to the player who put it there, and nobody else") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tuck From Hand Test")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Plains") // a second candidate, so the choice is a real one
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.state.handCard(game.player1Id, "Grizzly Bears")

                game.castSpell(1, "Tuck From Hand Test")
                game.resolveStack()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("the chosen card is on top of the library") {
                    game.state.libraryTop(game.player1Id) shouldBe bears
                }
                withClue("the player who put it there still knows it") {
                    game.state.revealedTo(bears) shouldBe setOf(game.player1Id)
                }

                // End to end through the masking seam the client renders from.
                val ownerView = game.getClientState(1)
                withClue("the owner's view carries the card's details") {
                    ownerView.cards.containsKey(bears) shouldBe true
                    ownerView.cards[bears]!!.name shouldBe "Grizzly Bears"
                }
                val opponentView = game.getClientState(2)
                withClue("the opponent's view of that library stays opaque") {
                    opponentView.cards.containsKey(bears).shouldBeFalse()
                }
            }

            test("a card the whole table knew in hand goes dark again once it is tucked away") {
                // Regression: the reveal audience is *replaced* at placement, not merged. Without
                // that, a card made public while in hand would keep leaking from inside a library.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Unsummon")
                    .withCardInHand(1, "Tuck From Hand Test")
                    .withCardInHand(1, "Plains") // a second candidate, so the choice is a real one
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val unsummon = game.state.handCard(game.player1Id, "Unsummon")

                // Bounce it: returning to hand from the battlefield makes it public knowledge.
                game.execute(CastSpell(game.player1Id, unsummon, listOf(ChosenTarget.Permanent(bears))))
                game.resolveStack()
                withClue("the bounced creature is known to the opponent while it sits in hand") {
                    game.state.revealedTo(bears) shouldContain game.player2Id
                }

                // Now tuck that same card on top of the library.
                game.castSpell(1, "Tuck From Hand Test")
                game.resolveStack()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("the opponent loses it — a hidden zone hides it again") {
                    (game.player2Id in game.state.revealedTo(bears)).shouldBeFalse()
                }
                withClue("the player who tucked it still knows it") {
                    game.state.revealedTo(bears) shouldBe setOf(game.player1Id)
                }
                withClue("and the opponent's view cannot see it either") {
                    game.getClientState(2).cards.containsKey(bears).shouldBeFalse()
                }
            }

            test("a creature put on top of its owner's library from the battlefield is known to both players") {
                // Time Ebb (POR) — the single-card move path. The card was public on the
                // battlefield and every player watched where it went, so nobody is guessing.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Time Ebb")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val timeEbb = game.state.handCard(game.player1Id, "Time Ebb")

                game.execute(CastSpell(game.player1Id, timeEbb, listOf(ChosenTarget.Permanent(bears))))
                game.resolveStack()

                withClue("the creature is on top of its owner's library") {
                    game.state.libraryTop(game.player2Id) shouldBe bears
                }
                withClue("both players know what is on top") {
                    game.state.revealedTo(bears) shouldBe setOf(game.player1Id, game.player2Id)
                }
            }

            test("shuffling the library forgets the card that was put on top") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tuck From Hand Test")
                    .withCardInHand(1, "Shuffle Up Test")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.state.handCard(game.player1Id, "Grizzly Bears")

                game.castSpell(1, "Tuck From Hand Test")
                game.resolveStack()
                game.selectCards(listOf(bears))
                game.resolveStack()
                game.state.revealedTo(bears) shouldBe setOf(game.player1Id)

                game.castSpell(1, "Shuffle Up Test")
                game.resolveStack()

                withClue("a shuffle wipes what anyone knew about this library's contents") {
                    game.state.revealedTo(bears) shouldBe emptySet()
                }
            }
        }
    }
}
