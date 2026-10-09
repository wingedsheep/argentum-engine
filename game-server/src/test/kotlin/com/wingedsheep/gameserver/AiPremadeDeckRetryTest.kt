package com.wingedsheep.gameserver

import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.engine.deck.CommanderDeckGenerator
import com.wingedsheep.ai.engine.deck.ConstructedDeckGenerator
import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.gameserver.ai.RandomDeckResolver
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import kotlin.time.Duration.Companion.seconds

/**
 * An AI seat in a premade lobby is dealt a generated deck, and a sealed pool is free to hold more
 * copies of a common than the premade 4-of rule allows — about one Portal build in a hundred plays
 * five or six. The lobby used to log the rejection and leave the seat empty, which blocks the host's
 * start gate for good (and flaked `FreeForAllLobbyTest`). This pins the reroll: the first build the
 * resolver hands out is illegal, the second is not, and the seat ends up with a deck.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["game.ai.heuristic-deckbuilding=true"],
)
@Import(AiPremadeDeckRetryTest.IllegalFirstDeck::class)
class AiPremadeDeckRetryTest : GameServerTestBase() {

    @TestConfiguration
    class IllegalFirstDeck {
        @Bean
        @Primary
        fun illegalFirstResolver(
            sealed: SealedDeckGenerator,
            constructed: ConstructedDeckGenerator,
            commander: CommanderDeckGenerator,
        ): RandomDeckResolver = spyk(RandomDeckResolver(sealed, constructed, commander)).also { resolver ->
            every { resolver.resolve(any(), any(), any<List<String>>(), any()) } returnsMany listOf(
                GeneratedDeck(mapOf("Hulking Goblin" to 6, "Mountain" to 34)),
                GeneratedDeck(mapOf("Hulking Goblin" to 4, "Mountain" to 36)),
            )
        }
    }

    init {
        test("a generated deck the premade lobby rejects is rolled again, not left unsubmitted") {
            val host = createClient()
            host.connectAs("Retry Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }

            host.send(ClientMessage.AddAiToLobby)
            eventually(10.seconds) {
                val ai = host.messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull()
                    ?.players?.singleOrNull { it.isAi }
                ai?.deckSubmitted shouldBe true
            }
        }
    }
}
