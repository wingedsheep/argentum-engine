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
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import io.kotest.matchers.shouldNotBe

/**
 * `Effects.GrantEscape` — the single-card runtime Escape (CR 702.138) grant behind Desdemona,
 * Freedom's Edge: "target creature card in your graveyard … gains escape until end of turn. The
 * escape cost is equal to its mana cost plus exile two other cards from your graveyard." Read
 * through `EscapeCasts.effectiveEscape`, so a granted escape behaves like a printed one:
 *  - 702.138a: cast from the graveyard for the escape cost; the card itself is never in its exile pool.
 *  - 702.138b: a permanent cast this way "escaped" and stays on the battlefield (no exile).
 *  - "until end of turn": the grant expires in cleanup.
 *  - 400.7: the permanent it becomes is a new object — dying again the same turn, it has no escape.
 *  - Multiple escape abilities offer a choice of costs.
 */
class GrantEscapeTest : FunSpec({

    val granter = card("Escape Granting Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Any, zone = Zone.GRAVEYARD).ownedByYou())
            effect = Effects.GrantEscape(t, additionalCost = Costs.additional.ExileOtherCards(2))
        }
    }

    val bear = card("Granted Escape Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val instant = card("Granted Escape Instant") {
        manaCost = "{1}{G}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
    }

    val printed = card("Granted Escape Printed Sorcery") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
        keywordAbility(KeywordAbility.escape("{R}", Costs.additional.ExileOtherCards(1)))
    }

    val zero = card("Zero Escape Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 1
        toughness = 1
    }
    val noCost = card("No Cost Escape Bear") {
        typeLine = "Creature — Bear"
        power = 1
        toughness = 1
    }

    val adventure = card("Escape Adventure Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
        adventure("Escape Adventure Bolt") {
            manaCost = "{R}"
            typeLine = "Instant — Adventure"
            additionalCost(Costs.additional.PayLife(1))
            spell {
                val t = target(TargetFilter.Creature)
                effect = Effects.DealDamage(2, t)
            }
        }
    }
    val instantDiscount = card("Escape Instant Discounter") {
        typeLine = "Enchantment"
        staticAbility {
            ability = com.wingedsheep.sdk.scripting.ModifySpellCost(
                com.wingedsheep.sdk.scripting.SpellCostTarget.YouCast(com.wingedsheep.sdk.scripting.GameObjectFilter.Instant),
                com.wingedsheep.sdk.scripting.CostModification.ReduceGeneric(1)
            )
        }
    }
    val flashAdventure = card("Flash Escape Adventure Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(com.wingedsheep.sdk.core.Keyword.FLASH)
        adventure("Sorcery Escape Adventure") {
            manaCost = "{R}"
            typeLine = "Sorcery — Adventure"
            spell { effect = Effects.GainLife(2) }
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(granter, bear, instant, printed, zero, noCost, adventure, instantDiscount, flashAdventure))
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

    fun grantEscape(driver: GameTestDriver, you: EntityId, cardId: EntityId) {
        val spell = driver.putCardInHand(you, "Escape Granting Sorcery")
        driver.giveMana(you, Color.RED, 1)
        val result = driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Card(cardId, you, Zone.GRAVEYARD)))
        withClue("error=${result.error}") { result.error shouldBe null }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun fillGraveyard(driver: GameTestDriver, playerId: EntityId, n: Int) =
        List(n) { driver.putCardInGraveyard(playerId, "Mountain") }

    test("without the grant, a plain graveyard creature card has no escape") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearCard = driver.putCardInGraveyard(you, "Granted Escape Bear")
        fillGraveyard(driver, you, 2)
        driver.giveMana(you, Color.GREEN, 2)

        escapeOffers(driver, you, bearCard).shouldBeEmpty()
    }

    test("granted escape costs the card's mana cost plus two other cards; the card is outside its own exile pool") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearCard = driver.putCardInGraveyard(you, "Granted Escape Bear")
        val one = fillGraveyard(driver, you, 1)
        grantEscape(driver, you, bearCard)
        driver.giveMana(you, Color.GREEN, 2)

        // The resolved granting sorcery is in the graveyard too, so two other cards are available.
        val offer = escapeOffers(driver, you, bearCard).single()
        offer.manaCostString shouldBe "{1}{G}"
        val pool = offer.additionalCostInfo.shouldNotBeNull().validExileTargets
        pool shouldNotContain bearCard
        pool shouldContainAll one
    }

    test("with only one other card in the graveyard, the granted escape is unaffordable") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearCard = driver.putCardInGraveyard(you, "Granted Escape Bear")
        grantEscape(driver, you, bearCard) // the resolved granter is the one other card
        driver.giveMana(you, Color.GREEN, 2)

        escapeOffers(driver, you, bearCard).single().affordable.shouldBeFalse()
    }

    test("a creature cast with granted escape pays the exile half, escaped, and stays on the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearCard = driver.putCardInGraveyard(you, "Granted Escape Bear")
        val others = fillGraveyard(driver, you, 2)
        grantEscape(driver, you, bearCard)
        driver.giveMana(you, Color.GREEN, 2)

        // The card being cast can't be named in its own exile pool.
        driver.submit(escape(you, bearCard, listOf(bearCard, others.first()))).outcome shouldNotBe Outcome.Done

        val result = driver.submit(escape(you, bearCard, others))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(you) shouldContainAll others
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(you, "Granted Escape Bear").shouldNotBeNull()
        perm shouldBe bearCard
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
        driver.getExile(you) shouldNotContain bearCard

        // CR 400.7: the permanent is a new object; back in the graveyard it has no escape.
        driver.state.grantedKeywordAbilities.none { it.entityId == bearCard }.shouldBeTrue()
        driver.moveToGraveyard(perm)
        fillGraveyard(driver, you, 2)
        driver.giveMana(you, Color.GREEN, 2)
        escapeOffers(driver, you, bearCard).shouldBeEmpty()
    }

    test("the grant ends at end of turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val instantCard = driver.putCardInGraveyard(you, "Granted Escape Instant")
        fillGraveyard(driver, you, 2)
        grantEscape(driver, you, instantCard)
        driver.giveMana(you, Color.GREEN, 2)
        escapeOffers(driver, you, instantCard).single().affordable.shouldBeTrue()

        // On the opponent's turn an instant could still escape — but the grant is gone.
        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.state.grantedKeywordAbilities.shouldBeEmpty()
        driver.getGraveyard(you) shouldContain instantCard
        driver.giveMana(you, Color.GREEN, 2)
        escapeOffers(driver, you, instantCard).shouldBeEmpty()
    }

    test("a printed escape and a granted escape offer both costs") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCardInGraveyard(you, "Granted Escape Printed Sorcery")
        fillGraveyard(driver, you, 1)
        grantEscape(driver, you, card)
        driver.giveMana(you, Color.RED, 1)

        val offers = escapeOffers(driver, you, card)
        offers.size shouldBe 2
        val offer = offers.single { it.manaCostString == "{R}" }
        offer.affordable.shouldBeTrue()
        offer.manaCostString shouldBe "{R}"
    }
    test("a zero mana cost is payable but an absent mana cost is not") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val free = driver.putCardInGraveyard(you, "Zero Escape Bear")
        val absent = driver.putCardInGraveyard(you, "No Cost Escape Bear")
        fillGraveyard(driver, you, 2)
        grantEscape(driver, you, free)
        grantEscape(driver, you, absent)
        escapeOffers(driver, you, free).single().affordable.shouldBeTrue()
        escapeOffers(driver, you, absent).shouldBeEmpty()
    }

    test("casting the granted escape option uses its own mana and exile costs") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCardInGraveyard(you, "Granted Escape Printed Sorcery")
        val others = fillGraveyard(driver, you, 2)
        grantEscape(driver, you, card)
        driver.giveMana(you, Color.RED, 4)
        val option = escapeOffers(driver, you, card).single { it.manaCostString == "{3}{R}" }
        val action = (option.action as CastSpell).copy(
            additionalCostPayment = AdditionalCostPayment(exiledCards = others),
            paymentStrategy = PaymentStrategy.FromPool
        )
        driver.submit(action.copy(escapeAbility = KeywordAbility.Escape(com.wingedsheep.sdk.core.ManaCost.ZERO))).outcome shouldNotBe Outcome.Done
        driver.submit(action.copy(additionalCostPayment = AdditionalCostPayment(exiledCards = others.take(1)))).outcome shouldNotBe Outcome.Done
        driver.submit(action).outcome shouldBe Outcome.Done
        driver.getExile(you) shouldContainAll others
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getGraveyard(you) shouldContain card
        driver.state.grantedKeywordAbilities.none { it.entityId == card }.shouldBeTrue()
    }

    test("a countered escaped creature cannot reuse its expired object grant") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCardInGraveyard(you, "Granted Escape Bear")
        val others = fillGraveyard(driver, you, 2)
        grantEscape(driver, you, card)
        driver.giveMana(you, Color.GREEN, 2)
        driver.submit(escape(you, card, others)).outcome shouldBe Outcome.Done
        driver.passPriority(you)
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.castSpellWithTargets(opponent, counter, listOf(ChosenTarget.Spell(card))).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getGraveyard(you) shouldContain card
        fillGraveyard(driver, you, 2)
        driver.giveMana(you, Color.GREEN, 2)
        escapeOffers(driver, you, card).shouldBeEmpty()
    }

    test("an instant Adventure escapes outside main phase with its own targets and the selected escape base") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCardInGraveyard(you, "Escape Adventure Bear")
        val others = fillGraveyard(driver, you, 2)
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        grantEscape(driver, you, card)
        driver.putPermanentOnBattlefield(you, "Escape Instant Discounter")
        driver.giveMana(you, Color.GREEN, 3)
        val mainOffers = escapeOffers(driver, you, card)
        mainOffers.size shouldBe 2
        mainOffers.single { (it.action as CastSpell).faceIndex == null }.manaCostString shouldBe "{2}{G}"
        mainOffers.single { (it.action as CastSpell).faceIndex == 0 }.manaCostString shouldBe "{1}{G}"

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.giveMana(you, Color.GREEN, 2)
        val offer = escapeOffers(driver, you, card).single()
        (offer.action as CastSpell).faceIndex shouldBe 0
        offer.requiresTargets.shouldBeTrue()
        offer.validTargets.shouldNotBeNull() shouldContain victim
        val action = (offer.action as CastSpell).copy(
            targets = listOf(ChosenTarget.Permanent(victim)),
            additionalCostPayment = AdditionalCostPayment(exiledCards = others),
            paymentStrategy = PaymentStrategy.FromPool,
        )
        val result = driver.submit(action)
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getLifeTotal(you) shouldBe 19
        driver.getExile(you) shouldContainAll others
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getGraveyard(opponent) shouldContain victim
        driver.getExile(you) shouldContain card
    }

    test("a sorcery Adventure does not inherit flash from its creature face") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCardInGraveyard(you, "Flash Escape Adventure Bear")
        val others = fillGraveyard(driver, you, 2)
        grantEscape(driver, you, card)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.giveMana(you, Color.GREEN, 2)
        val offers = escapeOffers(driver, you, card)
        offers.single().action.let { it as CastSpell }.faceIndex shouldBe null
        driver.submit(escape(you, card, others).copy(faceIndex = 0)).outcome shouldNotBe Outcome.Done
    }

})
