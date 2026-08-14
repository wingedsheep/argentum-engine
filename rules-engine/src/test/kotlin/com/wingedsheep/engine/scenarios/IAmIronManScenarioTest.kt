package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.msh.cards.IAmIronMan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * I Am Iron Man (MSH) — "Until end of turn, target artifact or creature becomes an artifact creature
 * with base power and toughness 4/4 and gains flying."
 *
 * Playtesting reported the UI never showing that the creature had become an artifact. The animation
 * itself is Layer 4/6/7b and works; what was missing was any way for the player to see a *granted*
 * card type, since the battlefield draws the printed card image. This pins both halves: the
 * projection, and the `grantedCardTypes` field the preview renders.
 */
class IAmIronManScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(IAmIronMan)
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the target becomes a 4/4 flying artifact creature and the grant is visible") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3 Centaur Warrior

        val spell = d.putCardInHand(d.player1, "I Am Iron Man")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.castSpell(d.player1, spell, listOf(bear)).isSuccess shouldBe true
        repeat(6) { if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        val card = ClientStateTransformer(d.cardRegistry).transform(d.state, d.player1)
            .cards.getValue(bear)

        withClue("cardTypes=${card.cardTypes} typeLine=${card.typeLine}") {
            card.cardTypes.contains("ARTIFACT") shouldBe true
            card.cardTypes.contains("CREATURE") shouldBe true
        }
        card.power shouldBe 4
        card.toughness shouldBe 4

        // The printed image can't show a granted type, so this is the player's only cue.
        card.grantedCardTypes shouldBe setOf("ARTIFACT")
    }
})
