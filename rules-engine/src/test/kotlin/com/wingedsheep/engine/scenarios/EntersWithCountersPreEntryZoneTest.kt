package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An enters-with-counters replacement modifies the entry event itself, so a dynamic count is measured
 * before the permanent leaves the zone it is coming from. A card entering from exile counts itself
 * among "cards in exile" (Ulamog, the Defiler's ruling); one returned from the graveyard counts itself
 * among the cards in that graveyard (Golgari Grave-Troll's). A card cast from hand is on the stack as
 * it resolves — in neither zone — and so counts only the others.
 */
class EntersWithCountersPreEntryZoneTest : FunSpec({

    val exileCounter = card("Exile Counter") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.zone(Player.Each, Zone.EXILE).count()))
    }
    val graveCounter = card("Grave Counter") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
        replacementEffect(
            EntersWithDynamicCounters(count = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).count())
        )
    }
    val testBlink = card("Test Blink") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.Exile(creature) then Effects.Move(creature, Zone.BATTLEFIELD)
        }
    }
    val testReanimate = card("Test Reanimate") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(exileCounter, graveCounter, testBlink, testReanimate).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveAll() {
        repeat(6) { if (state.stack.isNotEmpty() && pendingDecision == null) bothPass() }
    }

    test("a permanent returned from exile counts itself among the cards in exile") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCardInExile(player, "Centaur Courser")
        val creature = driver.putCreatureOnBattlefield(player, "Exile Counter")
        withClue("put onto the battlefield directly, no replacement ran") { driver.plusCounters(creature) shouldBe 0 }

        val blink = driver.putCardInHand(player, "Test Blink")
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, blink, listOf(creature)).error shouldBe null
        driver.resolveAll()

        driver.state.getBattlefield() shouldContain creature
        withClue("the Courser and the entrant itself were in exile as it entered") { driver.plusCounters(creature) shouldBe 2 }
    }

    test("a creature card returned from the graveyard counts itself among the creature cards there") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCardInGraveyard(player, "Centaur Courser")
        val zombie = driver.putCardInGraveyard(player, "Grave Counter")

        val reanimate = driver.putCardInHand(player, "Test Reanimate")
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpellWithTargets(player, reanimate, listOf(ChosenTarget.Card(zombie, player, Zone.GRAVEYARD))).error shouldBe null
        driver.resolveAll()

        driver.state.getBattlefield() shouldContain zombie
        withClue("the Courser and the entrant itself were in the graveyard as it entered") { driver.plusCounters(zombie) shouldBe 2 }
    }

    test("cast from hand, the entrant is on the stack and counts only the other cards") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCardInGraveyard(player, "Centaur Courser")

        val zombie = driver.putCardInHand(player, "Grave Counter")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, zombie).error shouldBe null
        driver.resolveAll()

        driver.state.getBattlefield() shouldContain zombie
        withClue("only the Courser is in the graveyard") { driver.plusCounters(zombie) shouldBe 1 }
    }
})
