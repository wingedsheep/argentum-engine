package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.cube.ResolvedCube
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Cube Pool Play: no draft, everybody deckbuilds from the whole cube at once, copies unlimited up to
 * the 4-of cap. The invariants that separate it from ordinary cube Sealed are that nothing is dealt
 * (so capacity can't fail and pools are identical), copy counts aren't bounded by the pool, and the
 * sideboard is *not* derived from the pool — deriving it would seed the whole cube into SIDEBOARD.
 */
class CubePoolPlayTest : FunSpec({
    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val generator = BoosterGenerator(
        mapOf(
            "TST" to BoosterGenerator.SetConfig(
                setCode = "TST",
                setName = "Test",
                cards = emptyList(),
                basicLands = listOf(forest),
            )
        )
    )
    val cards = (1..30).map { number ->
        CardDefinition.creature(
            name = "Cube Bear $number",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype.BEAR),
            power = 2,
            toughness = 2,
        )
    }

    fun lobby(
        format: TournamentFormat = TournamentFormat.SEALED,
        boosterCount: Int = 3,
        packSize: Int = 15,
        poolPlay: Boolean = true,
    ): TournamentLobby {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = generator,
            format = format,
            boosterCount = boosterCount,
        )
        lobby.addPlayer(PlayerIdentity(playerId = EntityId("host"), playerName = "Host"))
        lobby.addPlayer(PlayerIdentity(playerId = EntityId("guest"), playerName = "Guest"))
        lobby.configureCube(
            ResolvedCube(
                name = "Pool Cube",
                cards = cards,
                basicLandSetCode = "TST",
                packSize = packSize,
            )
        )
        lobby.cubePoolPlay = poolPlay
        return lobby
    }

    /** A legal 40-card Pool Play deck: 10 distinct cube cards at 4 copies each. */
    fun poolPlayDeck(): Map<String, Int> = (1..10).associate { "Cube Bear $it" to 4 }

    test("every player gets the entire cube as their pool") {
        val lobby = lobby()

        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        lobby.state shouldBe LobbyState.DECK_BUILDING
        lobby.players.values.forEach { it.cardPool.map { card -> card.name } shouldBe cards.map { c -> c.name } }
    }

    test("nothing is dealt, so a cube far smaller than players x packs x packSize still starts") {
        // 2 × 3 × 15 = 90 cards would be needed to deal; the cube only has 30.
        val lobby = lobby(boosterCount = 3, packSize = 15)

        lobby.cubeCapacityError() shouldBe null
        lobby.startDeckBuilding(EntityId("host")) shouldBe true
        lobby.cubeDealerRemainingCards() shouldBe emptyList()
    }

    test("banned cards are excluded from the Pool Play pool") {
        val lobby = lobby()
        lobby.bannedCardNames = setOf("Cube Bear 1")

        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        lobby.players.values.forEach { playerState ->
            playerState.cardPool.size shouldBe 29
            playerState.cardPool.none { it.name == "Cube Bear 1" } shouldBe true
        }
    }

    test("four copies of a singleton cube card is legal in Pool Play") {
        val lobby = lobby()
        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        val result = lobby.submitDeck(EntityId("host"), poolPlayDeck())

        result shouldBe TournamentLobby.DeckSubmissionResult.Success(allReady = false)
    }

    test("the 4-of cap still applies") {
        val lobby = lobby()
        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        val overCap = mapOf("Cube Bear 1" to 5) + (2..9).associate { "Cube Bear $it" to 4 } + mapOf("Forest" to 3)
        val result = lobby.submitDeck(EntityId("host"), overCap)

        (result as TournamentLobby.DeckSubmissionResult.Error).message shouldContain
            "more than 4 copies of Cube Bear 1"
    }

    test("a card outside the cube is still rejected") {
        val lobby = lobby()
        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        val result = lobby.submitDeck(EntityId("host"), poolPlayDeck() + mapOf("Lightning Bolt" to 1))

        (result as TournamentLobby.DeckSubmissionResult.Error).message shouldBe "Card not in cube: Lightning Bolt"
    }

    test("the sideboard is not derived from the pool") {
        val lobby = lobby()
        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        lobby.submitDeck(EntityId("host"), poolPlayDeck())

        // Ordinary Limited would derive pool − maindeck here, seeding the whole cube into SIDEBOARD.
        lobby.players[EntityId("host")]!!.submittedSideboard shouldBe emptyMap()
    }

    test("ordinary cube Sealed still derives the sideboard and enforces pool copy counts") {
        // 2 players × 1 pack × 15 exactly consumes the 30-card cube.
        val lobby = lobby(boosterCount = 1, packSize = 15, poolPlay = false)
        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        val pool = lobby.players[EntityId("host")]!!.cardPool
        val deck = pool.take(10).associate { it.name to 1 } + mapOf("Forest" to 30)
        lobby.submitDeck(EntityId("host"), deck) shouldBe
            TournamentLobby.DeckSubmissionResult.Success(allReady = false)

        lobby.players[EntityId("host")]!!.submittedSideboard.isNotEmpty() shouldBe true
    }

    test("Pool Play is inert outside Sealed") {
        val lobby = lobby(format = TournamentFormat.DRAFT, boosterCount = 1, packSize = 4)

        lobby.isCubePoolPlay shouldBe false
        lobby.startDraft(EntityId("host")) shouldBe true
        // The dealer ran, so packs were dealt rather than pools handed out whole.
        lobby.players.values.flatMap { it.currentPack.orEmpty() }.size shouldBe 8
    }

    test("clearing the cube clears Pool Play") {
        val lobby = lobby()

        lobby.configureCube(null)

        lobby.cubePoolPlay shouldBe false
        lobby.isCubePoolPlay shouldBe false
    }

    test("Pool Play survives a lobby restart") {
        val lobby = lobby()
        lobby.startDeckBuilding(EntityId("host")) shouldBe true
        val registry = CardRegistry().also { it.register(cards) }

        val (restored, _) = restoreTournamentLobby(lobby.toPersistent(), registry, generator)

        restored.cubePoolPlay shouldBe true
        restored.isCubePoolPlay shouldBe true
    }

    test("the broadcast lobby settings carry the Pool Play flag") {
        val lobby = lobby()

        lobby.buildLobbyUpdate(EntityId("host")).settings.cubePoolPlay shouldBe true
    }
})
