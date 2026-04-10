package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.YgraEaterOfAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YgraEaterOfAllTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Plain Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(YgraEaterOfAll, Bear))
        return driver
    }

    test("opponent's creatures become Food artifacts too") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")
        val opponentBear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        val projected = StateProjector().project(driver.state)
        projected.getSubtypes(opponentBear).contains("Food") shouldBe true
        projected.getTypes(opponentBear).contains("ARTIFACT") shouldBe true
    }

    test("Ygra gains +1/+1 counters when an opponent's creature (now a Food) dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ygra = driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")
        driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        // Cast Doom Blade on the opponent's bear. The bear is a Food (granted by Ygra),
        // so when it dies the Ygra trigger should fire.
        val doomBlade = driver.putCardInHand(active, "Doom Blade")
        driver.giveMana(active, Color.BLACK, 1)
        driver.giveMana(active, Color.BLACK, 1)
        val opponentBear = driver.state.getBattlefield()
            .first { it != ygra && driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Plain Bear" }
        val castResult = driver.castSpell(active, doomBlade, listOf(opponentBear))
        castResult.isSuccess shouldBe true

        // Resolve doom blade and the trigger
        driver.bothPass()
        driver.bothPass()

        val counters = driver.state.getEntity(ygra)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        counters shouldBe 2
    }

    test("opponent can pay Forage cost by sacrificing one of their creatures Ygra turned into Food") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Ygra is on the active player's side; opponent only controls a vanilla Bear.
        // Because Ygra makes other creatures Food artifacts, the opponent's Bear should
        // qualify as a Food the opponent can sacrifice to pay the Forage cost.
        val ygra = driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")
        val opponentBear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        // Sanity-check projection: the opponent's Bear is a Food.
        val projected = driver.state.projectedState
        projected.hasSubtype(opponentBear, Subtype.FOOD.value) shouldBe true

        val costHandler = CostHandler()
        val emptyPool = ManaPool()

        // canPay should now see the opponent's Bear as a sacrifice-able Food, even though
        // the opponent's graveyard is empty (so the exile-3-cards branch is unavailable).
        costHandler.canPayAbilityCost(
            state = driver.state,
            cost = AbilityCost.Forage,
            sourceId = ygra,
            controllerId = opponent,
            manaPool = emptyPool
        ) shouldBe true

        costHandler.canPayAdditionalCost(
            state = driver.state,
            cost = AdditionalCost.Forage,
            controllerId = opponent
        ) shouldBe true

        // Actually paying Forage as the opponent should sacrifice the Bear.
        val payResult = costHandler.payAbilityCost(
            state = driver.state,
            cost = AbilityCost.Forage,
            sourceId = ygra,
            controllerId = opponent,
            manaPool = emptyPool
        )
        payResult.success shouldBe true
        val newState = payResult.newState!!
        newState.getBattlefield().contains(opponentBear) shouldBe false
        newState.getGraveyard(opponent).contains(opponentBear) shouldBe true
    }

    test("Ygra itself is not a Food artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ygra = driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")

        val projected = StateProjector().project(driver.state)
        projected.getSubtypes(ygra).contains("Food") shouldBe false
        projected.getTypes(ygra).contains("ARTIFACT") shouldBe false
    }
})
