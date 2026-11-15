package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.Rageform
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rageform (FRF) — {2}{R}{R} Enchantment. "When this enchantment enters, it becomes an Aura with enchant
 * creature. Manifest the top card of your library and attach this enchantment to it.
 * Enchanted creature has double strike."
 */
class RageformScenarioTest : FunSpec({

    test("enters, becomes an Aura and enchants the manifested 2/2, granting it double strike") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + Rageform)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val top = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        val form = d.putCardInHand(you, "Rageform")
        d.giveMana(you, Color.RED, 4)
        d.castSpell(you, form).error shouldBe null
        repeat(6) { if (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass() }

        d.state.getEntity(top)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        val projected = d.state.projectedState
        projected.hasSubtype(form, "Aura") shouldBe true
        d.state.getEntity(form)?.get<AttachedToComponent>()?.targetId shouldBe top
        projected.hasKeyword(top, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
