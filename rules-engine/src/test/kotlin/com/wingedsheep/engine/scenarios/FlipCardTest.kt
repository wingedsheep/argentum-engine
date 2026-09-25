package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.FlippedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FlippedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Flip cards (CR 710) — `CardDefinition.flipCard` + `Effects.Flip`.
 *
 * The test card is a blue {1}{U} 1/2 whose flip half is a legendary 3/3 lord; each half carries
 * its own free activated abilities, so the test can tell which half's abilities are live.
 */
class FlipCardTest : FunSpec({

    val upright = card("Test Flip Apprentice") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Human Wizard"
        oracleText = "{0}: Flip this creature."
        power = 1
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Flip()
            description = "{0}: Flip this creature."
        }
    }
    val flipHalf = card("Test Flip Master") {
        manaCost = "{1}{U}"
        typeLine = "Legendary Creature — Human Wizard"
        oracleText = "Other creatures you control get +1/+1.\n{0}: Return this creature to its " +
            "owner's hand.\n{0}: Create a token that's a copy of this creature.\n{0}: Flip this creature."
        power = 3
        toughness = 3
        staticAbility {
            ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true))
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.ReturnToHand(EffectTarget.Self)
            description = "{0}: Return this creature to its owner's hand."
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.CreateTokenCopyOfSelf()
            description = "{0}: Create a token that's a copy of this creature."
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Flip()
            description = "{0}: Flip this creature."
        }
    }
    val flipCard = CardDefinition.flipCard(unflipped = upright, flipped = flipHalf)
    val flipAbility = flipCard.activatedAbilities.single().id
    val (bounceAbility, copyAbility, reflipAbility) = flipCard.flipSide!!.activatedAbilities.map { it.id }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + flipCard)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activateAndResolve(source: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId) {
        submit(ActivateAbility(playerId = player1, sourceId = source, abilityId = abilityId)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.card(id: EntityId): CardComponent = state.getEntity(id)!!.get<CardComponent>()!!

    test("the definition is a FLIP layout, not a double-faced card, and the flip half keeps the mana cost") {
        flipCard.layout shouldBe CardLayout.FLIP
        flipCard.isDoubleFaced shouldBe false
        flipCard.flipSide!!.manaCost shouldBe flipCard.manaCost
    }

    test("flipping swaps name, type line and P/T but keeps mana cost and colour (CR 710.1c)") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")

        d.activateAndResolve(id, flipAbility)

        val card = d.card(id)
        card.name shouldBe "Test Flip Master"
        card.typeLine.isLegendary shouldBe true
        card.manaCost.toString() shouldBe "{1}{U}"
        card.colors shouldBe setOf(Color.BLUE)
        val projected = d.state.projectedState
        projected.getPower(id) shouldBe 3
        projected.getToughness(id) shouldBe 3
        projected.getColors(id) shouldBe setOf(Color.BLUE.name)
        d.state.getEntity(id)!!.get<FlippedComponent>().shouldNotBeNull()
        d.events.filterIsInstance<FlippedEvent>().single().newName shouldBe "Test Flip Master"
    }

    test("the flip half's static and activated abilities replace the upright half's") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.state.projectedState.getPower(courser) shouldBe 3

        d.activateAndResolve(id, flipAbility)

        withClue("the flip half's lord static now applies") {
            d.state.projectedState.getPower(courser) shouldBe 4
            d.state.projectedState.getToughness(courser) shouldBe 4
        }
        withClue("the upright half's ability is gone") {
            d.submitExpectFailure(ActivateAbility(playerId = d.player1, sourceId = id, abilityId = flipAbility))
        }
    }

    test("counters, tapped status and identity survive the flip") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        d.replaceState(d.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))).with(TappedComponent)
        })

        d.activateAndResolve(id, flipAbility)

        d.state.getBattlefield() shouldContain id
        d.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
        d.state.projectedState.getPower(id) shouldBe 5
    }

    test("flipping is one-way — a flipped permanent can't flip again (CR 710.4)") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        d.activateAndResolve(id, flipAbility)

        d.activateAndResolve(id, reflipAbility)

        d.card(id).name shouldBe "Test Flip Master"
        d.events.filterIsInstance<FlippedEvent>() shouldHaveSize 1
    }

    test("a flipped permanent that leaves the battlefield forgets its status (CR 710.4)") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        d.activateAndResolve(id, flipAbility)

        d.activateAndResolve(id, bounceAbility)

        d.getHand(d.player1) shouldContain id
        d.card(id).name shouldBe "Test Flip Apprentice"
        d.card(id).typeLine.isLegendary shouldBe false
        d.state.getEntity(id)!!.get<FlippedComponent>().shouldBeNull()
    }

    test("a flip card can't be transformed — transforming it does nothing (CR 701.27c)") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        val spell = d.putCardInHand(d.player1, "Transform Target Creature")
        d.giveMana(d.player1, Color.BLUE, 2)

        d.castSpell(d.player1, spell, listOf(id)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.card(id).name shouldBe "Test Flip Apprentice"
        d.events.filterIsInstance<TransformedEvent>().shouldBeEmpty()
    }

    test("flipping is not transforming — no TransformedEvent is emitted") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")

        d.activateAndResolve(id, flipAbility)

        d.events.filterIsInstance<TransformedEvent>().shouldBeEmpty()
    }

    test("a token copy of a flipped permanent is the upright half — status isn't copied (CR 707.2)") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        d.activateAndResolve(id, flipAbility)

        d.activateAndResolve(id, copyAbility)

        val token = d.state.getBattlefield().single { it != id && d.card(it).cardDefinitionId.startsWith("Test Flip") }
        d.card(token).name shouldBe "Test Flip Apprentice"
        d.state.getEntity(token)!!.get<FlippedComponent>().shouldBeNull()
        withClue("the copy can flip on its own") {
            d.activateAndResolve(token, flipAbility)
            d.card(token).name shouldBe "Test Flip Master"
        }
    }

    test("the client sees a flipped permanent's art rotated 180° and its flip-half name") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, "Test Flip Apprentice")
        fun view() = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(d.state, d.player1).cards.getValue(id)
        view().imageRotation shouldBe 0

        d.activateAndResolve(id, flipAbility)

        view().imageRotation shouldBe 180
        view().name shouldBe "Test Flip Master"
    }
})
