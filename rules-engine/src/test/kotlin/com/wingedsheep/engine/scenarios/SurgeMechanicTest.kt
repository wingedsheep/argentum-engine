package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Surge (CR 702.117a): "You may pay [cost] rather than pay this spell's mana cost as you cast this
 * spell if you or one of your teammates has cast another spell this turn." It follows the ordinary
 * alternative-cost rules (601.2b, 601.2f–h), and its linked "if its surge cost was paid" riders read
 * `Conditions.SurgeCostWasPaid`.
 */
class SurgeMechanicTest : FunSpec({

    // A surge creature whose enters trigger reads the rider: gain 3 life if the surge cost was paid.
    val surgeCreature = card("Surge Test Creature") {
        manaCost = "{4}{R}"
        typeLine = "Creature — Goblin"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.surge("{R}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.SurgeCostWasPaid
            effect = Effects.GainLife(3)
        }
    }

    // An instant with surge whose own resolving rider reads the condition off the spell.
    val surgeInstant = card("Surge Test Instant") {
        manaCost = "{3}{U}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.surge("{U}"))
        spell {
            effect = Effects.GainLife(1) then
                Effects.If(Conditions.SurgeCostWasPaid, then = Effects.GainLife(10))
        }
    }

    // Fall of the Titans' shape: {X} in the surge cost itself.
    val surgeXSorcery = card("Surge X Test Sorcery") {
        manaCost = "{X}{X}{R}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.surge("{X}{R}"))
        spell {
            effect = Effects.GainLife(com.wingedsheep.sdk.dsl.DynamicAmounts.xValue(), EffectTarget.Controller)
        }
    }

    // The "another spell" a player casts first.
    val cantrip = card("Surge Opener") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(0) }
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(surgeCreature, surgeInstant, surgeXSorcery, cantrip))
    }

    fun surgeCast(player: EntityId, cardId: EntityId, x: Int? = null) =
        CastSpell(
            playerId = player,
            cardId = cardId,
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SURGE,
            xValue = x,
            paymentStrategy = PaymentStrategy.FromPool,
        )

    fun GameTestDriver.surgeOffer(player: EntityId, cardId: EntityId) =
        legalActions(player).firstOrNull { it.actionType == "CastWithSurge" && (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.castOpener(player: EntityId) {
        val opener = putCardInHand(player, "Surge Opener")
        giveMana(player, Color.RED, 1)
        submit(CastSpell(player, opener, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
    }

    test("surge is neither offered nor accepted before any other spell was cast this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Surge Test Creature")
        driver.giveMana(player, Color.RED, 1)

        driver.surgeOffer(player, cardId) shouldBe null
        driver.submitExpectFailure(surgeCast(player, cardId))
        driver.findPermanent(player, "Surge Test Creature") shouldBe null
    }

    test("after another spell, the creature is cast for its surge cost and its enters rider fires") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player)
        driver.bothPass()
        val cardId = driver.putCardInHand(player, "Surge Test Creature")
        driver.giveMana(player, Color.RED, 1)

        val offer = driver.surgeOffer(player, cardId)
        offer shouldNotBe null
        offer!!.affordable shouldBe true
        offer.manaCostString shouldBe "{R}"

        driver.submit(surgeCast(player, cardId)).outcome shouldBe Outcome.Done
        driver.bothPass() // creature resolves
        driver.bothPass() // enters trigger resolves
        val permanent = driver.findPermanent(player, "Surge Test Creature")!!
        withClue("the permanent carries the surge flag for its linked riders") {
            driver.state.getEntity(permanent)!!.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.SURGE_CAST) shouldBe true
        }
        driver.getLifeTotal(player) shouldBe 23
    }

    test("the normal cast of a surge card doesn't count as paying its surge cost, even when surge was available") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player)
        driver.bothPass()
        val cardId = driver.putCardInHand(player, "Surge Test Creature")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(CastSpell(player, cardId, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        driver.bothPass()
        val permanent = driver.findPermanent(player, "Surge Test Creature")!!
        (driver.state.getEntity(permanent)!!.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.SURGE_CAST) == true) shouldBe false
        driver.assertStackSize(0, "no rider trigger without the surge cost")
        driver.getLifeTotal(player) shouldBe 20
    }

    test("the other spell still on the stack enables surge, and the spell's own rider reads it as it resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player) // still on the stack
        val instant = driver.putCardInHand(player, "Surge Test Instant")
        driver.giveMana(player, Color.BLUE, 1)

        driver.surgeOffer(player, instant) shouldNotBe null
        driver.submit(surgeCast(player, instant)).outcome shouldBe Outcome.Done
        driver.bothPass() // the surge instant resolves first
        driver.getLifeTotal(player) shouldBe 31
    }

    test("an opponent's spell doesn't enable your surge") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val other = driver.getOpponent(active)
        driver.castOpener(active)
        driver.passPriority(active) // the non-active player now holds priority with the opener on the stack
        val instant = driver.putCardInHand(other, "Surge Test Instant")
        driver.giveMana(other, Color.BLUE, 1)

        driver.surgeOffer(other, instant) shouldBe null
        driver.submitExpectFailure(surgeCast(other, instant))
    }

    test("the cast count resets: a spell cast last turn doesn't enable surge this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player)
        driver.bothPass()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, player)
        val cardId = driver.putCardInHand(player, "Surge Test Creature")
        driver.giveMana(player, Color.RED, 1)

        driver.surgeOffer(player, cardId) shouldBe null
        driver.submitExpectFailure(surgeCast(player, cardId))
    }

    test("a sorcery-speed surge card isn't offered at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player) // stack is non-empty: no sorcery timing
        val cardId = driver.putCardInHand(player, "Surge Test Creature")
        driver.giveMana(player, Color.RED, 1)

        driver.surgeOffer(player, cardId) shouldBe null
    }

    test("{X} in the surge cost is announced and paid") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.castOpener(player)
        driver.bothPass()
        val cardId = driver.putCardInHand(player, "Surge X Test Sorcery")
        driver.giveMana(player, Color.RED, 4)

        val offer = driver.surgeOffer(player, cardId)!!
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 3
        driver.submit(surgeCast(player, cardId, x = 3)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe 23
    }

    test("a teammate's spell enables surge (Two-Headed Giant); an opponent's doesn't") {
        val driver = createDriver()
        val (p1, p2, p3, _) = driver.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe p1
        val cardId = driver.putCardInHand(p1, "Surge Test Creature")
        driver.giveMana(p1, Color.RED, 1)
        val base = driver.state

        driver.replaceState(base.copy(playerSpellsCastThisTurn = mapOf(p3 to 1)))
        withClue("only an opposing team's player cast a spell") { driver.surgeOffer(p1, cardId) shouldBe null }

        driver.replaceState(base.copy(playerSpellsCastThisTurn = mapOf(p2 to 1)))
        withClue("teammate p2 cast a spell this turn") { driver.surgeOffer(p1, cardId) shouldNotBe null }
        driver.submit(surgeCast(p1, cardId)).outcome shouldBe Outcome.Done
    }
})
