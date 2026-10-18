package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Dragon-Kami Reborn // Dragon-Kami's Egg (NEO #181).
 *
 * Chapters I and II gain 2 life and exile one of the top three cards face down with a hatching
 * counter; the Egg's death (or a Dragon's) lets its owner cast a hatched creature card for free.
 */
class TheDragonKamiRebornScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.exiled(name: String): EntityId =
            state.getExile(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun TestGame.hatch(id: EntityId) {
            state = state.updateEntity(id) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.HATCHING, 1))
            }
        }

        context("The Dragon-Kami Reborn") {

            test("chapter I gains 2 life and exiles the chosen card face down with a hatching counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Dragon-Kami Reborn")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "The Dragon-Kami Reborn").error shouldBe null
                game.resolveStack()

                val pick = game.getPendingDecision()
                pick.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                withClue("the other two go to the bottom in an order of the player's choosing") {
                    game.getPendingDecision().shouldBeInstanceOf<ReorderLibraryDecision>()
                }
                game.keepLibraryOrder()

                game.getLifeTotal(1) shouldBe 22
                game.librarySize(1) shouldBe 2
                val hatched = game.exiled("Grizzly Bears")
                withClue("exiled face down, carrying one hatching counter") {
                    game.state.getEntity(hatched)!!.get<FaceDownComponent>() shouldNotBe null
                    game.state.getEntity(hatched)!!.get<CountersComponent>()!!
                        .getCount(CounterType.HATCHING) shouldBe 1
                }
            }

            test("the Egg dying lets its owner cast a hatched creature card without paying its mana cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dragon-Kami's Egg")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInExile(1, "Hill Giant")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.exiled("Grizzly Bears")
                game.hatch(bears)
                val egg = game.findPermanent("Dragon-Kami's Egg")!!

                game.castSpell(1, "Lightning Bolt", egg).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the exiled creature card with a hatching counter is offered") {
                    decision.options shouldContainExactly listOf(bears)
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.isInExile(1, "Hill Giant") shouldBe true
            }

            test("a card hatched face down by chapter I is cast when the Egg later dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Dragon-Kami Reborn")
                    .withCardOnBattlefield(1, "Dragon-Kami's Egg")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "The Dragon-Kami Reborn").error shouldBe null
                game.resolveStack()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.keepLibraryOrder()
                val hatched = game.exiled("Grizzly Bears")
                game.state.getEntity(hatched)!!.get<FaceDownComponent>() shouldNotBe null

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Dragon-Kami's Egg")!!).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the face-down hatched card is still read as a creature card") {
                    decision.options shouldContainExactly listOf(hatched)
                }
                game.selectCards(listOf(hatched)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            test("a Dragon you control dying also triggers the Egg") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dragon-Kami's Egg")
                    .withCardOnBattlefield(1, "Dragon Whelp")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.exiled("Grizzly Bears")
                game.hatch(bears)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Dragon Whelp")!!).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanent("Dragon-Kami's Egg") shouldNotBe null
            }
        }
    }
}
