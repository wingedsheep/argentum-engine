package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.persistence.MatchResultRow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

class RecordedDeckTest : FunSpec({

    test("pins are stripped from the counts and kept as each card's printing") {
        val deck = RecordedDeck.of(
            listOf("Shock#M19-156", "Shock#M19-156", "Forest#BLB-276", "Grizzly Bears"),
        )
        deck.cards shouldBe mapOf("Shock" to 2, "Forest" to 1, "Grizzly Bears" to 1)
        deck.printings shouldBe mapOf("Shock" to "M19-156", "Forest" to "BLB-276")
    }

    test("mixed printings of one card record the one most copies use") {
        val deck = RecordedDeck.of(listOf("Forest#BLB-276", "Forest#BLB-277", "Forest#BLB-277", "Forest"))
        deck.cards shouldBe mapOf("Forest" to 4)
        deck.printings shouldBe mapOf("Forest" to "BLB-277")
    }

    test("a pin without a set code addresses no printing") {
        RecordedDeck.of(listOf("Shock#156")).printings shouldBe emptyMap()
    }

    test("the sink stores each card's printing on its row") {
        val repo = mockk<MatchResultRepository>()
        val saved = slot<MatchResultRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }
        val deck = RecordedDeck.of(listOf("Shock#M19-156", "Grizzly Bears"))
        JdbcMatchResultSink(repo).record(
            RecordedMatch(
                gameId = "g", format = "Standard", tournamentName = null, gameMode = "CASUAL",
                frameCount = 20, turnCount = 6, startedAt = Instant.now(), endedAt = Instant.now(),
                participants = listOf(
                    RecordedParticipant(
                        userId = null, playerName = "Guest", won = true,
                        deckCards = deck.cards, deckPrintings = deck.printings,
                    ),
                ),
            ),
        )
        saved.captured.participants.single().cards.associate { it.cardName to it.printing } shouldBe
            mapOf("Shock" to "M19-156", "Grizzly Bears" to null)
    }
})
