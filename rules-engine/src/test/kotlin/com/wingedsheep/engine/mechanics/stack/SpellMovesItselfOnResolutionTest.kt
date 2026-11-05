package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A spell whose own instruction moves it ("put this into its owner's library third from the top")
 * leaves the stack as part of its resolution, so the usual "put it into its owner's graveyard" as
 * it finishes resolving has nothing left to move. The move finds the spell on the stack, which is
 * not one of the game's card zones.
 */
class SpellMovesItselfOnResolutionTest : FunSpec({

    val Tucker = card("Self Tucker") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Put this card into its owner's library third from the top, then you gain 1 life."
        spell {
            effect = Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.STACK, positionFromTop = 2) then
                Effects.GainLife(1)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + Tucker)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0, startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the spell goes to the library position it names, not to the graveyard") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Self Tucker")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass()

        d.state.stack shouldNotContain spell
        d.state.getLibrary(me).indexOf(spell) shouldBe 2
        d.state.getGraveyard(me) shouldNotContain spell
        d.getLifeTotal(me) shouldBe 21
    }
})
