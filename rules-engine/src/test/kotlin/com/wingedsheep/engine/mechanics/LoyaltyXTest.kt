package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CardDefinition.loyaltyIsX` — a planeswalker printed with loyalty X (Nissa, Steward of Elements).
 * Its intrinsic "enters with loyalty counters equal to its printed loyalty" (CR 306.5b) reads X as
 * the value chosen for the spell it resolved from (CR 107.3m); entering any other way, X is 0, so it
 * enters with no loyalty and state-based actions put it into the graveyard (the card's ruling).
 */
class LoyaltyXTest : FunSpec({

    val xWalker = card("X Walker") {
        manaCost = "{X}{G}"
        typeLine = "Legendary Planeswalker — Test"
        loyaltyIsX = true
    }

    val reanimate = card("Walker Revival") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Return target planeswalker card from your graveyard to the battlefield."
        spell {
            val pw = target(TargetFilter(GameObjectFilter.Planeswalker.ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefield(pw)
        }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(xWalker, reanimate))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            driver.bothPass()
            guard++
        }
    }

    fun walkerOnBattlefield(driver: GameTestDriver): EntityId? = driver.state.getBattlefield().singleOrNull {
        driver.state.getEntity(it)?.get<CardComponent>()?.name == "X Walker"
    }

    fun loyalty(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    test("the definition carries loyalty X over a printed base of 0") {
        xWalker.loyaltyIsX shouldBe true
        xWalker.startingLoyalty shouldBe 0
    }

    test("cast with X = 3, it enters with three loyalty counters") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "X Walker")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castXSpell(me, spell, xValue = 3).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val walker = walkerOnBattlefield(driver)
        withClue("the walker survives with loyalty X") { (walker != null) shouldBe true }
        loyalty(driver, walker!!) shouldBe 3
    }

    test("cast with X = 0, it enters with no loyalty and is put into the graveyard") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "X Walker")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castXSpell(me, spell, xValue = 0).outcome shouldBe Outcome.Done
        resolveAll(driver)

        walkerOnBattlefield(driver) shouldBe null
        driver.getGraveyardCardNames(me).contains("X Walker") shouldBe true
    }

    test("put onto the battlefield without being cast, X is 0 and it dies to state-based actions") {
        val (driver, me) = setup()
        val inGraveyard = driver.putCardInGraveyard(me, "X Walker")
        val spell = driver.putCardInHand(me, "Walker Revival")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(inGraveyard, me, Zone.GRAVEYARD))).outcome shouldBe Outcome.Done
        resolveAll(driver)

        walkerOnBattlefield(driver) shouldBe null
        driver.getGraveyardCardNames(me).contains("X Walker") shouldBe true
    }
})
