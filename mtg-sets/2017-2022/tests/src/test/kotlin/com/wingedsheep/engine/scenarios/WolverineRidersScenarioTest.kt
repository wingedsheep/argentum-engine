package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khc.cards.WolverineRiders
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wolverine Riders (KHC #14) — "At the beginning of each upkeep, create a 1/1 green Elf Warrior
 * creature token. Whenever another Elf you control enters, you gain life equal to its toughness."
 */
class WolverineRidersScenarioTest : FunSpec({

    fun elfTokens(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.state.getBattlefield().filter {
            val e = driver.state.getEntity(it) ?: return@filter false
            e.has<TokenComponent>() &&
                e.get<ControllerComponent>()?.playerId == player &&
                e.get<CardComponent>()?.typeLine?.subtypes?.map { s -> s.value } == listOf("Elf", "Warrior")
        }

    test("on the opponent's upkeep it makes an Elf Warrior, and that Elf entering gains 1 life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WolverineRiders)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Wolverine Riders")
        withClue("putting the Riders itself down gains nothing — it says *another* Elf") {
            driver.getLifeTotal(me) shouldBe 20
        }

        // Advance into the opponent's upkeep: "each upkeep" fires on theirs too.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe driver.getOpponent(me)

        driver.bothPass() // resolve the upkeep token trigger
        val tokens = elfTokens(driver, me)
        tokens.size shouldBe 1
        val token = tokens.single()
        driver.state.getEntity(token)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.GREEN)
        driver.state.projectedState.getPower(token) shouldBe 1
        driver.state.projectedState.getToughness(token) shouldBe 1

        driver.bothPass() // resolve the lifegain trigger from the Elf entering
        driver.getLifeTotal(me) shouldBe 21
    }

    test("the life gained equals the entering Elf's toughness") {
        val elfBrute = CardDefinition.creature(
            name = "Test Elf Brute",
            manaCost = ManaCost.parse("{G}"),
            subtypes = setOf(Subtype("Elf")),
            power = 2,
            toughness = 3,
        )
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WolverineRiders)
        driver.registerCard(elfBrute)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Wolverine Riders")
        val brute = driver.putCardInHand(me, "Test Elf Brute")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, brute).error shouldBe null
        driver.bothPass() // resolve the Elf
        driver.bothPass() // resolve the lifegain trigger
        driver.getLifeTotal(me) shouldBe 23
    }
})
