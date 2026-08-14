package com.wingedsheep.gameserver.ranking

import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.GameRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class EloTest : FunSpec({

    test("expected score is 0.5 for equal ratings and symmetric") {
        Elo.expectedScore(1500.0, 1500.0) shouldBe (0.5 plusOrMinus 1e-9)
        val a = Elo.expectedScore(1700.0, 1500.0)
        val b = Elo.expectedScore(1500.0, 1700.0)
        (a + b) shouldBe (1.0 plusOrMinus 1e-9)
        (a > 0.5) shouldBe true
    }

    test("a 400-point gap is roughly a 10:1 expectation") {
        Elo.expectedScore(1900.0, 1500.0) shouldBe (0.9090909 plusOrMinus 1e-5)
    }

    test("K-factor is higher during the placement window then settles") {
        Elo.kFactor(0) shouldBe Elo.K_PROVISIONAL
        Elo.kFactor(Elo.PROVISIONAL_GAMES - 1) shouldBe Elo.K_PROVISIONAL
        Elo.kFactor(Elo.PROVISIONAL_GAMES) shouldBe Elo.K_ESTABLISHED
        Elo.kFactor(100) shouldBe Elo.K_ESTABLISHED
    }

    test("equal-rated win/loss moves by half the K-factor in opposite directions") {
        val expected = Elo.expectedScore(1500.0, 1500.0)
        val win = Elo.newRating(1500.0, expected, score = 1.0, kFactor = Elo.K_ESTABLISHED)
        val loss = Elo.newRating(1500.0, expected, score = 0.0, kFactor = Elo.K_ESTABLISHED)
        // Established K = 20 → an even game shifts about ±10, chess.com-style.
        win shouldBe (1510.0 plusOrMinus 1e-9)
        loss shouldBe (1490.0 plusOrMinus 1e-9)
    }

    test("beating a much stronger opponent gains more than beating a peer") {
        val vsPeer = Elo.newRating(1500.0, Elo.expectedScore(1500.0, 1500.0), 1.0, 24.0) - 1500.0
        val vsStrong = Elo.newRating(1500.0, Elo.expectedScore(1500.0, 1900.0), 1.0, 24.0) - 1500.0
        (vsStrong > vsPeer) shouldBe true
    }

    test("tier is Provisional until placement completes, regardless of rating") {
        Elo.tier(2500.0, gamesPlayed = 0) shouldBe RatingTier.PROVISIONAL
        Elo.tier(800.0, gamesPlayed = Elo.PROVISIONAL_GAMES - 1) shouldBe RatingTier.PROVISIONAL
    }

    test("tier bands are a pure function of rating once established") {
        val g = Elo.PROVISIONAL_GAMES
        Elo.tier(999.0, g) shouldBe RatingTier.BRONZE
        Elo.tier(1000.0, g) shouldBe RatingTier.SILVER
        Elo.tier(1199.0, g) shouldBe RatingTier.SILVER
        Elo.tier(1200.0, g) shouldBe RatingTier.GOLD
        Elo.tier(1399.0, g) shouldBe RatingTier.GOLD
        Elo.tier(1400.0, g) shouldBe RatingTier.PLATINUM
        Elo.tier(1600.0, g) shouldBe RatingTier.DIAMOND
        Elo.tier(1999.0, g) shouldBe RatingTier.DIAMOND
        Elo.tier(2000.0, g) shouldBe RatingTier.MYTHIC
        Elo.tier(3000.0, g) shouldBe RatingTier.MYTHIC
    }

    test("quick-game mode derivation maps the rules axis and format to the right queue") {
        val standard = GameRules.STANDARD
        Ranked.modeForQuickGame(standard, format = null, momirBasic = false) shouldBe RankedMode.LIMITED
        Ranked.modeForQuickGame(standard, format = null, momirBasic = true) shouldBe RankedMode.CONSTRUCTED
        Ranked.modeForQuickGame(standard, format = DeckFormat.STANDARD, momirBasic = false) shouldBe RankedMode.CONSTRUCTED
        Ranked.modeForQuickGame(standard, format = DeckFormat.MODERN, momirBasic = false) shouldBe RankedMode.CONSTRUCTED
        // Commander-shaped deck legality is what *derives* the rules on this lobby kind, but the queue
        // follows the rules — so a Commander-legal deck list under Standard rules is CONSTRUCTED.
        Ranked.modeForQuickGame(standard, format = DeckFormat.COMMANDER, momirBasic = false) shouldBe RankedMode.CONSTRUCTED
        Ranked.modeForQuickGame(GameRules.COMMANDER, format = DeckFormat.COMMANDER, momirBasic = false) shouldBe RankedMode.COMMANDER
        Ranked.modeForQuickGame(GameRules.COMMANDER, format = DeckFormat.BRAWL, momirBasic = false) shouldBe RankedMode.COMMANDER
    }

    test("tournament mode derivation asks the rules axis first, then where the cards came from") {
        val standard = GameRules.STANDARD
        Ranked.modeForTournament(standard, TournamentFormat.SEALED) shouldBe RankedMode.LIMITED
        Ranked.modeForTournament(standard, TournamentFormat.DRAFT) shouldBe RankedMode.LIMITED
        Ranked.modeForTournament(standard, TournamentFormat.WINSTON_DRAFT) shouldBe RankedMode.LIMITED
        Ranked.modeForTournament(standard, TournamentFormat.GRID_DRAFT) shouldBe RankedMode.LIMITED
        Ranked.modeForTournament(standard, TournamentFormat.PREMADE_DECKS) shouldBe RankedMode.CONSTRUCTED
        // Commander is one queue however the pool was built: drafted, sealed, or brought.
        Ranked.modeForTournament(GameRules.COMMANDER, TournamentFormat.COMMANDER_DRAFT) shouldBe RankedMode.COMMANDER
        Ranked.modeForTournament(GameRules.COMMANDER, TournamentFormat.COMMANDER_SEALED) shouldBe RankedMode.COMMANDER
        Ranked.modeForTournament(GameRules.COMMANDER, TournamentFormat.PREMADE_DECKS) shouldBe RankedMode.COMMANDER
        Ranked.modeForTournament(GameRules.COMMANDER, TournamentFormat.DRAFT) shouldBe RankedMode.COMMANDER
    }
})
