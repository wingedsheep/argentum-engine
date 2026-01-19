package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.GameActionHandler
import com.wingedsheep.rulesengine.ecs.action.GameActionResult
import com.wingedsheep.rulesengine.ecs.action.CastSpell
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the additional cost system.
 */
class AdditionalCostTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val elfDef = CardDefinition.creature(
        name = "Llanowar Elves",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype(value = "Elf")),
        power = 1,
        toughness = 1
    )

    val sorceryDef = CardDefinition.sorcery(
        name = "Test Sorcery",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Do something"
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (creatureId, state1) = createEntity(EntityId.generate(), components)
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addCardToHand(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        val handZone = ZoneId(ZoneType.HAND, ownerId)
        return cardId to state1.addToZone(cardId, handZone)
    }

    val handler = GameActionHandler()

    context("AdditionalCost data classes") {
        test("SacrificePermanent has correct description") {
            val cost = AdditionalCost.SacrificePermanent(CardFilter.CreatureCard)
            cost.description shouldBe "Sacrifice a creature card"
        }

        test("SacrificePermanent with count has correct description") {
            val cost = AdditionalCost.SacrificePermanent(CardFilter.LandCard, count = 3)
            cost.description shouldBe "Sacrifice 3 land cards"
        }

        test("SacrificePermanent with filter has correct description") {
            val cost = AdditionalCost.SacrificePermanent(
                CardFilter.And(listOf(CardFilter.CreatureCard, CardFilter.HasColor(Color.GREEN)))
            )
            cost.description shouldBe "Sacrifice a creature card green card"
        }

        test("DiscardCards has correct description") {
            val cost = AdditionalCost.DiscardCards(1)
            cost.description shouldBe "Discard a card"
        }

        test("DiscardCards with count has correct description") {
            val cost = AdditionalCost.DiscardCards(2)
            cost.description shouldBe "Discard 2 cards"
        }

        test("PayLife has correct description") {
            val cost = AdditionalCost.PayLife(3)
            cost.description shouldBe "Pay 3 life"
        }

        test("ExileCards has correct description") {
            val cost = AdditionalCost.ExileCards(1, CardFilter.CreatureCard, CostZone.GRAVEYARD)
            cost.description shouldBe "Exile a creature card from your graveyard"
        }

        test("TapPermanents has correct description") {
            val cost = AdditionalCost.TapPermanents(2, CardFilter.CreatureCard)
            cost.description shouldBe "Tap 2 untapped creature cards you control"
        }
    }

    context("AdditionalCostPayment") {
        test("NONE is empty") {
            AdditionalCostPayment.NONE.isEmpty shouldBe true
        }

        test("payment with sacrifices is not empty") {
            val payment = AdditionalCostPayment(
                sacrificedPermanents = listOf(EntityId.of("creature1"))
            )
            payment.isEmpty shouldBe false
        }

        test("payment with discards is not empty") {
            val payment = AdditionalCostPayment(
                discardedCards = listOf(EntityId.of("card1"))
            )
            payment.isEmpty shouldBe false
        }

        test("payment with life is not empty") {
            val payment = AdditionalCostPayment(lifePaid = 1)
            payment.isEmpty shouldBe false
        }
    }

    context("CardScript with additional costs") {
        test("can create script with sacrifice cost") {
            val script = cardScript("Natural Order") {
                sacrificeCost(CardFilter.And(listOf(
                    CardFilter.CreatureCard,
                    CardFilter.HasColor(Color.GREEN)
                )))
                spell(SearchLibraryEffect(
                    filter = CardFilter.And(listOf(
                        CardFilter.CreatureCard,
                        CardFilter.HasColor(Color.GREEN)
                    )),
                    destination = SearchDestination.BATTLEFIELD
                ))
            }

            script.additionalCosts shouldHaveSize 1
            script.additionalCosts[0].shouldBeInstanceOf<AdditionalCost.SacrificePermanent>()
        }

        test("can create script with discard cost") {
            val script = cardScript("Force of Will") {
                discardCost(1, CardFilter.HasColor(Color.BLUE))
                payLifeCost(1)
                spell(DrawCardsEffect(1))  // placeholder effect
            }

            script.additionalCosts shouldHaveSize 2
            script.additionalCosts[0].shouldBeInstanceOf<AdditionalCost.DiscardCards>()
            script.additionalCosts[1].shouldBeInstanceOf<AdditionalCost.PayLife>()
        }

        test("can create script with multiple additional costs") {
            val script = cardScript("Complex Spell") {
                additionalCosts(
                    AdditionalCost.SacrificePermanent(CardFilter.CreatureCard),
                    AdditionalCost.PayLife(2),
                    AdditionalCost.DiscardCards(1)
                )
                spell(DrawCardsEffect(3))
            }

            script.additionalCosts shouldHaveSize 3
        }
    }

    context("Casting spell with sacrifice cost") {
        test("sacrifices permanent when casting") {
            var state = newGame()

            // Add a creature to battlefield (to be sacrificed)
            val (creatureId, state1) = state.addCreatureToBattlefield(bearDef, player1Id)
            state = state1

            // Add spell to hand
            val (spellId, state2) = state.addCardToHand(sorceryDef, player1Id)
            state = state2

            // Verify creature is on battlefield
            state.getBattlefield() shouldContain creatureId

            // Cast spell with sacrifice cost payment
            val payment = AdditionalCostPayment(
                sacrificedPermanents = listOf(creatureId)
            )
            val action = CastSpell(
                cardId = spellId,
                casterId = player1Id,
                fromZone = ZoneId(ZoneType.HAND, player1Id),
                additionalCostPayment = payment
            )

            val result = handler.execute(state, action)
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state

            // Creature should no longer be on battlefield (sacrificed)
            newState.getBattlefield() shouldNotContain creatureId

            // Creature should be in graveyard
            newState.getGraveyard(player1Id) shouldContain creatureId

            // Spell should be on stack
            newState.getStack() shouldContain spellId
        }
    }

    context("Casting spell with discard cost") {
        test("discards card when casting") {
            var state = newGame()

            // Add two cards to hand (one to discard, one to cast)
            val (cardToDiscard, state1) = state.addCardToHand(bearDef, player1Id)
            val (spellId, state2) = state1.addCardToHand(sorceryDef, player1Id)
            state = state2

            val handZone = ZoneId(ZoneType.HAND, player1Id)

            // Verify cards are in hand
            state.getZone(handZone) shouldContain cardToDiscard
            state.getZone(handZone) shouldContain spellId

            // Cast spell with discard cost payment
            val payment = AdditionalCostPayment(
                discardedCards = listOf(cardToDiscard)
            )
            val action = CastSpell(
                cardId = spellId,
                casterId = player1Id,
                fromZone = handZone,
                additionalCostPayment = payment
            )

            val result = handler.execute(state, action)
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state

            // Discarded card should be in graveyard
            newState.getGraveyard(player1Id) shouldContain cardToDiscard

            // Spell should be on stack
            newState.getStack() shouldContain spellId
        }
    }

    context("Casting spell with life payment") {
        test("pays life when casting") {
            var state = newGame()

            // Add spell to hand
            val (spellId, state1) = state.addCardToHand(sorceryDef, player1Id)
            state = state1

            // Check initial life
            val initialLife = state.getComponent<LifeComponent>(player1Id)!!.life
            initialLife shouldBe 20

            // Cast spell with life payment
            val payment = AdditionalCostPayment(lifePaid = 3)
            val action = CastSpell(
                cardId = spellId,
                casterId = player1Id,
                fromZone = ZoneId(ZoneType.HAND, player1Id),
                additionalCostPayment = payment
            )

            val result = handler.execute(state, action)
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state

            // Life should be reduced
            newState.getComponent<LifeComponent>(player1Id)!!.life shouldBe 17

            // Spell should be on stack
            newState.getStack() shouldContain spellId
        }
    }

    context("Casting spell with multiple costs") {
        test("pays all costs when casting") {
            var state = newGame()

            // Add creature to battlefield
            val (creatureId, state1) = state.addCreatureToBattlefield(bearDef, player1Id)
            state = state1

            // Add card to hand for discard
            val (cardToDiscard, state2) = state.addCardToHand(elfDef, player1Id)
            state = state2

            // Add spell to hand
            val (spellId, state3) = state.addCardToHand(sorceryDef, player1Id)
            state = state3

            val initialLife = state.getComponent<LifeComponent>(player1Id)!!.life

            // Cast spell with multiple cost payments
            val payment = AdditionalCostPayment(
                sacrificedPermanents = listOf(creatureId),
                discardedCards = listOf(cardToDiscard),
                lifePaid = 2
            )
            val action = CastSpell(
                cardId = spellId,
                casterId = player1Id,
                fromZone = ZoneId(ZoneType.HAND, player1Id),
                additionalCostPayment = payment
            )

            val result = handler.execute(state, action)
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val newState = (result as GameActionResult.Success).state

            // Creature should be in graveyard (sacrificed)
            newState.getBattlefield() shouldNotContain creatureId
            newState.getGraveyard(player1Id) shouldContain creatureId

            // Card should be in graveyard (discarded)
            newState.getGraveyard(player1Id) shouldContain cardToDiscard

            // Life should be reduced
            newState.getComponent<LifeComponent>(player1Id)!!.life shouldBe initialLife - 2

            // Spell should be on stack
            newState.getStack() shouldContain spellId
        }
    }
})
