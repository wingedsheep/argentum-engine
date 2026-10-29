package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.UginTheSpiritDragon
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ugin, the Spirit Dragon — "−X: Exile each permanent with mana value X or less that's one or
 * more colors."
 *
 * Proves the sweep reads the loyalty ability's chosen X through a battlefield group filter, and
 * that the colored narrowing leaves colorless permanents (and anything above X) alone, on both
 * sides of the table.
 */
class UginTheSpiritDragonScenarioTest : FunSpec({
    val greenTwo = card("Ugin Test Green Two-Drop") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val redFour = card("Ugin Test Red Four-Drop") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
    }
    val colorlessOne = card("Ugin Test Colorless Artifact") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val whiteThree = card("Ugin Test White Enchantment") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(UginTheSpiritDragon, greenTwo, redFour, colorlessOne, whiteThree))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    val minusX = UginTheSpiritDragon.script.activatedAbilities[1]

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("minus X exiles every colored permanent with mana value X or less, and nothing else") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val ugin = d.putPermanentOnBattlefield(me, "Ugin, the Spirit Dragon").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 7)))
        }
        val myBear = d.putPermanentOnBattlefield(me, "Ugin Test Green Two-Drop")
        val theirEnchantment = d.putPermanentOnBattlefield(opp, "Ugin Test White Enchantment")
        val theirGiant = d.putPermanentOnBattlefield(opp, "Ugin Test Red Four-Drop")
        val theirArtifact = d.putPermanentOnBattlefield(opp, "Ugin Test Colorless Artifact")

        d.submitSuccess(ActivateAbility(me, ugin, minusX.id, xValue = 3))
        loyalty(d, ugin) shouldBe 4
        d.settle()

        withClue("colored permanents with mana value 3 or less are exiled from both sides") {
            d.state.getZone(ZoneKey(me, Zone.EXILE)).contains(myBear) shouldBe true
            d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(theirEnchantment) shouldBe true
        }
        withClue("a colored permanent above X stays") {
            d.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)).contains(theirGiant) shouldBe true
        }
        withClue("a colorless permanent within X stays, and so does colorless Ugin") {
            d.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)).contains(theirArtifact) shouldBe true
            d.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(ugin) shouldBe true
        }
    }
})
