package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.Escaped
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Escape (CR 702.138, Theros Beyond Death).
 *
 * - 702.138a: "Escape [cost]" — you may cast this card from your graveyard by paying [cost]
 *   rather than its mana cost. The usual non-mana half is "exile N other cards from your
 *   graveyard"; the card being cast is on the stack by then and can't pay for itself.
 * - 702.138b: a spell or permanent "escaped" if it (or the spell that became it) was cast from a
 *   graveyard with an escape ability — a hard-cast copy of the same card did not escape.
 * - 702.138c: "escapes with [counters]" — if it escaped, it enters with those counters.
 * - Unlike flashback, escape doesn't exile the card on resolution: a permanent stays, an
 *   instant/sorcery goes back to the graveyard (and could escape again).
 */
class EscapeTest : FunSpec({

    // {3}{B} 3/3, Escape—{B}, Exile two other cards; escapes with a +1/+1 counter.
    val escapeBeast = card("Escape Beast") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.escape("{B}", Costs.additional.ExileOtherCards(2)))
        replacementEffect(EntersWithCounters(count = 1, selfOnly = true, condition = Conditions.Escaped))
    }

    // Instant, Escape—{R}, Exile one other card; gains 5 life if it escaped, else 2.
    val escapeBolt = card("Escape Bolt") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.If(
                condition = Conditions.Escaped,
                then = Effects.GainLife(5),
                otherwise = Effects.GainLife(2)
            )
        }
        keywordAbility(KeywordAbility.escape("{R}", Costs.additional.ExileOtherCards(1)))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(escapeBeast, escapeBolt))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
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

    fun plusOneCounters(driver: GameTestDriver, id: EntityId) =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun escaped(driver: GameTestDriver, id: EntityId, controller: EntityId) =
        PredicateEvaluator(cardRegistry = null).conditions.evaluate(
            driver.state, Escaped, EffectContext(sourceId = id, controllerId = controller)
        )

    test("escape is offered from the graveyard, and the exile pool never includes the card itself") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCardInGraveyard(player, "Escape Beast")
        val other1 = driver.putCardInGraveyard(player, "Swamp")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        // The beast plus one other card: "two other" can't be paid.
        escapeOffers(driver, player, beast).single().affordable.shouldBeFalse()

        val other2 = driver.putCardInGraveyard(player, "Swamp")
        val offer = escapeOffers(driver, player, beast).single()
        offer.affordable.shouldBeTrue()
        offer.actionType shouldBe "CastWithEscape"
        val pool = offer.additionalCostInfo.shouldNotBeNull().validExileTargets
        pool shouldContainAll listOf(other1, other2)
        pool shouldNotContain beast
    }

    test("escaping a permanent pays the escape cost, stays on the battlefield, and escapes with its counter") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCardInGraveyard(player, "Escape Beast")
        val others = listOf(driver.putCardInGraveyard(player, "Swamp"), driver.putCardInGraveyard(player, "Swamp"))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        val result = driver.submit(escape(player, beast, others))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll others
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Escape Beast").shouldNotBeNull()
        driver.getExile(player) shouldNotContain beast
        plusOneCounters(driver, perm) shouldBe 1
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
        escaped(driver, perm, player).shouldBeTrue()
    }

    test("the card being cast can't be one of the cards it exiles, and each card counts once") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCardInGraveyard(player, "Escape Beast")
        val other = driver.putCardInGraveyard(player, "Swamp")
        driver.putCardInGraveyard(player, "Swamp")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        driver.submitExpectFailure(escape(player, beast, listOf(beast, other)))
        driver.submitExpectFailure(escape(player, beast, listOf(other, other)))
        driver.getGraveyard(player) shouldContain beast
    }

    test("escape needs the escape ability's zone: a card in hand can't be cast with escape") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCardInHand(player, "Escape Beast")
        val others = listOf(driver.putCardInGraveyard(player, "Swamp"), driver.putCardInGraveyard(player, "Swamp"))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 1)

        escapeOffers(driver, player, beast).isEmpty().shouldBeTrue()
        driver.submitExpectFailure(escape(player, beast, others))
    }

    test("a hard-cast permanent did not escape: no counter, no flag (CR 702.138b)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCardInHand(player, "Escape Beast")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 4)

        val result = driver.submit(CastSpell(player, beast, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Escape Beast").shouldNotBeNull()
        plusOneCounters(driver, perm) shouldBe 0
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.ESCAPED).shouldBeNull()
        escaped(driver, perm, player).shouldBeFalse()
    }

    test("an escaped instant reads Escaped while resolving and returns to the graveyard, not exile") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bolt = driver.putCardInGraveyard(player, "Escape Bolt")
        val other = driver.putCardInGraveyard(player, "Swamp")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 1)
        val lifeBefore = driver.getLifeTotal(player)

        val result = driver.submit(escape(player, bolt, listOf(other)))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 5
        driver.getGraveyard(player) shouldContain bolt
        driver.getExile(player) shouldNotContain bolt
        driver.getExile(player) shouldContain other
    }

    test("a hard-cast instant with escape did not escape") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bolt = driver.putCardInHand(player, "Escape Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 3)
        val lifeBefore = driver.getLifeTotal(player)

        driver.submit(CastSpell(player, bolt, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 2
    }
})
