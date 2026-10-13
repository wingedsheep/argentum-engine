package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `GraveyardCardsHaveEscape` — the whole-graveyard Escape (CR 702.138) group grant behind
 * Underworld Breach: "Each nonland card in your graveyard has escape. The escape cost is equal to
 * the card's mana cost plus exile three other cards from your graveyard." Routed through
 * `EscapeCasts.effectiveEscape`, so a granted escape behaves like a printed one:
 *  - 702.138a: cast from the graveyard for the escape cost; the exile pool never holds the card itself.
 *  - not exiled on resolution — an escaped sorcery goes back to the graveyard and can escape again.
 *  - 702.138b: a permanent cast this way "escaped".
 *  - Ruling: a card with no mana cost has an unpayable escape cost, so "equal to its mana cost" grants nothing.
 */
class GraveyardCardsHaveEscapeTest : FunSpec({

    val breach = card("Escape Grant Test") {
        manaCost = "{1}{R}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GraveyardCardsHaveEscape(
                filter = GameObjectFilter.Nonland,
                additionalCost = Costs.additional.ExileOtherCards(3),
            )
        }
    }

    // A fixed-cost, during-your-turn grant, to pin the two optional axes.
    val fixedBreach = card("Fixed Escape Grant Test") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GraveyardCardsHaveEscape(
                filter = GameObjectFilter.Nonland,
                cost = ManaCost.parse("{B}"),
                duringYourTurnOnly = true,
            )
        }
    }

    val gainSorcery = card("Escape Gain Sorcery") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    val gainInstant = card("Escape Gain Instant") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
    }

    val bear = card("Escape Grant Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // No mana cost at all (Ancestral Vision shape) — distinct from a printed {0}.
    val costless = card("Escape Costless Sorcery") {
        manaCost = ""
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    // A printed escape: the printed one wins over the grant.
    val printed = card("Escape Printed Sorcery") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
        keywordAbility(KeywordAbility.escape("{R}", Costs.additional.ExileOtherCards(1)))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(breach, fixedBreach, gainSorcery, gainInstant, bear, costless, printed))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    fun escapeOffers(driver: GameTestDriver, playerId: EntityId, cardId: EntityId): List<LegalAction> =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .filter { (it.action as? CastSpell)?.let { a -> a.cardId == cardId && a.alternativeCostType == AlternativeCostType.ESCAPE } == true }

    fun escape(playerId: EntityId, cardId: EntityId, exiled: List<EntityId>) = CastSpell(
        playerId = playerId,
        cardId = cardId,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool
    )

    fun fillGraveyard(driver: GameTestDriver, playerId: EntityId, n: Int) =
        List(n) { driver.putCardInGraveyard(playerId, "Mountain") }

    test("the grant offers escape at the card's mana cost, with the card itself outside the exile pool") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Escape Grant Test")
        val sorcery = driver.putCardInGraveyard(you, "Escape Gain Sorcery")
        fillGraveyard(driver, you, 2)
        driver.giveMana(you, Color.RED, 2)

        // Two other cards can't pay "exile three other cards".
        escapeOffers(driver, you, sorcery).single().affordable.shouldBeFalse()

        driver.putCardInGraveyard(you, "Mountain")
        val offer = escapeOffers(driver, you, sorcery).single()
        offer.affordable.shouldBeTrue()
        offer.manaCostString shouldBe "{1}{R}"
        offer.additionalCostInfo.shouldNotBeNull().validExileTargets shouldNotContain sorcery
    }

    test("without the granter, a plain graveyard card has no escape") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val sorcery = driver.putCardInGraveyard(you, "Escape Gain Sorcery")
        fillGraveyard(driver, you, 3)
        driver.giveMana(you, Color.RED, 2)

        escapeOffers(driver, you, sorcery).shouldBeEmpty()
    }

    test("the Nonland filter excludes lands, and a card with no mana cost can't escape") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Escape Grant Test")
        val lands = fillGraveyard(driver, you, 4)
        val costlessCard = driver.putCardInGraveyard(you, "Escape Costless Sorcery")
        driver.giveMana(you, Color.RED, 2)

        escapeOffers(driver, you, lands.first()).shouldBeEmpty()
        escapeOffers(driver, you, costlessCard).shouldBeEmpty()
    }

    test("an escaped sorcery pays the exile half, resolves, and returns to the graveyard to escape again") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Escape Grant Test")
        val sorcery = driver.putCardInGraveyard(you, "Escape Gain Sorcery")
        val others = fillGraveyard(driver, you, 6)
        driver.giveMana(you, Color.RED, 2)
        val life = driver.getLifeTotal(you)

        val result = driver.submit(escape(you, sorcery, others.take(3)))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(you) shouldContainAll others.take(3)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(you) shouldBe life + 3
        driver.getGraveyard(you) shouldContain sorcery
        driver.getExile(you) shouldNotContain sorcery

        driver.giveMana(you, Color.RED, 2)
        escapeOffers(driver, you, sorcery).single().affordable.shouldBeTrue()
    }

    test("a permanent cast with granted escape escaped and stays on the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Escape Grant Test")
        val bearCard = driver.putCardInGraveyard(you, "Escape Grant Bear")
        val others = fillGraveyard(driver, you, 3)
        driver.giveMana(you, Color.GREEN, 2)

        val result = driver.submit(escape(you, bearCard, others))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(you, "Escape Grant Bear").shouldNotBeNull()
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
    }

    test("printed and graveyard-wide escape abilities offer both costs") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Escape Grant Test")
        val card = driver.putCardInGraveyard(you, "Escape Printed Sorcery")
        fillGraveyard(driver, you, 1)
        driver.giveMana(you, Color.RED, 1)

        val offers = escapeOffers(driver, you, card)
        offers.size shouldBe 2
        val offer = offers.single { it.manaCostString == "{R}" }
        offer.affordable.shouldBeTrue()
        offer.manaCostString shouldBe "{R}"
    }

    test("a fixed-cost grant uses its own cost, and a during-your-turn grant is off on the opponent's turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Fixed Escape Grant Test")
        driver.putPermanentOnBattlefield(opponent, "Fixed Escape Grant Test")
        val theirs = driver.putCardInGraveyard(opponent, "Escape Gain Instant")
        driver.giveMana(opponent, Color.BLACK, 1)

        // Opponent's during-your-turn grant, on your turn: their instant has no escape.
        escapeOffers(driver, opponent, theirs).shouldBeEmpty()

        val yours = driver.putCardInGraveyard(you, "Escape Gain Instant")
        driver.giveMana(you, Color.BLACK, 1)
        val offer = escapeOffers(driver, you, yours).single()
        offer.manaCostString shouldBe "{B}"
        offer.affordable.shouldBeTrue()
    }
})
