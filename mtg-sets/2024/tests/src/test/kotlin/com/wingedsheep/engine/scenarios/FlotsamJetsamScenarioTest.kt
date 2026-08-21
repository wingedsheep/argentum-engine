package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.FlotsamJetsam
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Flotsam // Jetsam (MKM #247) — a split card (CR 709).
 *
 * "Flotsam {1}{G/U} — Instant. Mill three cards. Investigate.
 *  Jetsam {4}{U/B}{U/B} — Sorcery. Each opponent mills three cards, then you may cast a spell from
 *  each opponent's graveyard without paying its mana cost. If a spell cast this way would be put
 *  into a graveyard, exile it instead."
 *
 * Flotsam is existing vocabulary and is tested here only to prove the split face resolves at all.
 * Jetsam is the half that needed engine work, and the tests pin the two properties that would
 * silently invert if it were wired naively:
 *
 * - **Who chooses and who casts.** The per-opponent iteration rebinds the context's controller to
 *   the iterated opponent so `Player.You` names *their* graveyard. Both the picker and the cast
 *   therefore take `Chooser.SourceController`. If either used the plain controller, the opponent
 *   would be handed the decision and the spell would resolve under *their* control — visible here
 *   as the life gain landing on the wrong player.
 * - **Where the spell goes.** The rider must exile the cast card rather than let it fall straight
 *   back into the graveyard it came from.
 */
class FlotsamJetsamScenarioTest : FunSpec({

    // A targetless instant, so the assertion is about who gained the life rather than about a
    // target prompt. Owned by the opponent whose graveyard it sits in.
    val salvage = card("Test Jetsam Salvage") {
        manaCost = "{2}{U}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(5) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        // The Clue token is a real CardDefinition the investigate executor looks up by name;
        // GameTestDriver starts with only TestCards, so it has to be registered explicitly.
        driver.registerCards(listOf(FlotsamJetsam, salvage, PredefinedTokens.Clue))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle(you: EntityId) {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun optionNames(driver: GameTestDriver, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    test("Flotsam mills three of your own cards and makes a Clue") {
        val driver = newDriver()
        val you = driver.player1
        val card = driver.putCardInHand(you, "Flotsam // Jetsam")
        driver.giveMana(you, Color.GREEN, 2)

        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        driver.submit(
            CastSpell(you, card, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.settle(you)

        withClue("three cards left your library for your graveyard") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore - 3
            driver.getGraveyardCardNames(you).count { it == "Island" } shouldBe 3
        }
        withClue("investigate created a Clue") {
            driver.getPermanents(you)
                .mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }
                .contains("Clue") shouldBe true
        }
    }

    test("Jetsam mills each opponent, and you pick and cast the spell from their graveyard") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val card = driver.putCardInHand(you, "Flotsam // Jetsam")
        val salvageId = driver.putCardInGraveyard(opponent, "Test Jetsam Salvage")
        driver.giveMana(you, Color.BLUE, 6)

        val opponentLibraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        val yourLifeBefore = driver.getLifeTotal(you)
        val opponentLifeBefore = driver.getLifeTotal(opponent)

        driver.submit(
            CastSpell(you, card, faceIndex = 1, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("the mill happened before the casting step") {
            driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size shouldBe opponentLibraryBefore - 3
        }

        val decision = driver.pendingDecision as SelectCardsDecision
        withClue("Chooser.SourceController — *you* pick out of the opponent's graveyard, not them") {
            decision.playerId shouldBe you
        }
        withClue("lands milled into the graveyard aren't spells") {
            optionNames(driver, decision) shouldBe setOf("Test Jetsam Salvage")
            decision.minSelections shouldBe 0
            decision.maxSelections shouldBe 1
        }

        driver.submitCardSelection(you, listOf(salvageId))
        driver.settle(you)

        withClue("the spell resolved under your control — the life is yours, not its owner's") {
            driver.getLifeTotal(you) shouldBe yourLifeBefore + 5
            driver.getLifeTotal(opponent) shouldBe opponentLifeBefore
        }
        withClue("no mana was spent on it: the pool paid only for Jetsam itself") {
            driver.getGraveyardCardNames(opponent).contains("Test Jetsam Salvage") shouldBe false
        }
        withClue("\"exile it instead\" — the spell doesn't fall back into the graveyard it came from") {
            driver.getExile(opponent).contains(salvageId) shouldBe true
        }
    }

    test("declining leaves the opponent's graveyard alone") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val card = driver.putCardInHand(you, "Flotsam // Jetsam")
        val salvageId = driver.putCardInGraveyard(opponent, "Test Jetsam Salvage")
        driver.giveMana(you, Color.BLUE, 6)

        driver.submit(
            CastSpell(you, card, faceIndex = 1, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val yourLifeBefore = driver.getLifeTotal(you)
        driver.submitCardSelection(you, emptyList())
        driver.settle(you)

        withClue("\"you may cast\" — nothing was cast and nothing moved") {
            driver.getLifeTotal(you) shouldBe yourLifeBefore
            driver.getGraveyardCardNames(opponent).contains("Test Jetsam Salvage") shouldBe true
            driver.getExile(opponent).contains(salvageId) shouldBe false
        }
        withClue("no destination rider is left behind on a card that wasn't cast") {
            driver.state.getEntity(salvageId)
                ?.get<com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent>() shouldBe null
        }
    }

    test("both halves are on the definition with their own costs and types") {
        FlotsamJetsam.cardFaces.map { it.name } shouldBe listOf("Flotsam", "Jetsam")
        FlotsamJetsam.cardFaces[0].manaCost.toString() shouldBe "{1}{G/U}"
        FlotsamJetsam.cardFaces[1].manaCost.toString() shouldBe "{4}{U/B}{U/B}"
        FlotsamJetsam.cardFaces[0].typeLine.toString() shouldBe "Instant"
        FlotsamJetsam.cardFaces[1].typeLine.toString() shouldBe "Sorcery"
    }
})
