package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.SvellaIceShaper
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Svella, Ice Shaper (KHM #230).
 *
 * "{3}, {T}" makes an Icy Manalith — a noncreature snow artifact token that taps for any color the
 * turn it's made. "{6}{R}{G}, {T}" looks at the top four, offers a free cast of a spell (not a
 * land) among them, and bottoms the rest — a declined pick included.
 */
class SvellaIceShaperScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.activate(index: Int) {
            val svella = findPermanent("Svella, Ice Shaper")!!
            execute(ActivateAbility(player1Id, svella, SvellaIceShaper.activatedAbilities[index].id)).error shouldBe null
            resolveStack()
        }

        fun TestGame.libraryNames(): List<String> =
            state.getLibrary(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.name }

        context("Icy Manalith") {
            test("creates a colorless snow artifact token that taps for any color right away") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Svella, Ice Shaper")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activate(0)

                val manalith = game.findPermanent("Icy Manalith")
                manalith shouldNotBe null
                val card = game.state.getEntity(manalith!!)!!.get<CardComponent>()!!
                withClue("Icy Manalith is a colorless noncreature snow artifact") {
                    card.typeLine.isSnow shouldBe true
                    card.typeLine.isArtifact shouldBe true
                    card.typeLine.isCreature shouldBe false
                    card.colors shouldBe emptySet()
                }

                withClue("Forest + the fresh Manalith pay {1}{G} for Grizzly Bears") {
                    game.castSpell(1, "Grizzly Bears").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        context("look at the top four") {
            fun bigBoard() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Svella, Ice Shaper")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Forest", 4)
                // Top → bottom: four looked-at cards, then one that stays on top.
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("casts a creature spell for free and bottoms the other three") {
                val game = bigBoard()
                game.activate(1)

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val island = game.findCardsInLibrary(1, "Island").first()
                withClue("a land isn't a spell, so it can't be chosen") {
                    game.selectCards(listOf(island)).error shouldNotBe null
                }
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears was cast without paying its mana cost, at instant-like timing") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                val library = game.libraryNames()
                library.first() shouldBe "Mountain"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Island", "Forest", "Island")
            }

            test("declining puts all four on the bottom") {
                val game = bigBoard()
                game.activate(1)

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                val library = game.libraryNames()
                library.first() shouldBe "Mountain"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Island", "Grizzly Bears", "Forest", "Island")
            }
        }
    }
}
