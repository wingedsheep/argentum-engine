package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.view.CastProvenance
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the Disturb [cost] keyword (CR 702.146, Innistrad: Midnight Hunt / Crimson Vow).
 *
 * "Disturb [cost]" — *"You may cast this card transformed from your graveyard by paying [cost]
 * rather than its mana cost."* (CR 702.146a) The spell goes on the stack **back face up**, so per
 * CR 712.8c it has only the back face's characteristics — while its mana value is still calculated
 * from the front face's mana cost.
 *
 * Exercised with inline cards so the engine behavior is pinned independent of the MID/VOW sets;
 * the four printed cards have their own scenario tests.
 */
class DisturbKeywordTest : FunSpec({

    // A creature-backed disturb card: {W} 1/1 front, a 2/2 flying Spirit back that exiles itself
    // instead of ever reaching a graveyard.
    val geistFront = card("Test Geist") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        disturb("{1}{W}")
    }
    val geistBack = card("Test Geist Spirit") {
        manaCost = ""
        colorIndicator = "W"
        typeLine = "Creature — Spirit"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING)
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
                selfOnly = true,
            )
        )
    }
    val testGeist: CardDefinition = CardDefinition.doubleFacedCreature(geistFront, geistBack)

    // An Aura-backed disturb card — the back face targets what it enchants as the spell is cast.
    val auraFront = card("Test Lantern") {
        manaCost = "{U}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        disturb("{2}{U}")
    }
    val auraBack = card("Test Lantern Aura") {
        manaCost = ""
        colorIndicator = "U"
        typeLine = "Enchantment — Aura"
        auraTarget = Targets.Creature
        staticAbility { ability = GrantKeyword(Keyword.FLYING) }
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
                selfOnly = true,
            )
        )
    }
    val testLantern: CardDefinition = CardDefinition.doubleFacedPermanent(auraFront, auraBack)

    // A disturb card whose back face has flash — proves the timing check reads the BACK face.
    val flashFront = card("Test Flash Geist") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        disturb("{W}")
    }
    val flashBack = card("Test Flash Geist Spirit") {
        manaCost = ""
        colorIndicator = "W"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        keywords(Keyword.FLASH)
    }
    val testFlashGeist: CardDefinition = CardDefinition.doubleFacedCreature(flashFront, flashBack)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testGeist, testLantern, testFlashGeist))
        return driver
    }

    fun disturbActions(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.alternativeCostType == AlternativeCostType.DISTURB }

    test("disturb is offered only from the graveyard, never from hand (CR 702.146a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val inHand = driver.putCardInHand(player, "Test Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        disturbActions(driver, player).map { it.cardId } shouldNotContain inHand

        val inYard = driver.putCardInGraveyard(player, "Test Geist")
        disturbActions(driver, player).map { it.cardId } shouldContain inYard
    }

    test("a disturbed permanent enters as its BACK face with the back face's characteristics (CR 712.8c)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val geist = driver.putCardInGraveyard(player, "Test Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = geist,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error} pending=${result.pendingDecision}") {
            result.isSuccess shouldBe true
        }

        // On the stack it is already the back face — its name, types and P/T all come from there.
        driver.getStackSpellNames() shouldContain "Test Geist Spirit"

        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(player, "Test Geist Spirit")
        perm.shouldNotBeNull()
        val card = driver.state.getEntity(perm)?.get<CardComponent>()
        card?.baseStats shouldBe geistBack.creatureStats
        card?.typeLine?.subtypes?.map { it.value }?.shouldContain("Spirit")
        driver.state.projectedState.hasKeyword(perm, Keyword.FLYING).shouldBeTrue()
        driver.state.getEntity(perm)?.get<DoubleFacedComponent>()?.isBack shouldBe true
        // ...but not its mana value. CR 712.8e: "While a nonmodal double-faced permanent has its
        // back face up, it has only the characteristics of its back face. However, its mana value is
        // calculated using the mana cost of its front face." Test Geist's front is {W}, so the
        // permanent is mana value 1 — not the 0 its blank-costed back face would otherwise give,
        // which is what "creatures with mana value 1 or less" and every other reader of
        // CardComponent.manaValue sees.
        io.kotest.assertions.withClue("CR 712.8e — front face's mana cost, not the back's blank one") {
            card?.manaValue shouldBe 1
        }
    }

    test("the disturbed spell's mana value comes from the FRONT face's mana cost (CR 712.8c)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        // Test Geist's front face costs {W} (mana value 1); the disturb cost paid is {1}{W} and the
        // back face is printed with no mana cost at all. The recorded cast history is the visible
        // carrier of the spell's mana value, and it must read 1 — not 2 (the cost paid) and not 0
        // (the back face's blank cost).
        val geist = driver.putCardInGraveyard(player, "Test Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            CastSpell(
                playerId = player, cardId = geist,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        val record = driver.state.spellsCastThisTurnByPlayer[player]?.last()
        record.shouldNotBeNull()
        record.manaValue shouldBe 1
        // ...and the rest of the record is the back face's: it was a Spirit spell, not a Human one.
        record.name shouldBe "Test Geist Spirit"
        record.typeLine.subtypes.map { it.value }.shouldContain("Spirit")
    }

    test("timing follows the BACK face: a flash back face is disturb-castable at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val plain = driver.putCardInGraveyard(player, "Test Geist")
        val flash = driver.putCardInGraveyard(player, "Test Flash Geist")
        driver.giveMana(player, Color.WHITE, 3)

        driver.passPriorityUntil(Step.END)
        val ids = disturbActions(driver, player).map { it.cardId }
        ids shouldContain flash
        ids shouldNotContain plain
    }

    test("an Aura back face is cast as an Aura spell and attaches to the creature it targeted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val lantern = driver.putCardInGraveyard(player, "Test Lantern")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLUE, 3)

        // The enumerator surfaces the Aura's enchant-creature requirement from the back face.
        val offer = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .firstOrNull { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.DISTURB }
        offer.shouldNotBeNull()
        offer.requiresTargets shouldBe true
        offer.validTargets.shouldNotBeNull().shouldContain(bear)

        val result = driver.submit(
            CastSpell(
                playerId = player, cardId = lantern,
                targets = listOf(ChosenTarget.Permanent(bear)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error} pending=${result.pendingDecision}") {
            result.isSuccess shouldBe true
        }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val aura = driver.findPermanent(player, "Test Lantern Aura")
        aura.shouldNotBeNull()
        driver.state.getEntity(aura)
            ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            ?.targetId shouldBe bear
        driver.state.projectedState.hasKeyword(bear, Keyword.FLYING).shouldBeTrue()
    }

    test("a countered disturb spell is EXILED by its back face's own replacement, not returned to the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val geist = driver.putCardInGraveyard(player, "Test Geist")
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.submit(
            CastSpell(
                playerId = player, cardId = geist,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.passPriority(player)
        val countered = driver.submit(
            CastSpell(
                playerId = opponent, cardId = counter,
                targets = listOf(ChosenTarget.Spell(geist)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${countered.error}") { countered.isSuccess shouldBe true }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // The back face's "would be put into a graveyard from anywhere, exile it instead" clause
        // functions on the stack (CR 614.12), so the countered spell is exiled — it cannot be
        // disturbed a second time.
        driver.state.getExile(player).shouldContain(geist)
        driver.getGraveyard(player) shouldNotContain geist
    }

    test("a disturb cast tells the opponent it came from the graveyard, not from hand") {
        // Regression: the cast carried no origin-zone or alternative-cost information to the client,
        // so a disturbed spell — wearing its back face's unfamiliar name, with no printed mana cost
        // of its own, its graveyard card gone — read as though it had been cast from the caster's
        // hand. The log line and the stack badge both have to say otherwise.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val geist = driver.putCardInGraveyard(player, "Test Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player, cardId = geist,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }

        val castEvent = result.events.filterIsInstance<SpellCastEvent>().single()
        castEvent.castFromZone shouldBe Zone.GRAVEYARD
        castEvent.alternativeCost shouldBe AlternativeCostType.DISTURB
        // The spell is on the stack back face up, so that is the name it was cast under (CR 712.8c).
        // It used to be announced under the front face's name, contradicting the stack itself.
        castEvent.cardName shouldBe "Test Geist Spirit"

        // What the opponent actually reads in the game log. The mana is part of it because an
        // alternative cost replaces the printed one: the card on the stack shows `{2}{W}` while
        // disturb charged `{1}{W}`, so the amount is only knowable if the log says it.
        val logged = ClientEventTransformer.transform(result.events, opponent)
            .filterIsInstance<ClientEvent.SpellCast>()
            .single()
        logged.description shouldBe "Opponent cast Test Geist Spirit (disturb, from graveyard, paid 2 mana)"

        // ...and the badge on the spell while it sits on the stack, from the same recorded facts.
        val onStack = driver.state.getEntity(geist)?.get<SpellOnStackComponent>()
        onStack.shouldNotBeNull()
        CastProvenance.badgeLabel(onStack.alternativeCost, onStack.castFromZone) shouldBe
            "Disturb · Graveyard"
    }

    test("a plain cast from hand stays free of provenance noise") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val geist = driver.putCardInHand(player, "Test Geist")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 1)

        val result = driver.submit(
            CastSpell(playerId = player, cardId = geist, paymentStrategy = PaymentStrategy.FromPool)
        )
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }

        result.events.filterIsInstance<SpellCastEvent>().single().alternativeCost shouldBe null
        ClientEventTransformer.transform(result.events, opponent)
            .filterIsInstance<ClientEvent.SpellCast>()
            .single().description shouldBe "Opponent cast Test Geist"
    }

    test("a disturbed creature that dies is exiled, and reverts to its front face on the way out (Rule 712.8a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val geist = driver.putCardInGraveyard(player, "Test Geist")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                playerId = player, cardId = geist,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Kill it for real, so the battlefield → graveyard move runs through the replacement pipeline.
        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(geist)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.getExile(player).shouldContain(geist)
        driver.getGraveyard(player) shouldNotContain geist
        // Outside the battlefield and stack a DFC has only its front face's characteristics.
        driver.state.getEntity(geist)?.get<CardComponent>()?.name shouldBe "Test Geist"
        driver.state.getEntity(geist)?.get<DoubleFacedComponent>()?.isBack shouldBe false
    }
})
