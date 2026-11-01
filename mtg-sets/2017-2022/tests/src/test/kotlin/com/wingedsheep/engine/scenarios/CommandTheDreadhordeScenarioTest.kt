package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.CommandTheDreadhorde
import com.wingedsheep.mtg.sets.definitions.war.cards.UginTheIneffable
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Command the Dreadhorde — {4}{B}{B} Sorcery (War of the Spark #82)
 *
 * "Choose any number of target creature and/or planeswalker cards in graveyards. Command the
 *  Dreadhorde deals damage to you equal to the total mana value of those cards. Put them onto the
 *  battlefield under your control."
 *
 * Covers reanimating from both graveyards (a creature and a planeswalker the opponent owns come
 * under the caster's control, the planeswalker with its starting loyalty), the summed-mana-value
 * self-damage, and the zero-target cast.
 */
class CommandTheDreadhordeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CommandTheDreadhorde + UginTheIneffable)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castCommand(targets: List<ChosenTarget>): EntityId {
        val spell = putCardInHand(player1, "Command the Dreadhorde")
        giveMana(player1, Color.BLACK, 2)
        giveColorlessMana(player1, 4)
        castSpellWithTargets(player1, spell, targets).error shouldBe null
        bothPass()
        return spell
    }

    fun GameTestDriver.onBattlefield(id: EntityId): Boolean =
        listOf(player1, player2).any { id in state.getZone(ZoneKey(it, Zone.BATTLEFIELD)) }

    test("returns creature and planeswalker cards from both graveyards under your control and deals their total mana value to you") {
        val d = driver()
        val courser = d.putCardInGraveyard(d.player1, "Centaur Courser")   // MV 3
        val lions = d.putCardInGraveyard(d.player2, "Savannah Lions")      // MV 1
        val ugin = d.putCardInGraveyard(d.player2, "Ugin, the Ineffable")  // MV 6
        val lifeBefore = d.getLifeTotal(d.player1)

        val spell = d.castCommand(
            listOf(
                ChosenTarget.Card(courser, d.player1, Zone.GRAVEYARD),
                ChosenTarget.Card(lions, d.player2, Zone.GRAVEYARD),
                ChosenTarget.Card(ugin, d.player2, Zone.GRAVEYARD),
            )
        )

        for (id in listOf(courser, lions, ugin)) {
            withClue("$id is on the battlefield") { d.onBattlefield(id) shouldBe true }
            withClue("$id is controlled by player1") { d.getController(id) shouldBe d.player1 }
        }
        withClue("Ugin enters with its starting loyalty") {
            d.state.getEntity(ugin)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 4
        }
        withClue("damage equals total mana value 3 + 1 + 6") {
            d.getLifeTotal(d.player1) shouldBe lifeBefore - 10
        }
        withClue("the opponent takes no damage") { d.getLifeTotal(d.player2) shouldBe 20 }
        withClue("the sorcery goes to the graveyard") { d.getGraveyard(d.player1) shouldContain spell }
    }

    test("with zero targets the spell resolves, deals no damage and goes to the graveyard") {
        val d = driver()
        val lifeBefore = d.getLifeTotal(d.player1)

        val spell = d.castCommand(emptyList())

        d.getLifeTotal(d.player1) shouldBe lifeBefore
        d.getGraveyard(d.player1) shouldContain spell
    }
})
