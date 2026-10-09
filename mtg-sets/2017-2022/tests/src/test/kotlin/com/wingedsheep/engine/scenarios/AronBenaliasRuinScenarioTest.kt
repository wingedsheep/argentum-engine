package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.AronBenaliasRuin
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AronBenaliasRuinScenarioTest : FunSpec({
    val creature = card("Aron Test Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(AronBenaliasRuin, DominariaUnitedForest274, creature))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.WHITE, 1)
        giveMana(player1, Color.BLACK, 1)
    }
    fun activate(d: GameTestDriver, source: EntityId, sacrifice: EntityId) = d.submit(
        ActivateAbility(
            d.player1, source, AronBenaliasRuin.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice)),
            paymentStrategy = PaymentStrategy.FromPool,
        )
    )

    for (sourceLeaves in listOf(false, true)) {
        test("sacrifice is paid first and resolution counts current creatures; source leaves = $sourceLeaves") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, AronBenaliasRuin.name)
            d.removeSummoningSickness(source)
            val fodder = d.putCreatureOnBattlefield(d.player1, creature.name)
            val opponent = d.putCreatureOnBattlefield(d.player2, creature.name)
            val land = d.putLandOnBattlefield(d.player1, "Forest")
            activate(d, source, fodder).outcome shouldBe Outcome.Done
            (fodder in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
            d.state.getEntity(source)?.has<TappedComponent>() shouldBe true
            d.stackSize shouldBe 1
            val lateArrival = d.putCreatureOnBattlefield(d.player1, creature.name)
            if (sourceLeaves) d.moveToGraveyard(source)
            d.bothPass()
            d.stackSize shouldBe 0
            d.state.projectedState.getPower(lateArrival) shouldBe 2
            d.state.projectedState.getToughness(lateArrival) shouldBe 2
            d.state.projectedState.getPower(opponent) shouldBe 1
            d.state.projectedState.isCreature(land) shouldBe false
            if (!sourceLeaves) {
                d.state.projectedState.getPower(source) shouldBe 4
                d.state.projectedState.getToughness(source) shouldBe 4
            }
        }
    }

    test("cannot pay with Aron himself or an opposing creature") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, AronBenaliasRuin.name)
        d.removeSummoningSickness(source)
        val opposing = d.putCreatureOnBattlefield(d.player2, creature.name)
        activate(d, source, source).error shouldNotBe null
        activate(d, source, opposing).error shouldNotBe null
        d.stackSize shouldBe 0
        d.state.getEntity(source)?.has<TappedComponent>() shouldBe false
    }
})
