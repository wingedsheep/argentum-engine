package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.VerdantCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Verdant Command (MH2 #182): "Counter target loyalty ability of a planeswalker" reaches only
 * activated abilities with a loyalty cost (CR 606.2) — not a planeswalker's other activated
 * abilities — and the remaining modes resolve alongside it.
 */
class VerdantCommandScenarioTest : FunSpec({

    val loyaltyGain = 0
    val tapGain = 1

    // A planeswalker with a loyalty ability and a non-loyalty activated ability.
    val testWalker = card("Test Walker") {
        manaCost = "{2}{W}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        loyaltyAbility(+1) {
            effect = Effects.GainLife(5)
        }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(7)
        }
    }

    val squirrels = 0
    val counterLoyalty = 1
    val exileGraveyardCard = 2
    val gainLife = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VerdantCommand, testWalker))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.walkerOnBattlefield(owner: EntityId): EntityId {
        val walker = putPermanentOnBattlefield(owner, "Test Walker")
        replaceState(
            state.updateEntity(walker) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        return walker
    }

    fun GameTestDriver.castCommand(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ) = submit(
        CastSpell(
            playerId = caster,
            cardId = putCardInHand(caster, "Verdant Command").also { giveMana(caster, Color.GREEN, 2) },
            targets = modeTargets.flatten(),
            chosenModes = modes,
            modeTargetsOrdered = modeTargets,
        )
    )

    fun loyalty(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    test("counters a planeswalker's loyalty ability; the cost stays paid and the other mode resolves") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opp = driver.getOpponent(active)
        val walker = driver.walkerOnBattlefield(active)

        driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = walker,
                abilityId = testWalker.activatedAbilities[loyaltyGain].id
            )
        ).error shouldBe null
        loyalty(driver, walker) shouldBe 4
        val ability = driver.getTopOfStack()!!

        driver.passPriority(active)
        driver.castCommand(
            opp,
            modes = listOf(counterLoyalty, gainLife),
            modeTargets = listOf(listOf(ChosenTarget.Spell(ability)), listOf(ChosenTarget.Player(opp))),
        ).error shouldBe null
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.getLifeTotal(active) shouldBe 20
        driver.getLifeTotal(opp) shouldBe 23
        loyalty(driver, walker) shouldBe 4
    }

    test("a planeswalker's non-loyalty activated ability is not a legal target") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opp = driver.getOpponent(active)
        val walker = driver.walkerOnBattlefield(active)

        driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = walker,
                abilityId = testWalker.activatedAbilities[tapGain].id
            )
        ).error shouldBe null
        val ability = driver.getTopOfStack()!!

        driver.passPriority(active)
        driver.castCommand(
            opp,
            modes = listOf(counterLoyalty, gainLife),
            modeTargets = listOf(listOf(ChosenTarget.Spell(ability)), listOf(ChosenTarget.Player(opp))),
        ).error shouldNotBe null
        // The cast was rejected: only the {T} ability is on the stack, and the Command is still in hand.
        driver.stackSize shouldBe 1
        driver.state.getZone(opp, Zone.HAND).any { driver.getCardName(it) == "Verdant Command" } shouldBe true
    }

    test("target player creates two tapped Squirrels; a card in a graveyard is exiled") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opp = driver.getOpponent(active)
        val buried = driver.putCardInGraveyard(opp, "Grizzly Bears")

        driver.castCommand(
            active,
            modes = listOf(squirrels, exileGraveyardCard),
            modeTargets = listOf(listOf(ChosenTarget.Player(opp)), listOf(ChosenTarget.Card(buried, opp, Zone.GRAVEYARD))),
        ).error shouldBe null
        driver.bothPass()

        val tokens = driver.state.getBattlefield(opp).filter { driver.getCardName(it) == "Squirrel Token" }
        tokens.size shouldBe 2
        tokens.all { driver.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
        driver.state.getZone(opp, Zone.EXILE).contains(buried) shouldBe true
    }
})
