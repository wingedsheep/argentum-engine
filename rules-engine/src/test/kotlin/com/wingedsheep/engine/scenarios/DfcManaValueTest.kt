package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mana value across a transform, where the Comprehensive Rules split the two kinds of double-faced
 * card apart — the one place the otherwise-shared face-swap machinery cannot be shared.
 *
 * **Nonmodal (CR 712.8e):** *"While a nonmodal double-faced permanent has its back face up, it has
 * only the characteristics of its back face. **However, its mana value is calculated using the mana
 * cost of its front face.**"* The back face of a transform card prints no mana cost at all, so
 * taking the face at face value would make every transformed Werewolf mana value 0 — wrong for
 * "creatures with mana value 3 or less", for convoke-style tallies, for emerge, and for the
 * `EntityNumericProperty.ManaValue` dynamic amount, all of which read the same
 * `CardComponent.manaValue`.
 *
 * **Modal (CR 712.8f):** *"While a modal double-faced spell is on the stack or a modal double-faced
 * permanent is on the battlefield, it has only the characteristics of the face that's up."* No
 * mana-value exception, so a modal back keeps its own printed cost. That direction is pinned by
 * [ModalDfcPermanentBackTest]; this file pins the nonmodal rule and the boundary between them.
 *
 * The disturb side of the same rule — CR 712.8c, a nonmodal transformed *spell* on the stack — is in
 * [DisturbKeywordTest]; the resolved permanent is checked here.
 */
class DfcManaValueTest : ScenarioTestBase() {

    init {
        fun TestGame.manaValueOf(cardName: String): Int =
            state.getEntity(findPermanent(cardName)!!)!!.get<CardComponent>()!!.manaValue

        fun TestGame.faceOf(cardName: String): DoubleFacedComponent.Face? =
            state.getEntity(findPermanent(cardName)!!)!!.get<DoubleFacedComponent>()?.currentFace

        /** Test DFC Front ({2}{G}, mana value 3) on the battlefield, plus a way to flip it. */
        fun board(transformSpells: Int = 1) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Test DFC Front")
            .withCardsInHand(1, "Transform Target Creature", transformSpells)
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a nonmodal DFC keeps its FRONT face's mana value while transformed (CR 712.8e)") {
            val game = board()
            withClue("the front face costs {2}{G}") { game.manaValueOf("Test DFC Front") shouldBe 3 }

            val front = game.findPermanent("Test DFC Front")!!
            game.castSpell(1, "Transform Target Creature", targetId = front).error shouldBe null
            game.resolveStack()

            withClue("it flipped") {
                game.isOnBattlefield("Test DFC Back") shouldBe true
                game.faceOf("Test DFC Back") shouldBe DoubleFacedComponent.Face.BACK
            }
            withClue(
                "CR 712.8e: the back face prints no mana cost, but the permanent's mana value is " +
                    "still calculated from the front's {2}{G} — 3, not 0"
            ) {
                game.manaValueOf("Test DFC Back") shouldBe 3
            }
        }

        test("transforming back to the front restores the front's own mana value") {
            // The override must be cleared on the way back, not left stamped on the component.
            val game = board(transformSpells = 2)

            val front = game.findPermanent("Test DFC Front")!!
            game.castSpell(1, "Transform Target Creature", targetId = front).error shouldBe null
            game.resolveStack()
            game.manaValueOf("Test DFC Back") shouldBe 3

            val back = game.findPermanent("Test DFC Back")!!
            game.castSpell(1, "Transform Target Creature", targetId = back).error shouldBe null
            game.resolveStack()

            withClue("front face up again") {
                game.faceOf("Test DFC Front") shouldBe DoubleFacedComponent.Face.FRONT
            }
            withClue("and its mana value is its own {2}{G}, by the same 3 either way") {
                game.manaValueOf("Test DFC Front") shouldBe 3
            }
        }

        test("a MODAL DFC takes its own back-face cost instead — the boundary (CR 712.8f)") {
            // The same flip on a modal DFC must *not* inherit the front's mana value: Jennifer
            // Walters is 2 and The Sensational She-Hulk is 6. Sharing one face-swap helper between
            // the two layouts is exactly what makes this worth pinning next to the nonmodal case.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jennifer Walters", summoningSickness = false)
                .withCardInHand(1, "Transform Target Creature")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.manaValueOf("Jennifer Walters") shouldBe 2

            val jennifer = game.findPermanent("Jennifer Walters")!!
            game.castSpell(1, "Transform Target Creature", targetId = jennifer).error shouldBe null
            game.resolveStack()

            withClue("CR 712.8f has no front-face exception, so the back keeps its own {3}{G}{W}{W}") {
                game.manaValueOf("The Sensational She-Hulk") shouldBe 6
            }
        }
    }
}
