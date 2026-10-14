package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `TurnTracker.CARDS_PUT_INTO_GRAVEYARD` — "the number of cards put into [a player's] graveyard from
 * anywhere this turn" (Fraying Sanity). It is the size of the owner-keyed
 * `CardsPutIntoGraveyardThisTurnComponent` arrival list, so every origin zone counts, tokens don't
 * (a token isn't a card, CR 111.6), a card that arrives twice counts twice, and a card that has since
 * left the graveyard still counts.
 */
class CardsPutIntoGraveyardThisTurnTrackerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Grizzly Bears" to 10,
                "Lightning Bolt" to 10
            ),
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun GameTestDriver.count(player: Player = Player.You): Int =
        evaluator.amounts.evaluate(
            state,
            DynamicAmounts.cardsPutIntoGraveyardThisTurn(player),
            EffectContext(sourceId = null, controllerId = activePlayer!!),
        )

    fun GameTestDriver.move(entityId: EntityId, destination: Zone) {
        replaceState(zones.moveToZone(state = state, entityId = entityId, destinationZone = destination).state)
    }

    test("starts at zero") {
        createDriver().count() shouldBe 0
    }

    test("a resolved instant counts — stack to graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.giveMana(player, Color.RED, 1)
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        driver.bothPass()

        driver.count() shouldBe 1
        driver.count(Player.EachOpponent) shouldBe 0
    }

    test("a countered spell counts for its owner, and the counterspell for its own") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.giveMana(player, Color.GREEN, 2)
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, bears).error shouldBe null
        driver.passPriority(player)

        driver.giveMana(opponent, Color.BLUE, 2)
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.castSpellWithTargets(opponent, counterspell, listOf(ChosenTarget.Spell(bears))).error shouldBe null
        driver.bothPass()

        driver.state.getGraveyard(player).contains(bears) shouldBe true
        driver.count() shouldBe 1
        driver.count(Player.EachOpponent) shouldBe 1
    }

    test("a spell copy the stack puts into a graveyard isn't a card and doesn't count") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val copy = EntityId.generate()
        val copyCard = driver.state.getEntity(bolt)!!.get<CardComponent>()!!
        driver.replaceState(
            driver.state.withEntity(
                copy,
                ComponentContainer.of(copyCard, OwnerComponent(player), CopyOfComponent("Lightning Bolt", "Lightning Bolt"))
            )
        )

        driver.replaceState(ZoneTransitionService.recordSpellGraveyardArrival(driver.state, copy, player))
        driver.count() shouldBe 0

        driver.replaceState(ZoneTransitionService.recordSpellGraveyardArrival(driver.state, bolt, player))
        driver.count() shouldBe 1
    }

    test("hand, library and battlefield arrivals all count, keyed on the owner") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.move(driver.putCardInHand(player, "Lightning Bolt"), Zone.GRAVEYARD)
        driver.move(driver.putCardOnTopOfLibrary(player, "Mountain"), Zone.GRAVEYARD)
        driver.move(driver.putCreatureOnBattlefield(player, "Grizzly Bears"), Zone.GRAVEYARD)
        driver.move(driver.putCardInHand(opponent, "Grizzly Bears"), Zone.GRAVEYARD)

        driver.count() shouldBe 3
        driver.count(Player.EachOpponent) shouldBe 1
    }

    test("a card that leaves the graveyard still counts, and counts again when it returns") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        driver.move(bears, Zone.GRAVEYARD)
        driver.move(bears, Zone.EXILE)
        driver.count() shouldBe 1

        driver.move(bears, Zone.HAND)
        driver.move(bears, Zone.GRAVEYARD)
        driver.count() shouldBe 2
    }

    test("a token put into the graveyard does not count") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tokenId = EntityId.generate()
        driver.replaceState(
            driver.state
                .withEntity(
                    tokenId,
                    ComponentContainer.of(
                        CardComponent(
                            cardDefinitionId = "token:Saproling",
                            name = "Saproling Token",
                            manaCost = ManaCost.ZERO,
                            typeLine = TypeLine.parse("Creature - Saproling"),
                            baseStats = CreatureStats(1, 1),
                            colors = setOf(Color.GREEN),
                            ownerId = player
                        ),
                        OwnerComponent(player),
                        TokenComponent,
                        ControllerComponent(player),
                        SummoningSicknessComponent
                    )
                )
                .addToZone(ZoneKey(player, Zone.BATTLEFIELD), tokenId)
        )

        driver.move(tokenId, Zone.GRAVEYARD)

        driver.count() shouldBe 0
    }

    test("the count resets for the next turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.move(driver.putCardInHand(player, "Lightning Bolt"), Zone.GRAVEYARD)
        driver.count() shouldBe 1

        driver.passPriorityUntil(Step.UPKEEP, driver.getOpponent(player))

        driver.count(Player.EachOpponent) shouldBe 0
    }
})
