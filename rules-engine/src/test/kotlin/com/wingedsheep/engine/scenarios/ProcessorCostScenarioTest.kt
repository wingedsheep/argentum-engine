package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine tests for the BFZ *processor* cost — "put a card an opponent owns from exile into that
 * player's graveyard" — as an activation cost (`Costs.PutFromExileIntoGraveyard`) and as a spell's
 * additional cost (`Costs.additional.PutFromExileIntoGraveyard`): a `CostAtom.ExileFrom` over every
 * player's exile with `destination = GRAVEYARD`.
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | an opponent-owned exiled card pays; it goes to *that player's* graveyard | "an opponent's exiled card pays the activation cost …" |
 * | "an opponent owns" — the payer's own exiled cards never pay (CR 118.3: can't pay → can't activate) | "your own exiled cards can't pay it" |
 * | the payer chooses which card (several candidates → a choice) | "with more candidates than the cost needs, the activation pauses …" |
 * | ruling — several cards may come from different opponents; each goes to its owner's graveyard | "two cards from two opponents each go to their owner's graveyard" |
 * | ruling — a graveyard replacement leaves the card in exile, and the cost is still paid | "a graveyard-to-exile replacement keeps the card in exile, and the cost is still paid" |
 * | CR 601.2h — a spell's additional cost must be paid to cast it | "a spell with the cost can't be cast without an opponent's exiled card" |
 * | the spell form pays the same way | "casting the spell puts the chosen card into its owner's graveyard" |
 * | a client-supplied payment naming the caster's own card is rejected | "a cast naming the caster's own exiled card is rejected" |
 */
class ProcessorCostScenarioTest : FunSpec({

    val processor = card("Test Processor") {
        manaCost = "{2}"
        typeLine = "Creature — Eldrazi Processor"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}"), Costs.PutFromExileIntoGraveyard())
            effect = Effects.DrawCards(1)
        }
    }

    val doubleProcessor = card("Test Double Processor") {
        manaCost = "{2}"
        typeLine = "Creature — Eldrazi Processor"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.PutFromExileIntoGraveyard(count = 2)
            effect = Effects.GainLife(2)
        }
    }

    val processorSpell = card("Test Processor Sorcery") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.PutFromExileIntoGraveyard())
        spell {
            effect = Effects.GainLife(3)
        }
    }

    val voidLeyline = card("Test Void Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Any.ownedByOpponent(),
                    to = Zone.GRAVEYARD,
                ),
            )
        )
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(processor)
        registerCard(doubleProcessor)
        registerCard(processorSpell)
        registerCard(voidLeyline)
    }

    fun GameTestDriver.startTwoPlayer(): Pair<EntityId, EntityId> {
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val alice = activePlayer!!
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        return alice to getOpponent(alice)
    }

    fun GameTestDriver.activate(playerId: EntityId, sourceName: String, exiled: List<EntityId>? = null) =
        submit(
            ActivateAbility(
                playerId = playerId,
                sourceId = findPermanent(playerId, sourceName)!!,
                abilityId = (if (sourceName == "Test Processor") processor else doubleProcessor)
                    .activatedAbilities.first().id,
                costPayment = exiled?.let { AdditionalCostPayment(exiledCards = it) },
            )
        )

    fun GameTestDriver.castProcessorSpell(playerId: EntityId, spellId: EntityId, exiled: List<EntityId>) =
        submit(
            CastSpell(
                playerId = playerId,
                cardId = spellId,
                additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    fun GameTestDriver.exile(playerId: EntityId) = state.getZone(com.wingedsheep.engine.state.ZoneKey(playerId, Zone.EXILE))

    test("an opponent's exiled card pays the activation cost and goes to that player's graveyard") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        d.putPermanentOnBattlefield(alice, "Test Processor")
        val exiled = d.putCardInExile(bob, "Grizzly Bears")
        d.giveColorlessMana(alice, 1)
        val handBefore = d.getHandSize(alice)

        d.activate(alice, "Test Processor", listOf(exiled)).error shouldBe null
        withClue("the cost moved the card while activating, before anything resolved") {
            d.getGraveyard(bob) shouldContain exiled
            d.exile(bob) shouldNotContain exiled
        }
        withClue("it went to its owner's graveyard, not the payer's") {
            d.getGraveyard(alice) shouldNotContain exiled
        }
        d.bothPass()
        d.getHandSize(alice) shouldBe handBefore + 1
    }

    test("your own exiled cards can't pay it") {
        val d = driver()
        val (alice, _) = d.startTwoPlayer()
        val source = d.putPermanentOnBattlefield(alice, "Test Processor")
        val mine = d.putCardInExile(alice, "Grizzly Bears")
        d.giveColorlessMana(alice, 1)

        withClue("no payable activation is offered") {
            d.legalActions(alice).none { (it.action as? ActivateAbility)?.sourceId == source && it.affordable } shouldBe true
        }
        d.activate(alice, "Test Processor", listOf(mine)).error shouldNotBe null
        d.exile(alice) shouldContain mine
    }

    test("the offered cost lists only opponent-owned exiled cards, in the printed wording") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        val source = d.putPermanentOnBattlefield(alice, "Test Processor")
        d.putCardInExile(alice, "Grizzly Bears")
        val theirs = listOf(d.putCardInExile(bob, "Grizzly Bears"), d.putCardInExile(bob, "Island"))
        d.giveColorlessMana(alice, 1)

        val info = d.legalActions(alice)
            .first { (it.action as? ActivateAbility)?.sourceId == source }
            .additionalCostInfo!!
        info.validExileTargets shouldContainExactlyInAnyOrder theirs
        info.description shouldBe "Put a card an opponent owns from exile into that player's graveyard"
    }

    test("with more candidates than the cost needs, the activation pauses for the payer's choice") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        d.putPermanentOnBattlefield(alice, "Test Processor")
        val bears = d.putCardInExile(bob, "Grizzly Bears")
        val island = d.putCardInExile(bob, "Island")
        d.giveColorlessMana(alice, 1)

        d.activate(alice, "Test Processor").error shouldBe null
        val decision = d.state.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainExactlyInAnyOrder listOf(bears, island)

        d.submitCardSelection(alice, listOf(island)).error shouldBe null
        d.getGraveyard(bob) shouldContain island
        d.exile(bob) shouldContain bears
    }

    test("two cards from two opponents each go to their owner's graveyard") {
        val d = driver()
        val (alice, bob, carol) = d.initMultiplayer(List(3) { Deck.of("Island" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(alice, "Test Double Processor")
        val bobs = d.putCardInExile(bob, "Grizzly Bears")
        val carols = d.putCardInExile(carol, "Island")

        d.activate(alice, "Test Double Processor", listOf(bobs, carols)).error shouldBe null
        d.getGraveyard(bob) shouldContain bobs
        d.getGraveyard(carol) shouldContain carols
        d.getGraveyard(alice) shouldNotContain bobs
    }

    test("a graveyard-to-exile replacement keeps the card in exile, and the cost is still paid") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        d.putPermanentOnBattlefield(alice, "Test Processor")
        d.putPermanentOnBattlefield(alice, "Test Void Leyline")
        d.putCardInExile(bob, "Grizzly Bears")
        d.giveColorlessMana(alice, 1)
        val handBefore = d.getHandSize(alice)

        d.activate(alice, "Test Processor", d.exile(bob).toList()).error shouldBe null
        withClue("the replacement sent it back to exile instead of the graveyard") {
            d.getGraveyardCardNames(bob) shouldNotContain "Grizzly Bears"
            d.getExileCardNames(bob) shouldContain "Grizzly Bears"
        }
        d.bothPass()
        d.getHandSize(alice) shouldBe handBefore + 1
    }

    test("a spell with the cost can't be cast without an opponent's exiled card") {
        val d = driver()
        val (alice, _) = d.startTwoPlayer()
        val spell = d.putCardInHand(alice, "Test Processor Sorcery")
        d.putCardInExile(alice, "Grizzly Bears")
        d.giveColorlessMana(alice, 1)

        d.legalActions(alice).none { (it.action as? CastSpell)?.cardId == spell } shouldBe true
    }

    test("casting the spell puts the chosen card into its owner's graveyard") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        val spell = d.putCardInHand(alice, "Test Processor Sorcery")
        val exiled = d.putCardInExile(bob, "Grizzly Bears")
        d.giveColorlessMana(alice, 1)

        val info = d.legalActions(alice).first { (it.action as? CastSpell)?.cardId == spell }
        withClue("the cast offers the opponent's exiled card: ${info.additionalCostInfo}") {
            info.additionalCostInfo!!.validExileTargets shouldContainExactlyInAnyOrder listOf(exiled)
        }

        d.castProcessorSpell(alice, spell, listOf(exiled)).error shouldBe null
        d.getGraveyard(bob) shouldContain exiled
        d.bothPass()
        d.getLifeTotal(alice) shouldBe 23
    }

    test("a cast naming the caster's own exiled card is rejected") {
        val d = driver()
        val (alice, bob) = d.startTwoPlayer()
        val spell = d.putCardInHand(alice, "Test Processor Sorcery")
        val mine = d.putCardInExile(alice, "Grizzly Bears")
        d.putCardInExile(bob, "Island")
        d.giveColorlessMana(alice, 1)

        d.castProcessorSpell(alice, spell, listOf(mine)).error shouldNotBe null
        d.exile(alice) shouldContain mine
    }
})
