package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.BlitzedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blitz (CR 702.152): the alternative cost, the haste and "when this dies, draw a card" a blitzed
 * permanent has, the end-step sacrifice, and casting from the graveyard "using its blitz ability".
 */
class BlitzMechanicTest : FunSpec({

    // "When you sacrifice this" proves the end-step removal is a sacrifice, not a plain move.
    val blitzCreature = card("Blitz Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Ogre Warrior"
        power = 4
        toughness = 3
        keywordAbility(KeywordAbility.blitz("{1}{R}"))
        triggeredAbility {
            trigger = Triggers.self.isSacrificed()
            effect = Effects.GainLife(3)
        }
    }

    // Tenacious Underdog's shape: an em-dash blitz cost with a life payment, castable from the
    // graveyard using its blitz ability.
    val graveyardBlitzCreature = card("Graveyard Blitz Creature") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Warrior"
        power = 3
        toughness = 2
        keywordAbility(KeywordAbility.blitz("{2}{B}{B}", Costs.additional.PayLife(2)))
        staticAbility {
            ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.BLITZ)
        }
    }

    val murder = card("Blitz Test Murder") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    // Minimal blink: exile the creature, then return it — a new object (CR 400.7).
    val blink = card("Blitz Blink Test Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.Move(creature, Zone.EXILE) then Effects.Move(creature, Zone.BATTLEFIELD)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(blitzCreature, graveyardBlitzCreature, murder, blink))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castForBlitz(player: EntityId, cardId: EntityId) =
        submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BLITZ,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

    fun GameTestDriver.castActionsFor(cardId: EntityId): List<LegalAction> =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, activePlayer!!)
            .filter { (it.action as? CastSpell)?.cardId == cardId }

    fun LegalAction.isBlitzCast(): Boolean = (action as CastSpell).let {
        it.useAlternativeCost && it.alternativeCostType == AlternativeCostType.BLITZ
    }

    test("a blitzed creature has haste and can attack the turn it's cast") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.castForBlitz(player, cardId).outcome shouldBe Outcome.Done
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Blitz Test Creature")!!
        driver.state.getEntity(permanent)?.has<BlitzedComponent>() shouldBe true
        driver.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(permanent), opponent).error shouldBe null
    }

    test("a blitzed creature is sacrificed at the beginning of the next end step and its controller draws a card") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        driver.giveMana(player, Color.RED, 2)
        driver.castForBlitz(player, cardId)
        driver.bothPass()
        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.END)
        driver.bothPass() // sacrifice trigger resolves
        driver.bothPass() // "dies: draw" and "sacrificed: gain 3" resolve
        driver.bothPass()

        driver.findPermanent(player, "Blitz Test Creature") shouldBe null
        driver.getGraveyardCardNames(player).contains("Blitz Test Creature") shouldBe true
        driver.getHandSize(player) shouldBe handBefore + 1
        driver.getLifeTotal(player) shouldBe 23
    }

    test("a blitzed creature that dies before the end step still draws its controller a card") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        driver.giveMana(player, Color.RED, 2)
        driver.castForBlitz(player, cardId)
        driver.bothPass()
        val permanent = driver.findPermanent(player, "Blitz Test Creature")!!

        val murderId = driver.putCardInHand(player, "Blitz Test Murder")
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpell(player, murderId, listOf(permanent)).outcome shouldBe Outcome.Done
        val handBefore = driver.getHandSize(player)
        driver.bothPass() // Murder resolves
        driver.bothPass() // blitz draw resolves

        driver.findPermanent(player, "Blitz Test Creature") shouldBe null
        driver.getHandSize(player) shouldBe handBefore + 1
        // Destroyed, not sacrificed — the sacrifice rider stays quiet.
        driver.getLifeTotal(player) shouldBe 20
    }

    test("a blitzed creature blinked before the end step is a new object and is not sacrificed") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        driver.giveMana(player, Color.RED, 2)
        driver.castForBlitz(player, cardId)
        driver.bothPass()
        val blitzed = driver.findPermanent(player, "Blitz Test Creature")!!

        val blinkId = driver.putCardInHand(player, "Blitz Blink Test Sorcery")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, blinkId, listOf(blitzed)).outcome shouldBe Outcome.Done
        driver.bothPass()
        val returned = driver.findPermanent(player, "Blitz Test Creature")!!
        driver.state.getEntity(returned)?.has<BlitzedComponent>() shouldBe false

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.bothPass()

        driver.findPermanent(player, "Blitz Test Creature") shouldNotBe null
        driver.getLifeTotal(player) shouldBe 20
    }

    test("a creature cast for its normal cost has neither haste nor the draw trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        driver.giveMana(player, Color.RED, 5)
        driver.submit(CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        val permanent = driver.findPermanent(player, "Blitz Test Creature")!!
        driver.state.getEntity(permanent)?.has<BlitzedComponent>() shouldBe false
        driver.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe false

        val murderId = driver.putCardInHand(player, "Blitz Test Murder")
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpell(player, murderId, listOf(permanent))
        val handBefore = driver.getHandSize(player)
        driver.bothPass()

        driver.findPermanent(player, "Blitz Test Creature") shouldBe null
        driver.getHandSize(player) shouldBe handBefore
        driver.stackSize shouldBe 0
    }

    test("a blitz card in hand offers both the normal cast and the blitz cast") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Blitz Test Creature")
        repeat(2) { driver.putLandOnBattlefield(player, "Mountain") }

        val actions = driver.castActionsFor(cardId)
        val blitz = actions.single { it.isBlitzCast() }
        blitz.affordable shouldBe true
        blitz.actionType shouldBe "CastWithBlitz"
        actions.single { !(it.action as CastSpell).useAlternativeCost }.affordable shouldBe false
    }

    test("a card with 'cast from your graveyard using its blitz ability' is offered only the blitz cast there") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInGraveyard(player, "Graveyard Blitz Creature")
        // A blitz card without the permission stays in the graveyard.
        val plainId = driver.putCardInGraveyard(player, "Blitz Test Creature")

        val actions = driver.castActionsFor(cardId)
        actions.size shouldBe 1
        actions.single().isBlitzCast() shouldBe true
        actions.single().sourceZone shouldBe Zone.GRAVEYARD.name
        driver.castActionsFor(plainId) shouldBe emptyList()
    }

    test("blitz from the graveyard pays the blitz mana and the bundled life, and the creature is blitzed") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInGraveyard(player, "Graveyard Blitz Creature")
        driver.giveMana(player, Color.BLACK, 4)

        driver.castForBlitz(player, cardId).outcome shouldBe Outcome.Done
        driver.getLifeTotal(player) shouldBe 18
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Graveyard Blitz Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<BlitzedComponent>() shouldBe true
        driver.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe true
    }

    test("a graveyard card can't be cast for its normal cost through a blitz-only permission") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val cardId = driver.putCardInGraveyard(player, "Graveyard Blitz Creature")
        driver.giveMana(player, Color.BLACK, 2)

        driver.submit(CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldNotBe Outcome.Done
        driver.getGraveyardCardNames(player).contains("Graveyard Blitz Creature") shouldBe true
    }
})
