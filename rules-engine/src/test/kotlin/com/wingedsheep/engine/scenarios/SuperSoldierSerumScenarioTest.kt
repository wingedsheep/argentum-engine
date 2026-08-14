package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.msh.cards.SuperSoldierSerum
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Super-Soldier Serum (MSH) — "Enchanted creature gets +2/+2, has first strike and vigilance, and is
 * a legendary Soldier in addition to its other types."
 *
 * Playtesting reported the Soldier type never showing up on the enchanted creature. This asserts the
 * grant all the way out to the DTO the client actually renders, not just the projection, since the
 * type line the player sees is built from `projectedValues` in ClientStateTransformer.
 */
class SuperSoldierSerumScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(SuperSoldierSerum)
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the enchanted creature becomes a legendary Soldier and gets +2/+2") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3 Centaur Warrior

        val aura = d.putCardInHand(d.player1, "Super-Soldier Serum")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, aura, listOf(bear)).isSuccess shouldBe true
        repeat(6) { if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        val card = ClientStateTransformer(d.cardRegistry)
            .transform(d.state, d.player1)
            .cards
            .getValue(bear)

        withClue("subtypes=${card.subtypes} typeLine=${card.typeLine}") {
            card.subtypes.contains("Soldier") shouldBe true
            card.typeLine.contains("Soldier") shouldBe true
            card.typeLine.contains("Legendary") shouldBe true
        }
        card.power shouldBe 5
        card.toughness shouldBe 5

        // The battlefield renders the printed image and the preview only prints the type line for
        // tokens, so the grant is only visible to the player through this field.
        card.grantedSubtypes shouldBe setOf("Soldier")
        card.legendaryByEffect shouldBe true

        // grantedSubtypes exists for *static* grants like this Aura. Subtypes added by a floating
        // effect are excluded, because those already carry their own "+X" type-change badge and
        // would otherwise be reported twice in the preview.
        card.activeEffects.none { it.effectId == "type_added" } shouldBe true
    }
})
