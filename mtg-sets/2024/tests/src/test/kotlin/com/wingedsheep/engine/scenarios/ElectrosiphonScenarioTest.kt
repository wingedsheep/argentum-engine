package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Electrosiphon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Electrosiphon (PIP) — counter target spell; you get {E} equal to its mana value.
 *
 * Pins that the energy is the spell's mana value *on the stack*, where its {X} counts as the X
 * chosen (CR 202.3e), even though the countered card is in the graveyard (X = 0 there) by the time
 * the energy is paid out.
 */
class ElectrosiphonScenarioTest : FunSpec({

    val xBlast = card("Siphon Test X Blast") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(Electrosiphon, xBlast))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.siphon(spell: EntityId) {
        val siphon = putCardInHand(player2, Electrosiphon.name)
        giveMana(player2, Color.BLUE, 2)
        giveMana(player2, Color.RED, 1)
        castSpellWithTargets(player2, siphon, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        bothPass()
    }

    test("countering an X spell gives energy equal to its mana value with X counted") {
        val d = driver()
        val blast = d.putCardInHand(d.player1, xBlast.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 4)
        d.castXSpell(d.player1, blast, 4).error shouldBe null
        d.passPriority(d.player1)

        d.siphon(blast)

        d.energy(d.player2) shouldBe 5
        d.state.getZone(d.player1, Zone.GRAVEYARD) shouldContain blast
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("countering a creature spell gives energy equal to its mana value") {
        val d = driver()
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, bears).error shouldBe null
        d.passPriority(d.player1)

        d.siphon(bears)

        d.energy(d.player2) shouldBe 2
        d.state.getZone(d.player1, Zone.GRAVEYARD) shouldContain bears
        d.energy(d.player1) shouldBe 0
    }
})
