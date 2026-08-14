package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.conditions.WebSlungCostWasPaid
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the Web-slinging [cost] keyword (CR 702.188, Marvel's Spider-Man).
 *
 * "Web-slinging [cost]" — *"You may cast this spell by paying [cost] and returning a tapped creature
 * you control to its owner's hand rather than paying its mana cost."* (CR 702.188a) It is an
 * alternative cost with a bundled return-a-tapped-creature payment, cast at the spell's normal
 * timing (no timing permission of its own). The mana value is unchanged (CR 118.9c); a rider can
 * read that the web-slinging cost was paid and the returned creature's mana value.
 *
 * Exercised with inline cards so the engine behavior is pinned independent of the SPM set.
 */
class WebSlingingTest : FunSpec({

    // A vanilla web-slinger ({2}{W}, Web-slinging {W}) — mirrors Spider-Man, Web-Slinger.
    val webVanilla = card("Web Vanilla") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Spider"
        power = 3
        toughness = 3
        webSlinging("{W}")
    }

    // A web-slinger whose ETB fires only if it was web-slung — mirrors Spiders-Man, Heroic Horde.
    // Intervening-'if' (CR 603.4): the trigger only goes on the stack on a web-slung cast, so a
    // normal cast puts no empty trigger on the stack at all.
    val webPayoff = card("Web Payoff") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Spider"
        power = 2
        toughness = 3
        webSlinging("{4}{G}{G}")
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = Conditions.WebSlungCostWasPaid
            effect = Effects.GainLife(3)
        }
    }

    // A web-slinger that enters with +1/+1 counters equal to the returned creature's mana value —
    // mirrors Scarlet Spider, Ben Reilly.
    val webCounters = card("Web Counters") {
        manaCost = "{1}{R}{G}"
        typeLine = "Creature — Spider"
        power = 4
        toughness = 3
        webSlinging("{R}{G}")
        replacementEffect(
            EntersWithDynamicCounters(count = DynamicAmount.CastChoice(ChoiceSlot.WEB_SLUNG_RETURNED_MV))
        )
    }

    // The creature returned to pay the web-slinging cost — mana value 3 ({2}{G}).
    val returnable = card("Mana Three Beast") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    // Spider-UK's end-step clause: intervening-'if' on CreaturesEnteredThisTurn(2), the
    // CREATURES_ENTERED_UNDER_CONTROL turn tracker. (No web-slinging — this exercises the
    // collateral condition primitive.)
    val endStepPayoff = card("End Step Payoff") {
        manaCost = "{3}{W}"
        typeLine = "Creature — Spider"
        power = 3
        toughness = 4
        triggeredAbility {
            trigger = Triggers.YourEndStep
            triggerCondition = Conditions.CreaturesEnteredThisTurn(atLeast = 2)
            effect = Effects.DrawCards(1) then Effects.GainLife(2)
        }
    }

    // A cheap vanilla creature cast to register creature-entered-this-turn entries.
    val littleSpider = card("Little Spider") {
        manaCost = "{G}"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 1
    }

    // A creature whose ETB puts a "gain 5 life" triggered ability on the stack — the object the
    // Spider-Sense-style counter targets.
    val lifeGainer = card("Ability Gainer") {
        manaCost = "{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(5)
        }
    }

    // Spider-Sense's spell half: "counter target instant spell, sorcery spell, or triggered
    // ability" via Targets.InstantSorceryOrTriggeredAbility (activated abilities excluded).
    val senseMirror = card("Sense Mirror") {
        manaCost = "{1}{U}"
        typeLine = "Instant"
        spell {
            target("target instant/sorcery/triggered ability", Targets.InstantSorceryOrTriggeredAbility)
            effect = Effects.CounterSpellOrAbility()
        }
    }

    // A pinger with a fixed-id activated ability so a test can put an *activated* ability on the
    // stack and confirm the narrow counter filter refuses it.
    val pingerAbilityId = AbilityId("websling_test_pinger_gain")
    val abilityPinger = CardDefinition.creature(
        name = "Ability Pinger",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript.permanent(
            ActivatedAbility(
                id = pingerAbilityId,
                cost = AbilityCost.Tap,
                effect = Effects.GainLife(1)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                webVanilla, webPayoff, webCounters, returnable,
                endStepPayoff, littleSpider, lifeGainer, senseMirror, abilityPinger
            )
        )
        return driver
    }

    test("cast for web-slinging: pay the web-slinging mana + return a tapped creature; spell resolves and flag is set") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCreatureOnBattlefield(player, "Mana Three Beast")
        driver.tapPermanent(beast)
        val spider = driver.putCardInHand(player, "Web Vanilla")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 1)
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.WEB_SLINGING,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(beast)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error} pending=${result.pendingDecision}") {
            result.isSuccess shouldBe true
        }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // The tapped Beast was returned to its owner's hand as part of the cost.
        driver.getHand(player) shouldContain beast
        driver.findPermanent(player, "Mana Three Beast") shouldBe null

        // The web-slinger resolved and carries the durable web-slung flag; the condition agrees.
        val perm = driver.findPermanent(player, "Web Vanilla")
        perm.shouldNotBeNull()
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.WEB_SLUNG) shouldBe true
        ConditionEvaluator().evaluate(
            driver.state,
            WebSlungCostWasPaid,
            EffectContext(sourceId = perm, controllerId = player)
        ).shouldBeTrue()
    }

    test("web-slinging is offered as a legal action only while a tapped creature is available") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCreatureOnBattlefield(player, "Mana Three Beast")
        driver.putCardInHand(player, "Web Vanilla")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 1)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        fun webActions() = enumerator.enumerate(driver.state, player)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.alternativeCostType == AlternativeCostType.WEB_SLINGING }

        // Beast untapped: no tapped creature to return, so no web-slinging option.
        webActions().isEmpty().shouldBeTrue()

        // Tap the Beast: the web-slinging option now appears.
        driver.tapPermanent(beast)
        webActions().isNotEmpty().shouldBeTrue()
    }

    test("cannot web-sling by returning an untapped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCreatureOnBattlefield(player, "Mana Three Beast") // left untapped
        val spider = driver.putCardInHand(player, "Web Vanilla")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 1)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = spider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.WEB_SLINGING,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(beast)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("web-slung enters-with-counters rider reads the returned creature's mana value (CR 118.9c)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCreatureOnBattlefield(player, "Mana Three Beast") // mana value 3
        driver.tapPermanent(beast)
        val spider = driver.putCardInHand(player, "Web Counters")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.WEB_SLINGING,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(beast)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Web Counters")
        perm.shouldNotBeNull()
        // Beast's mana value is 3, so the web-slinger enters with 3 +1/+1 counters.
        driver.state.getEntity(perm)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
    }

    test("web-slung ETB payoff fires; a normal cast leaves the flag false and skips the payoff") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        // Normal cast of the payoff for its full {1}{G}: no web-slinging, so no life gain and no flag.
        val normal = driver.putCardInHand(player, "Web Payoff")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, normal).isSuccess shouldBe true
        // Resolve the creature spell only. Because the ETB is an intervening-'if' gated on the
        // web-slung flag (false here), the trigger is never put on the stack — no empty trigger.
        driver.bothPass()
        driver.assertStackSize(0, "a non-web-slung cast must not put an empty ETB trigger on the stack")
        driver.assertLifeTotal(player, 20)
        val normalPerm = driver.findPermanent(player, "Web Payoff")
        normalPerm.shouldNotBeNull()
        ConditionEvaluator().evaluate(
            driver.state,
            WebSlungCostWasPaid,
            EffectContext(sourceId = normalPerm, controllerId = player)
        ).shouldBeFalse()
    }

    test("web-slung ETB payoff gains 3 life when cast using web-slinging") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val beast = driver.putCreatureOnBattlefield(player, "Mana Three Beast")
        driver.tapPermanent(beast)
        val spider = driver.putCardInHand(player, "Web Payoff")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Web-slinging cost is {4}{G}{G}.
        driver.giveMana(player, Color.GREEN, 6)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.WEB_SLINGING,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(beast)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.assertLifeTotal(player, 23)
    }

    // ---- Spider-UK's CreaturesEnteredThisTurn / CREATURES_ENTERED_UNDER_CONTROL tracker ----

    test("CreaturesEnteredThisTurn: end-step payoff fires after two creatures enter under your control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        // The payoff permanent is placed directly (its own entry is not logged), so only creatures
        // that genuinely enter this turn count toward the tracker.
        driver.putCreatureOnBattlefield(player, "End Step Payoff")
        val c1 = driver.putCardInHand(player, "Little Spider")
        val c2 = driver.putCardInHand(player, "Little Spider")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast two 1/1 creatures — two creature entries under the player's control this turn
        // (counted per entry event, CR 400.7).
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, c1).isSuccess shouldBe true
        driver.bothPass()
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, c2).isSuccess shouldBe true
        driver.bothPass()

        val handBeforeEnd = driver.getHandSize(player)
        // Advance to the player's end step; the intervening-'if' condition (>= 2) holds, so the
        // trigger goes on the stack.
        driver.passPriorityUntil(Step.END)
        driver.getTopOfStack().shouldNotBeNull()
        driver.bothPass() // resolve draw a card + gain 2 life

        driver.assertLifeTotal(player, 22)
        driver.getHandSize(player) shouldBe handBeforeEnd + 1
    }

    test("CreaturesEnteredThisTurn: end-step payoff does not fire after only one creature enters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "End Step Payoff")
        val c1 = driver.putCardInHand(player, "Little Spider")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, c1).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        // One creature is below the two-creature threshold, so the intervening-'if' fails and no
        // trigger is put on the stack.
        driver.assertStackSize(0, "one creature is below the two-creature threshold")
        driver.assertLifeTotal(player, 20)
    }

    // ---- Spider-Sense's Targets.InstantSorceryOrTriggeredAbility counter ----

    test("InstantSorceryOrTriggeredAbility: the counter counters a triggered ability on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val gainer = driver.putCardInHand(player, "Ability Gainer")
        val counter = driver.putCardInHand(player, "Sense Mirror")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast the gainer; on resolution its ETB "gain 5 life" triggered ability goes on the stack.
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, gainer).isSuccess shouldBe true
        driver.bothPass()
        val trig = driver.getTopOfStack()
        trig.shouldNotBeNull()
        driver.state.getEntity(trig)?.get<TriggeredAbilityOnStackComponent>().shouldNotBeNull()

        // Counter the triggered ability with the Spider-Sense-style spell.
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = counter,
                targets = listOf(ChosenTarget.Spell(trig)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // The trigger was countered before it resolved — no 5 life.
        driver.assertLifeTotal(player, 20)
    }

    test("InstantSorceryOrTriggeredAbility: the counter cannot target an activated ability") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val pinger = driver.putCreatureOnBattlefield(player, "Ability Pinger")
        driver.removeSummoningSickness(pinger)
        // Activating a non-mana ability keeps priority with the activator, so it stays on the stack.
        driver.submit(ActivateAbility(playerId = player, sourceId = pinger, abilityId = pingerAbilityId))
            .isSuccess shouldBe true
        val abilityOnStack = driver.getTopOfStack()
        abilityOnStack.shouldNotBeNull()
        driver.state.getEntity(abilityOnStack)?.get<ActivatedAbilityOnStackComponent>().shouldNotBeNull()

        // The narrow filter admits triggered abilities but not activated ones, so this target is illegal.
        val counter = driver.putCardInHand(player, "Sense Mirror")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = counter,
                targets = listOf(ChosenTarget.Spell(abilityOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }
})
