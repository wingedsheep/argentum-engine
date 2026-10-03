package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Guardian of the Forgotten (MH3 #28): "Vigilance. Whenever a modified creature you control dies,
 * manifest the top card of your library."
 */
class GuardianOfTheForgottenScenarioTest : ScenarioTestBase() {

    init {
        context("Guardian of the Forgotten") {

            fun board(vararg creatures: String, aura: Pair<Int, String>? = null) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Guardian of the Forgotten")
                .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
                .apply { aura?.let { (player, name) -> withCardAttachedTo(player, name, creatures.first()) } }
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("has vigilance") {
                val game = board()
                game.state.projectedState.hasKeyword(
                    game.findPermanent("Guardian of the Forgotten")!!, Keyword.VIGILANCE
                ) shouldBe true
            }

            test("a creature with a counter dying manifests the top card of your library") {
                val game = board("Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                val topCard = game.state.getLibrary(game.player1Id).first()
                val librarySize = game.librarySize(1)

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe librarySize - 1
                game.state.getBattlefield().contains(topCard) shouldBe true
                game.state.getEntity(topCard)?.get<FaceDownComponent>() shouldBe FaceDownComponent
                game.state.projectedState.getPower(topCard) shouldBe 2
                game.state.projectedState.getToughness(topCard) shouldBe 2
            }

            test("a creature enchanted by your own Aura is modified") {
                val game = board("Grizzly Bears", aura = 1 to "Holy Strength")
                val librarySize = game.librarySize(1)

                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe librarySize - 1
            }

            test("an opponent's Aura doesn't make your creature modified") {
                val game = board("Grizzly Bears", aura = 2 to "Pacifism")
                val librarySize = game.librarySize(1)

                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe librarySize
            }

            test("an unmodified creature dying does nothing") {
                val game = board("Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!
                val librarySize = game.librarySize(1)

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe librarySize
                game.state.getBattlefield().none {
                    game.state.getEntity(it)?.get<FaceDownComponent>() != null
                } shouldBe true
            }
        }
    }
}
