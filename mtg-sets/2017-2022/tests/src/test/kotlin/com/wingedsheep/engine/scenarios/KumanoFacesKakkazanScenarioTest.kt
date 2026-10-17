package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kumano Faces Kakkazan // Etching of Kumano (NEO #152).
 *
 *   I — This Saga deals 1 damage to each opponent and each planeswalker they control.
 *   II — When you next cast a creature spell this turn, that creature enters with an additional
 *        +1/+1 counter on it.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *   Back: Haste. If a creature dealt damage this turn by a source you controlled would die, exile it
 *   instead.
 *
 * The replacement's own rules live in `WasDealtDamageThisTurnByTest`; this walks the
 * card through all three chapters and pins the back face.
 */
class KumanoFacesKakkazanScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toMyNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        fun TestGame.plusOneCounters(name: String) = findPermanents(name).map {
            state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        }

        context("Kumano Faces Kakkazan") {

            test("chapter I deals 1 damage to each opponent and each planeswalker they control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kumano Faces Kakkazan")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kumano Faces Kakkazan").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 20
                val jace = game.findPermanent("Jace Beleren")!!
                game.state.getEntity(jace)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 2
            }

            test("chapter II buffs only the next creature spell; chapter III flips it into Etching of Kumano") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kumano Faces Kakkazan")
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kumano Faces Kakkazan").error shouldBe null
                game.resolveStack()

                game.toMyNextMain()
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("chapter II's counter goes on the next creature spell only") {
                    game.plusOneCounters("Grizzly Bears").sorted() shouldBe listOf(0, 0, 1)
                }

                game.toMyNextMain()
                withClue("chapter III returned it transformed") {
                    game.findPermanent("Kumano Faces Kakkazan") shouldBe null
                    game.isOnBattlefield("Etching of Kumano") shouldBe true
                }

                val theirBears = game.findAllPermanents("Grizzly Bears")
                    .first { game.state.projectedState.getController(it) == game.player2Id }
                game.castSpell(1, "Shock", theirBears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
        }
    }
}
