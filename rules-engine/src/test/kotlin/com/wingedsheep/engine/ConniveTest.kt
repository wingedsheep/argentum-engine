package com.wingedsheep.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * BDD: Connive — draw a card, discard a card; if the discarded card is a nonland,
 * put a +1/+1 counter on the conniving creature.
 */
class ConniveTest : FunSpec({

    val conniveAbilityId = AbilityId(UUID.randomUUID().toString())

    val ConniveCreature = CardDefinition(
        name = "Connive Creature",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"))),
        oracleText = "{T}: Connive. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = conniveAbilityId,
                cost = AbilityCost.Tap,
                effect = Effects.Connive(target = EffectTarget.Self)
            )
        )
    )

    fun setupConnive(handCardName: String): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConniveCreature))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Forest" to 30),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val handCard = driver.putCardInHand(player, handCardName)
        val creature = driver.putCreatureOnBattlefield(player, "Connive Creature")
        driver.removeSummoningSickness(creature)

        val activateResult = driver.submit(
            ActivateAbility(playerId = player, sourceId = creature, abilityId = conniveAbilityId)
        )
        activateResult.isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Triple(driver, creature, handCard)
    }

    test("connive draws one card, discard of nonland gives targeted creature a +1/+1 counter") {
        val (driver, creature, nonlandToDiscard) = setupConnive("Grizzly Bears")
        val player = driver.activePlayer!!
        val countersBefore = driver.state.getEntity(creature)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            player,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(nonlandToDiscard))
        )
        driver.isPaused shouldBe false

        driver.state.getGraveyard(player).contains(nonlandToDiscard) shouldBe true

        val countersAfter = driver.state.getEntity(creature)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        countersAfter shouldBe countersBefore + 1

        // Events fire in order: draw → discard → +1/+1 counter.
        val events = driver.events
        val drawIdx = events.indexOfFirst { it is CardsDrawnEvent }
        val discardIdx = events.indexOfFirst { it is CardsDiscardedEvent }
        val counterIdx = events.indexOfFirst { it is CountersAddedEvent }
        drawIdx shouldNotBe -1
        discardIdx shouldNotBe -1
        counterIdx shouldNotBe -1
        (drawIdx < discardIdx) shouldBe true
        (discardIdx < counterIdx) shouldBe true

        val counterEvent = events.filterIsInstance<CountersAddedEvent>().first()
        counterEvent.entityId shouldBe creature
        counterEvent.counterType shouldBe Counters.PLUS_ONE_PLUS_ONE
        counterEvent.amount shouldBe 1

        // A ZoneChangeEvent for the discarded card must fire so madness, dredge, and
        // "whenever a card is put into a graveyard from your hand" observers can react.
        val zoneChange = events.filterIsInstance<ZoneChangeEvent>()
            .firstOrNull { it.entityId == nonlandToDiscard }
        zoneChange shouldNotBe null
        zoneChange!!.fromZone shouldBe Zone.HAND
        zoneChange.toZone shouldBe Zone.GRAVEYARD
    }

    test("connive does not place a +1/+1 counter when the discarded card is a land") {
        val (driver, creature, landToDiscard) = setupConnive("Forest")
        val player = driver.activePlayer!!

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            player,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(landToDiscard))
        )
        driver.isPaused shouldBe false

        driver.getGraveyard(player) shouldContain landToDiscard

        val events = driver.events
        val drawIdx = events.indexOfFirst { it is CardsDrawnEvent }
        val discardIdx = events.indexOfFirst { it is CardsDiscardedEvent }
        drawIdx shouldNotBe -1
        discardIdx shouldNotBe -1
        (drawIdx < discardIdx) shouldBe true

        events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList()

        val counters = driver.state.getEntity(creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
