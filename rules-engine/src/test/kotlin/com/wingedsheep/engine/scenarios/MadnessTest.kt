package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.MadnessExiledComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the Madness [cost] keyword (CR 702.35).
 *
 * *"Madness [cost]" means "If a player would discard this card, that player discards it, but exiles
 * it instead of putting it into their graveyard" and "When this card is exiled this way, its owner
 * may cast it by paying [cost] rather than paying its mana cost. If that player doesn't, they put
 * this card into their graveyard."* (CR 702.35a)
 *
 * The whole point of the mechanic is that it holds for **every** discard — an opponent's effect, a
 * cost payment, cycling, the cleanup-step hand-size discard — so most of the matrix below is one
 * discard route per test. Exercised with inline cards so the engine behavior is pinned independent
 * of any set.
 */
class MadnessTest : FunSpec({

    // A madness SORCERY with no targets — the easiest shape to drive, and the one that proves the
    // timing exception (CR 702.35b: the cast happens while the trigger resolves).
    val madnessBoon = card("Madness Boon") {
        manaCost = "{4}{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(7) }
        madness("{B}")
    }

    // A madness creature, to prove a permanent spell cast this way actually enters the battlefield.
    val madnessBeast = card("Madness Beast") {
        manaCost = "{4}{R}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        madness("{R}")
    }

    // A madness instant with cycling — Fiery Temper's actual line: cycle it, and the cycling
    // discard turns into a cheap cast.
    val madnessSpark = card("Madness Spark") {
        manaCost = "{4}{R}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
        keywordAbility(KeywordAbility.cycling("{1}"))
        madness("{R}")
    }

    // "Discard a card" — the controller's own discard.
    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    // "Each opponent discards a card" — an *opponent's* spell causing the discard, which per
    // CR 702.35a still offers the cast to the card's owner.
    val opponentDiscard = card("Opponent Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.EachOpponentDiscards(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(madnessBoon, madnessBeast, madnessSpark, selfDiscard, opponentDiscard)
        )
        return driver
    }

    /** Discard [cardId] from [player]'s hand by resolving their own "discard a card" sorcery. */
    fun discardViaSpell(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        val spell = driver.putCardInHand(player, "Self Discard")
        driver.giveColorlessMana(player, 1)
        driver.submitSuccess(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        // The discard effect asks which card to pitch.
        driver.submitCardSelection(player, listOf(cardId))
    }

    /**
     * Pass priority until the stack is empty or a decision is waiting. The madness trigger goes on
     * the stack when the discard happens, so its "cast it?" prompt only appears once both players
     * let that trigger resolve.
     */
    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    test("discarding a madness card exiles it instead of putting it into the graveyard (CR 702.35a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        discardViaSpell(driver, player, boon)

        driver.getExile(player) shouldContain boon
        driver.getGraveyard(player).shouldNotContain(boon)
        driver.state.getEntity(boon)?.get<MadnessExiledComponent>().shouldNotBeNull()
    }

    test("a madness card is still discarded — discard tracking sees it (CR 702.35a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        discardViaSpell(driver, player, boon)

        // "Discard it into exile" is still a discard: payoffs that count discards must see it.
        val discarded = driver.state.getEntity(player)?.get<CardsDiscardedThisTurnComponent>()
        discarded.shouldNotBeNull()
        discarded.cardIds shouldContain boon
        discarded.count shouldBe 1
    }

    test("accepting the madness trigger casts the card for its madness cost, not its mana cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        val lifeBefore = driver.getLifeTotal(player)
        discardViaSpell(driver, player, boon)

        // One black — enough for madness {B}, nowhere near the printed {4}{B}.
        driver.giveMana(player, Color.BLACK, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 7
        driver.getGraveyard(player) shouldContain boon
        driver.getExile(player).shouldNotContain(boon)
    }

    test("declining the madness trigger puts the card into its owner's graveyard (CR 702.35a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        val lifeBefore = driver.getLifeTotal(player)
        discardViaSpell(driver, player, boon)

        driver.giveMana(player, Color.BLACK, 1)
        settle(driver)
        driver.submitYesNo(player, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getGraveyard(player) shouldContain boon
        driver.getExile(player).shouldNotContain(boon)
        driver.getLifeTotal(player) shouldBe lifeBefore
    }

    test("a declined madness card carries no lingering fixed madness cost into the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        discardViaSpell(driver, player, boon)
        settle(driver)
        driver.submitYesNo(player, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Otherwise any later cast from the graveyard (flashback-shaped grants) would silently be
        // charged {B} instead of {4}{B}.
        val container = driver.state.getEntity(boon)
        container?.get<PlayWithFixedAlternativeManaCostComponent>().shouldBeNull()
        container?.get<MadnessExiledComponent>().shouldBeNull()
    }

    test("a madness card cast this way leaves no fixed madness cost behind either") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        discardViaSpell(driver, player, boon)
        driver.giveMana(player, Color.BLACK, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val container = driver.state.getEntity(boon)
        container?.get<PlayWithFixedAlternativeManaCostComponent>().shouldBeNull()
        container?.get<MadnessExiledComponent>().shouldBeNull()
    }

    test("saying yes with no mana to pay still puts the card into the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val boon = driver.putCardInHand(player, "Madness Boon")
        val lifeBefore = driver.getLifeTotal(player)
        discardViaSpell(driver, player, boon)

        // No mana anywhere: the cast can't be paid for, so per CR 702.35a the card is put into the
        // graveyard rather than being stranded in exile.
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.pendingDecision != null) driver.autoResolveDecision()
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getExile(player).shouldNotContain(boon)
        driver.getGraveyard(player) shouldContain boon
        driver.getLifeTotal(player) shouldBe lifeBefore
    }

    test("a madness creature cast this way enters the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCardInHand(player, "Madness Beast")
        discardViaSpell(driver, player, beast)
        driver.giveMana(player, Color.RED, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Madness Beast").shouldNotBeNull()
    }

    test("cycling a madness card exiles it and offers the madness cast (CR 702.29a discard)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spark = driver.putCardInHand(player, "Madness Spark")
        driver.giveColorlessMana(player, 1)
        val result = driver.submit(CycleCard(player, spark, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.isSuccess shouldBe true }

        // Cycling is a discard, so the madness replacement applies to it.
        driver.getExile(player) shouldContain spark
        driver.getGraveyard(player).shouldNotContain(spark)

        val lifeBefore = driver.getLifeTotal(player)
        driver.giveMana(player, Color.RED, 1)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getLifeTotal(player) shouldBe lifeBefore + 3
    }

    // Doubles as the CR 702.35b timing proof: the victim is the *non-active* player and the card is
    // a SORCERY, yet they cast it during the caster's main phase — legal only because the cast
    // happens while the madness trigger is resolving, so timing restrictions don't apply.
    test("an opponent's discard effect still offers the cast to the card's OWNER (CR 702.35a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val victim = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the victim's hand down to the single madness card so the discard is forced.
        driver.getHand(victim).forEach { driver.moveToGraveyard(it) }
        val boon = driver.putCardInHand(victim, "Madness Boon")
        val victimLifeBefore = driver.getLifeTotal(victim)

        val spell = driver.putCardInHand(caster, "Opponent Discard")
        driver.giveColorlessMana(caster, 1)
        driver.submitSuccess(CastSpell(caster, spell, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        while (driver.state.pendingDecision?.playerId == victim &&
            driver.state.pendingDecision !is com.wingedsheep.engine.core.YesNoDecision
        ) {
            driver.submitCardSelection(victim, listOf(boon))
        }

        driver.getExile(victim) shouldContain boon

        driver.giveMana(victim, Color.BLACK, 1)
        settle(driver)
        // The *victim* — the card's owner — is offered the cast, not the caster who caused it.
        driver.state.pendingDecision?.playerId shouldBe victim
        driver.submitYesNo(victim, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getLifeTotal(victim) shouldBe victimLifeBefore + 7
    }

    test("the cleanup-step hand-size discard also triggers madness (CR 514.1)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Exactly eight cards in hand, one of them the madness card: the cleanup-step turn-based
        // action forces a discard with no spell or ability causing it.
        driver.getHand(player).forEach { driver.moveToGraveyard(it) }
        repeat(7) { driver.putCardInHand(player, "Swamp") }
        val boon = driver.putCardInHand(player, "Madness Boon")

        driver.passPriorityUntil(Step.CLEANUP)
        var guard = 0
        while (driver.state.pendingDecision != null && guard++ < 5) {
            driver.submitCardSelection(player, listOf(boon))
        }

        driver.getExile(player) shouldContain boon
        driver.getGraveyard(player).shouldNotContain(boon)
    }

    test("a card without madness is discarded to the graveyard as normal") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val plain = driver.putCardInHand(player, "Self Discard")
        discardViaSpell(driver, player, plain)

        driver.getGraveyard(player) shouldContain plain
        driver.getExile(player).shouldNotContain(plain)
        (driver.state.pendingDecision != null).shouldBeFalse()
    }
})
