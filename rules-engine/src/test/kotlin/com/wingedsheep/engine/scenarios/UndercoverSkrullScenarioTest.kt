package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.msh.cards.UndercoverSkrull
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Undercover Skrull (MSH) — "As long as there are two or more creature cards in your graveyard, this
 * creature gets +2/+2 and is all creature types."
 *
 * The all-types half projects every creature type, but the type line deliberately collapses back to
 * the printed subtypes rather than rendering ~150 of them, and a *granted* all-types carries no
 * CHANGELING keyword to badge — so playtesting saw nothing at all when the condition turned on.
 */
class UndercoverSkrullScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(UndercoverSkrull)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.badges(id: com.wingedsheep.sdk.model.EntityId) =
        ClientStateTransformer(cardRegistry).transform(state, player1)
            .cards.getValue(id).activeEffects.map { it.effectId }

    test("the all-creature-types grant is badged once its condition is met") {
        val d = driver()
        val skrull = d.putCreatureOnBattlefield(d.player1, "Undercover Skrull")

        withClue("no creature cards in the graveyard yet") {
            d.badges(skrull).contains("all_creature_types") shouldBe false
        }

        d.putCardInGraveyard(d.player1, "Centaur Courser")
        d.putCardInGraveyard(d.player1, "Savannah Lions")

        withClue("badges=${d.badges(skrull)}") {
            d.badges(skrull).contains("all_creature_types") shouldBe true
        }
        d.state.projectedState.getPower(skrull) shouldBe 3
    }
})
