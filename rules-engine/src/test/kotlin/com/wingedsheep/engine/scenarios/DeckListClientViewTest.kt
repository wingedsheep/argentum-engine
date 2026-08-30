package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientDeckCard
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.StateDiffCalculator
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The in-game deck tracker: `ClientGameState.deck`, one row per distinct card the viewing player
 * *owns*, with a `remaining` count of copies whose location they can't identify.
 *
 * The list is derived from live ownership rather than a stored decklist, which is what makes the
 * awkward cases fall out for free — ownership never changes (CR 108.3), so a stolen permanent is
 * still in its owner's deck; tokens were never cards in anyone's deck; and the sideboard is outside
 * the game (CR 400.11a) until something wishes a card in.
 *
 * The privacy contract is the other half of the feature: the list only ever describes the viewer,
 * it is empty for spectators, and it reports "copies you haven't seen" rather than "copies in your
 * library" precisely so it can't be read backwards to learn which card an opponent exiled face
 * down. Library *order* is never exposed at all — these are aggregate counts.
 */
class DeckListClientViewTest : ScenarioTestBase() {

    private val transformer = ClientStateTransformer(cardRegistry)
    private val p1 = EntityId.of("player-1")
    private val p2 = EntityId.of("player-2")

    private fun deckOf(state: GameState, viewer: EntityId = p1): List<ClientDeckCard> =
        transformer.transform(state, viewer).deck

    private fun row(state: GameState, name: String, viewer: EntityId = p1): ClientDeckCard? =
        deckOf(state, viewer).firstOrNull { it.cardName == name }

    init {
        test("the deck lists every card you own, counting only unseen copies as remaining") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()

            val bolt = row(game.state, "Lightning Bolt")
            bolt.shouldNotBeNull()
            bolt.copies shouldBe 4
            bolt.remaining shouldBe 2 // only the two still in the library are unaccounted for

            val bears = row(game.state, "Grizzly Bears")
            bears.shouldNotBeNull()
            bears.copies shouldBe 1
            bears.remaining shouldBe 0 // it's on the battlefield; you can see it
        }

        test("rows carry the display metadata the tracker renders — curve, colours, type buckets") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .build()

            val bolt = row(game.state, "Lightning Bolt")
            bolt.shouldNotBeNull()
            bolt.cmc shouldBe 1
            bolt.cardTypes shouldContainExactly listOf("INSTANT")
            bolt.colors shouldContainExactly listOf("RED")
        }

        test("the deck is ordered by mana value then name, so the panel needn't re-sort") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Divination")      // {2}{U}
                .withCardInLibrary(1, "Lightning Bolt")  // {R}
                .withCardInLibrary(1, "Pacifism")        // {1}{W}
                .build()

            deckOf(game.state).map { it.cardName } shouldContainExactly
                listOf("Lightning Bolt", "Pacifism", "Divination")
        }

        test("a card an opponent owns is never in your deck, whoever controls it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            row(game.state, "Grizzly Bears", viewer = p1).shouldBeNull()
            row(game.state, "Grizzly Bears", viewer = p2).shouldNotBeNull()
        }

        test("a permanent an opponent stole is still in its owner's deck (CR 108.3)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()

            val bears = game.state.getBattlefield().single()
            val stolen = game.state.updateEntity(bears) { it.with(ControllerComponent(p2)) }

            // Owned by p1, controlled by p2: p1's deck still has it, p2's still doesn't.
            val p1Row = row(stolen, "Grizzly Bears", viewer = p1)
            p1Row.shouldNotBeNull()
            p1Row.copies shouldBe 1
            p1Row.remaining shouldBe 0 // face up on the battlefield — its owner can see it
            row(stolen, "Grizzly Bears", viewer = p2).shouldBeNull()
        }

        test("tokens are not cards and never enter the decklist") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .build()

            deckOf(game.state).shouldContainExactly(emptyList())
        }

        test("the sideboard is outside the game and isn't counted (CR 400.11a)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInSideboard(1, "Lightning Bolt")
                .build()

            val bolt = row(game.state, "Lightning Bolt")
            bolt.shouldNotBeNull()
            bolt.copies shouldBe 1
            bolt.remaining shouldBe 1
        }

        test("the command zone counts, so a commander stays one stable row as it's cast and returns") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInCommandZone(1, "Grizzly Bears")
                .build()

            val commander = row(game.state, "Grizzly Bears")
            commander.shouldNotBeNull()
            commander.copies shouldBe 1
            commander.remaining shouldBe 0 // the command zone is public
        }

        test("your own card exiled face down stays 'unseen' rather than being revealed as exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInExile(1, "Lightning Bolt")
                .build()

            val exiled = game.state.getZone(p1, Zone.EXILE).single()
            // An opponent's effect exiled it face down: they control the exile, you can't look.
            val hidden = game.state.updateEntity(exiled) {
                it.with(FaceDownComponent).with(ControllerComponent(p2))
            }

            val bolt = row(hidden, "Lightning Bolt")
            bolt.shouldNotBeNull()
            bolt.copies shouldBe 2
            // Both are "unseen" — reporting 1 would tell you a Bolt is the card that got exiled.
            bolt.remaining shouldBe 2
        }

        test("a face-down card you control yourself is one you can identify") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()

            val bears = game.state.getBattlefield().single()
            val faceDown = game.state.updateEntity(bears) { it.with(FaceDownComponent) }

            val row = row(faceDown, "Grizzly Bears")
            row.shouldNotBeNull()
            row.remaining shouldBe 0
        }

        test("an individually known library card is accounted for without treating the zone as public") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Lightning Bolt")
                .build()

            val known = game.state.getLibrary(p1).first()
            val withKnownTop = game.state.updateEntity(known) {
                it.with(RevealedToComponent.to(p1))
            }

            val bolt = row(withKnownTop, "Lightning Bolt")
            bolt.shouldNotBeNull()
            bolt.copies shouldBe 2
            bolt.remaining shouldBe 1
        }

        test("spectators are told nothing about anyone's deck") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .build()

            transformer.transform(game.state, p1, isSpectator = true).deck
                .shouldContainExactly(emptyList())
        }

        test("each player's view describes only their own deck") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(2, "Divination")
                .build()

            deckOf(game.state, viewer = p1).map { it.cardName } shouldContainExactly listOf("Lightning Bolt")
            deckOf(game.state, viewer = p2).map { it.cardName } shouldContainExactly listOf("Divination")
        }

        test("the delta only carries the deck when a count actually moved") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Lightning Bolt")
                .build()

            val before = transformer.transform(game.state, p1)

            // An unrelated change must not drag the whole decklist along with it.
            val laterTurn = game.state.copy(turnNumber = game.state.turnNumber + 1)
            StateDiffCalculator.computeDelta(before, transformer.transform(laterTurn, p1))
                .deck.shouldBeNull()

            // Drawing one moves a copy from unseen to seen, so the tracker has to be resent.
            val boltId = game.state.getLibrary(p1).first()
            val drawn = game.state.moveToZone(
                boltId,
                com.wingedsheep.engine.state.ZoneKey(p1, Zone.LIBRARY),
                com.wingedsheep.engine.state.ZoneKey(p1, Zone.HAND),
            )
            val delta = StateDiffCalculator.computeDelta(before, transformer.transform(drawn, p1))
            delta.deck.shouldNotBeNull()
            delta.deck.single().remaining shouldBe 1
        }
    }
}
