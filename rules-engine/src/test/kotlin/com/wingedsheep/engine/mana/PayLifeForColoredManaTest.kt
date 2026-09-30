package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PayLifeForColoredMana
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [PayLifeForColoredMana] — "For each {B} in a cost, you may pay 2 life rather than pay that mana"
 * (K'rrik, Son of Yawgmoth). Each case pins a rule or ruling:
 *
 *  - any {B} in a spell's mana cost or an activation cost may be paid with 2 life, explicitly or by
 *    auto-pay when mana can't cover it (the Phyrexian path, CR 107.4f);
 *  - generic mana can never be paid with life, and neither can a monocolored hybrid `{2/B}`;
 *  - a hybrid with a black half may be paid as {B} and then with life (ruling);
 *  - only the static's controller benefits; without it {B} is mana-only;
 *  - CR 119.4 — no paying more life than you have;
 *  - it doesn't change the cost: reductions still apply first, mana value is unchanged;
 *  - compleated (CR 702.150a) counts only Phyrexian symbols paid with life.
 */
class PayLifeForColoredManaTest : FunSpec({

    val lifeSource = card("Life Substitution Source") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "For each {B} in a cost, you may pay 2 life rather than pay that mana."
        staticAbility { ability = PayLifeForColoredMana(Color.BLACK) }
    }

    fun sorcery(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    val blackProbe = sorcery("Black Life Probe", "{1}{B}")
    val genericProbe = sorcery("Generic Life Probe", "{2}")
    val hybridProbe = sorcery("Hybrid Life Probe", "{R/B}")
    val twobridProbe = sorcery("Twobrid Life Probe", "{2/B}")
    val redProbe = sorcery("Red Life Probe", "{R}")

    val blackSiphon = card("Black Life Siphon") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{B}: Draw a card."
        activatedAbility {
            cost = Costs.Mana("{B}")
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }

    val walker = card("Compleated Life Walker") {
        manaCost = "{B}{B/P}"
        typeLine = "Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "Compleated\n+1: Draw a card."
        keywords(Keyword.COMPLEATED)
        loyaltyAbility(+1) { effect = Effects.DrawCards(1) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                lifeSource, blackProbe, genericProbe, hybridProbe, twobridProbe, redProbe, blackSiphon, walker
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.castOption(player: EntityId, cardId: EntityId) =
        legalActions(player).firstOrNull { (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.canCast(player: EntityId, cardId: EntityId) = castOption(player, cardId)?.affordable == true

    fun explicitLife(vararg colors: Color, sources: List<EntityId> = emptyList()) =
        PaymentStrategy.Explicit(manaAbilitiesToActivate = sources, phyrexianLifePayments = colors.toList())

    test("ManaCost.withLifePayable rewrites only the color's pips and keeps mana value") {
        val cost = ManaCost.parse("{2}{B}{B/R}{G/B}{2/B}{B/P}{C}{U}")
        val lowered = cost.withLifePayable(Color.BLACK)
        lowered.toString() shouldBe "{2}{B/P}{B/R/P}{B/G/P}{2/B}{B/P}{C}{U}"
        lowered.cmc shouldBe cost.cmc
        withClue("idempotent") { lowered.withLifePayable(Color.BLACK) shouldBe lowered }
    }

    test("auto-pay pays a spell's {B} with 2 life when no black source exists") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val island = driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "Black Life Probe")

        withClue("affordable only through life") { driver.canCast(me, spell) shouldBe true }
        withClue("the cast cost shows the life option") {
            driver.castOption(me, spell)?.manaCostString shouldBe "{1}{B/P}"
        }
        val hand = driver.getHandSize(me)
        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))
        driver.isTapped(island) shouldBe true
        driver.getLifeTotal(me) shouldBe 18
        driver.bothPass()
        driver.getHandSize(me) shouldBe hand
    }

    test("an explicit life payment is honoured even with a Swamp untapped") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val island = driver.putLandOnBattlefield(me, "Island")
        val swamp = driver.putLandOnBattlefield(me, "Swamp")
        val spell = driver.putCardInHand(me, "Black Life Probe")

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.BLACK, sources = listOf(island))))
        driver.getLifeTotal(me) shouldBe 18
        driver.isTapped(island) shouldBe true
        driver.isTapped(swamp) shouldBe false
    }

    test("auto-pay prefers mana when a black source can pay") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        driver.putLandOnBattlefield(me, "Island")
        val swamp = driver.putLandOnBattlefield(me, "Swamp")
        val spell = driver.putCardInHand(me, "Black Life Probe")

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))
        driver.isTapped(swamp) shouldBe true
        driver.getLifeTotal(me) shouldBe 20
    }

    test("without the static a {B} can't be paid with life") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "Black Life Probe")

        driver.canCast(me, spell) shouldBe false
        driver.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.BLACK))).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20
    }

    test("an opponent's static doesn't let you pay life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Life Substitution Source")
        driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "Black Life Probe")

        driver.canCast(me, spell) shouldBe false
        driver.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.BLACK))).error shouldNotBe null
    }

    test("generic mana can't be paid with life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val spell = driver.putCardInHand(me, "Generic Life Probe")

        driver.canCast(me, spell) shouldBe false
    }

    test("a pip of another color can't be paid with life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val spell = driver.putCardInHand(me, "Red Life Probe")

        driver.canCast(me, spell) shouldBe false
        driver.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.RED))).error shouldNotBe null
    }

    test("a hybrid with a black half may be paid with life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")
        val spell = driver.putCardInHand(me, "Hybrid Life Probe")

        driver.castOption(me, spell)?.manaCostString shouldBe "{B/R/P}"
        driver.submitSuccess(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.BLACK)))
        driver.getLifeTotal(me) shouldBe 18
        driver.isTapped(mountain) shouldBe false
    }

    test("a monocolored hybrid {2/B} stays mana-only") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val spell = driver.putCardInHand(me, "Twobrid Life Probe")

        driver.canCast(me, spell) shouldBe false
    }

    test("a player can't pay more life than they have (CR 119.4)") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        driver.putLandOnBattlefield(me, "Island")
        driver.setLifeTotal(me, 1)
        val spell = driver.putCardInHand(me, "Black Life Probe")

        driver.canCast(me, spell) shouldBe false
        driver.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = explicitLife(Color.BLACK))).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 1
    }

    test("an activation cost's {B} may be paid with life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")
        val siphon = driver.putPermanentOnBattlefield(me, "Black Life Siphon")
        val abilityId = driver.cardRegistry.requireCard("Black Life Siphon").activatedAbilities[0].id
        val option = driver.legalActions(me).first { (it.action as? ActivateAbility)?.sourceId == siphon }

        option.affordable shouldBe true
        option.manaCostString shouldBe "{B/P}"
        val hand = driver.getHandSize(me)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = siphon, abilityId = abilityId))
        driver.getLifeTotal(me) shouldBe 18
        driver.bothPass()
        driver.getHandSize(me) shouldBe hand + 1
    }

    test("compleated counts only Phyrexian pips paid with life") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Substitution Source")

        val swamp = driver.putLandOnBattlefield(me, "Swamp")
        val first = driver.putCardInHand(me, "Compleated Life Walker")
        driver.submitSuccess(CastSpell(playerId = me, cardId = first, paymentStrategy = explicitLife(Color.BLACK, sources = listOf(swamp))))
        driver.bothPass()
        withClue("one life payment is attributed to the substituted {B}: full loyalty") {
            driver.getLifeTotal(me) shouldBe 18
            driver.state.getEntity(first)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5
        }

        val second = driver.putCardInHand(me, "Compleated Life Walker")
        driver.submitSuccess(CastSpell(playerId = me, cardId = second, paymentStrategy = explicitLife(Color.BLACK, Color.BLACK)))
        driver.bothPass()
        withClue("two life payments: the {B/P} was one of them — two fewer loyalty") {
            driver.getLifeTotal(me) shouldBe 14
            driver.state.getEntity(second)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
        }
    }
})
