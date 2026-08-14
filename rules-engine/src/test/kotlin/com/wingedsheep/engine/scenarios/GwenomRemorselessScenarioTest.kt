package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.GwenomRemorseless
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gwenom, Remorseless (SPM) — "Whenever Gwenom attacks, until end of turn, you may play cards from
 * the top of your library. If you cast a spell this way, pay life equal to its mana value rather
 * than pay its mana cost."
 *
 * Pins the new `PlayFromTopWithAlternativeCost` permission granted durationally: after Gwenom
 * attacks, the top card of the library becomes castable for life (mana waived), and it isn't
 * before the attack.
 */
class GwenomRemorselessScenarioTest : FunSpec({

    // A vanilla creature ({4}{G}, mana value 5) to sit on top of the library.
    val topBeast = CardDefinition(
        name = "Top Beast",
        manaCost = ManaCost.parse("{4}{G}"),
        typeLine = TypeLine.parse("Creature — Beast"),
        oracleText = "",
        creatureStats = CreatureStats(3, 3),
    )

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GwenomRemorseless, topBeast))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var g = 0
        while (g++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun topOfLibraryCastCardIds(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .filter { it.sourceZone == "LIBRARY" }
            .mapNotNull { it.action as? CastSpell }
            .map { it.cardId }

    test("after Gwenom attacks, the top spell is castable for life; not before") {
        val (driver, you, opponent) = newGame()
        val gwenom = driver.putCreatureOnBattlefield(you, "Gwenom, Remorseless")
        driver.removeSummoningSickness(gwenom)
        val beast = driver.putCardOnTopOfLibrary(you, "Top Beast")

        // Before attacking: no permission, so the top card isn't castable.
        (beast in topOfLibraryCastCardIds(driver, you)) shouldBe false

        // Gwenom attacks → the attack trigger grants the play-from-top permission until end of turn.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(gwenom), opponent)
        resolveStack(driver)
        driver.declareNoBlockers(opponent)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        resolveStack(driver)

        // Now the top card is castable from the library.
        (beast in topOfLibraryCastCardIds(driver, you)) shouldBe true

        // Cast it: no mana is given, and it costs 5 life (its mana value).
        val lifeBefore = driver.getLifeTotal(you)
        val result = driver.submit(
            CastSpell(playerId = you, cardId = beast, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldBe null
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore - 5           // paid life = mana value
        (driver.findPermanent(you, "Top Beast") != null) shouldBe true  // resolved onto the battlefield
    }

    // Regression: the attack also grants LookAtTopOfLibrary, so the controller must actually SEE the
    // top card in their client view — otherwise it is castable but invisible, so there is nothing to
    // play. The granted static lives in `grantedStaticAbilities` (not the card's printed statics), a
    // path the visibility layer previously ignored.
    test("after Gwenom attacks, the top card of the library is revealed to its controller in the client view") {
        val (driver, you, opponent) = newGame()
        val gwenom = driver.putCreatureOnBattlefield(you, "Gwenom, Remorseless")
        driver.removeSummoningSickness(gwenom)
        val beast = driver.putCardOnTopOfLibrary(you, "Top Beast")

        // Before the attack there is no look permission, so the top card stays hidden.
        val before = ClientStateTransformer(driver.cardRegistry).transform(driver.state, viewingPlayerId = you)
        (beast in before.cards) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(gwenom), opponent)
        resolveStack(driver)

        // The granted LookAtTopOfLibrary now reveals the top card to its controller.
        val yourView = ClientStateTransformer(driver.cardRegistry).transform(driver.state, viewingPlayerId = you)
        (beast in yourView.cards) shouldBe true
        yourView.cards[beast]?.name shouldBe "Top Beast"

        // It is a private look, not a public reveal — the opponent still cannot see it.
        val oppView = ClientStateTransformer(driver.cardRegistry).transform(driver.state, viewingPlayerId = opponent)
        (beast in oppView.cards) shouldBe false
    }

    // The grant is `Duration.EndOfTurn`: on a later turn the top card must be neither castable nor
    // revealed again. Without this, a permanent grant would pass every "yes after the attack" check.
    test("the play-from-top permission and top-card reveal expire at end of turn") {
        val (driver, you, opponent) = newGame()
        val gwenom = driver.putCreatureOnBattlefield(you, "Gwenom, Remorseless")
        driver.removeSummoningSickness(gwenom)
        val beast = driver.putCardOnTopOfLibrary(you, "Top Beast")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(gwenom), opponent)
        resolveStack(driver)
        driver.declareNoBlockers(opponent)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        resolveStack(driver)

        // Active this turn: in the post-combat main the (sorcery-speed) top creature is castable, and
        // the top card is revealed to the controller.
        (beast in topOfLibraryCastCardIds(driver, you)) shouldBe true
        val duringTurn = ClientStateTransformer(driver.cardRegistry).transform(driver.state, viewingPlayerId = you)
        (beast in duringTurn.cards) shouldBe true

        // Advance into the next turn; the EndOfTurn grant is dropped at cleanup. Stop in the
        // opponent's precombat main so `you` hasn't drawn the beast off the top.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        (beast in topOfLibraryCastCardIds(driver, you)) shouldBe false
        val nextTurn = ClientStateTransformer(driver.cardRegistry).transform(driver.state, viewingPlayerId = you)
        (beast in nextTurn.cards) shouldBe false
    }
})
