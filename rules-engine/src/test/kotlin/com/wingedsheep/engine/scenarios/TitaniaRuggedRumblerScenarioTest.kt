package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.TitaniaRuggedRumbler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Titania, Rugged Rumbler (MSH #235) — "As an additional cost to cast this spell, discard a card or
 * pay {2}" plus "Ward—Discard a card or pay {2}."
 *
 * The point of the card, and of this test, is that the *same printed shape* rides two different
 * rails: the cast side folds its mana leg into Titania's own mana cost (`AdditionalCost.OrPay`),
 * the ward side pays its mana leg standing alone (`WardCost.Choice`).
 *
 * Both ward legs are paid here, plus the ward's decline. On the cast side the assertion is
 * *enumeration* — that both a discard cast and a pay-{2}-more cast are offered — not payment: the
 * or-pay additional cost itself is pre-existing and is exercised end to end elsewhere (Pumpkin
 * Bombardment), so what is new about Titania is that the two rails coexist on one card.
 */
class TitaniaRuggedRumblerScenarioTest : FunSpec({

    val Zap = card("Titania Test Zap") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(3, victim)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(TitaniaRuggedRumbler)
        registerCard(Zap)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
    }

    test("the cast-time additional cost offers both a discard cast and a pay-{2}-more cast") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val titania = driver.putCardInHand(activePlayer, "Titania, Rugged Rumbler")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        val casts = driver.legalActions(activePlayer).filter {
            (it.action as? CastSpell)?.cardId == titania
        }

        withClue("one castable action per leg of the or-pay additional cost: $casts") {
            casts.size shouldBe 2
        }
    }

    test("ward: the discard leg discards a card and the targeting spell resolves") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val titania = driver.putCreatureOnBattlefield(opponent, "Titania, Rugged Rumbler")

        driver.giveMana(activePlayer, Color.RED, 3)
        val spare = driver.putCardInHand(activePlayer, "Forest")
        val zap = driver.putCardInHand(activePlayer, "Titania Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(titania)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldContainExactly listOf("Discard a card", "Pay {2}", "Counter spell")

        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 0))
        driver.submitYesNo(activePlayer, true)
        if (driver.pendingDecision is SelectCardsDecision) {
            driver.submitCardSelection(activePlayer, listOf(spare))
        }
        repeat(4) { if (driver.state.priorityPlayerId != null && driver.pendingDecision == null) driver.bothPass() }

        driver.getGraveyardCardNames(activePlayer) shouldContain "Forest"
        withClue("3 damage doesn't kill a 5/5, but the spell was not countered") {
            driver.getGraveyardCardNames(activePlayer) shouldContain "Titania Test Zap"
            driver.findPermanent(opponent, "Titania, Rugged Rumbler") shouldNotBe null
        }
    }

    test("ward: the pay leg spends {2} and discards nothing") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val titania = driver.putCreatureOnBattlefield(opponent, "Titania, Rugged Rumbler")

        driver.giveMana(activePlayer, Color.RED, 3)
        val zap = driver.putCardInHand(activePlayer, "Titania Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(titania)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 1))

        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(activePlayer, true)
        repeat(4) { if (driver.state.priorityPlayerId != null && driver.pendingDecision == null) driver.bothPass() }

        driver.getHandSize(activePlayer) shouldBe handAfterCast
        driver.getGraveyardCardNames(activePlayer) shouldContain "Titania Test Zap"
    }

    test("ward: declining counters the spell") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val titania = driver.putCreatureOnBattlefield(opponent, "Titania, Rugged Rumbler")

        driver.giveMana(activePlayer, Color.RED, 3)
        val zap = driver.putCardInHand(activePlayer, "Titania Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(titania)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, decision.options.size - 1))
        repeat(3) { if (driver.state.priorityPlayerId != null && driver.pendingDecision == null) driver.bothPass() }

        withClue("nothing was paid and the spell was countered") {
            driver.getHandSize(activePlayer) shouldBe handAfterCast
            driver.findPermanent(opponent, "Titania, Rugged Rumbler") shouldNotBe null
        }
    }
})
