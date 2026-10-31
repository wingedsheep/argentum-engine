package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoyaltyDamageFloor
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [LoyaltyDamageFloor]: damage that would take a planeswalker below the floor removes only enough
 * loyalty counters to leave the floor (CR 120.3c, 306.8; an "instead" replacement, CR 614.1a). The
 * damage itself is unchanged.
 */
class LoyaltyDamageFloorTest : FunSpec({

    fun floorCard(name: String, floor: Int, damageType: DamageType = DamageType.Any) = card(name) {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        replacementEffect(
            LoyaltyDamageFloor(
                floor = floor,
                appliesTo = EventPattern.DamageEvent(
                    recipient = Recipient.Object(GameObjectFilter.Planeswalker.youControl()),
                    damageType = damageType
                )
            )
        )
    }

    val floorOne = floorCard("Test Floor One", 1)
    val floorTwo = floorCard("Test Floor Two", 2)
    val combatOnly = floorCard("Test Combat Floor", 1, DamageType.Combat)

    val walker = card("Test Floor Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(floorOne, floorTwo, combatOnly, walker))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.walkerWith(loyalty: Int): EntityId =
        putPermanentOnBattlefield(player1, "Test Floor Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) })
        }

    fun GameTestDriver.loyalty(pw: EntityId): Int =
        state.getEntity(pw)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Player 1 bolts [target] (3 damage). */
    fun GameTestDriver.bolt(target: ChosenTarget) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpellWithTargets(player1, bolt, listOf(target)).error shouldBe null
        bothPass()
    }

    test("lethal damage leaves the floor; the loyalty event reports only the counters removed") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.player1, "Test Floor One")
        val pw = d.walkerWith(3)

        d.bolt(ChosenTarget.Permanent(pw))

        d.loyalty(pw) shouldBe 1
        d.state.getBattlefield().contains(pw) shouldBe true
        d.events.filterIsInstance<CountersRemovedEvent>().last().amount shouldBe 2
    }

    test("a higher floor keeps more counters, and a planeswalker already at the floor loses none") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.player1, "Test Floor Two")
        val healthy = d.walkerWith(4)
        d.bolt(ChosenTarget.Permanent(healthy))
        withClue("4 loyalty, 3 damage, floor 2: two counters come off") { d.loyalty(healthy) shouldBe 2 }

        val atFloor = d.walkerWith(2)
        d.bolt(ChosenTarget.Permanent(atFloor))
        withClue("already at the floor: nothing comes off") { d.loyalty(atFloor) shouldBe 2 }
    }

    test("several floors: the strictest wins") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.player1, "Test Floor One")
        d.putPermanentOnBattlefield(d.player1, "Test Floor Two")
        val pw = d.walkerWith(3)

        d.bolt(ChosenTarget.Permanent(pw))

        d.loyalty(pw) shouldBe 2
    }

    test("the damage-type filter gates it, and an opponent's planeswalker is not covered") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.player1, "Test Combat Floor")
        val pw = d.walkerWith(3)
        d.bolt(ChosenTarget.Permanent(pw))
        withClue("noncombat damage isn't covered by a combat-only floor") {
            d.state.getBattlefield().contains(pw) shouldBe false
        }

        val d2 = newDriver()
        d2.putPermanentOnBattlefield(d2.player2, "Test Floor One")
        val mine = d2.walkerWith(3)
        d2.bolt(ChosenTarget.Permanent(mine))
        withClue("the floor's controller doesn't control this planeswalker") {
            d2.state.getBattlefield().contains(mine) shouldBe false
        }
    }
})
