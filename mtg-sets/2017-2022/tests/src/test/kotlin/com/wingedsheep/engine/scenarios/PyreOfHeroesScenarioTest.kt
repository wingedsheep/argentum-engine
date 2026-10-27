package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.PyreOfHeroes
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Pyre of Heroes (KHM #241). The creature sacrificed as a cost is read as it last existed on the
 * battlefield for both halves of the search: a creature card that shares a creature type with it
 * and has mana value exactly one more.
 */
class PyreOfHeroesScenarioTest : ScenarioTestBase() {

    private val pyreAbility = PyreOfHeroes.activatedAbilities.single().id

    init {
        test("finds only a creature card sharing a creature type with mana value one more") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pyre of Heroes")
                .withCardOnBattlefield(1, "Llanowar Elves")      // Elf Druid, MV 1
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Elvish Visionary")        // Elf Shaman, MV 2 — legal
                .withCardInLibrary(1, "Woodland Changeling")     // Changeling, MV 2 — legal (every type)
                .withCardInLibrary(1, "Grizzly Bears")           // Bear, MV 2 — no shared type
                .withCardInLibrary(1, "Elvish Mystic")           // Elf, MV 1 — wrong mana value
                .withCardInLibrary(1, "Imperious Perfect")       // Elf, MV 3 — wrong mana value
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pyre = game.findPermanent("Pyre of Heroes")!!
            val elves = game.findPermanent("Llanowar Elves")!!
            val visionary = game.findCardsInLibrary(1, "Elvish Visionary").single()
            val changeling = game.findCardsInLibrary(1, "Woodland Changeling").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, pyre, pyreAbility,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(elves)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            game.resolveStack()

            val search = game.getPendingDecision() as SelectCardsDecision
            withClue("only MV 2 creature cards sharing a type with the sacrificed Elf are findable") {
                search.options shouldContainExactlyInAnyOrder listOf(visionary, changeling)
            }
            game.selectCards(listOf(visionary)).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.BATTLEFIELD) shouldContain visionary
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("a sacrificed Changeling shares a creature type with any creature card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pyre of Heroes")
                .withCardOnBattlefield(1, "Woodland Changeling") // MV 2, every creature type
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Gray Ogre")               // Ogre, MV 3 — legal
                .withCardInLibrary(1, "Imperious Perfect")       // Elf Warrior, MV 3 — legal
                .withCardInLibrary(1, "Grizzly Bears")           // Bear, MV 2 — wrong mana value
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pyre = game.findPermanent("Pyre of Heroes")!!
            val changeling = game.findPermanent("Woodland Changeling")!!
            val ogre = game.findCardsInLibrary(1, "Gray Ogre").single()
            val perfect = game.findCardsInLibrary(1, "Imperious Perfect").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, pyre, pyreAbility,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(changeling)),
                )
            ).error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision() as SelectCardsDecision
            withClue("the sacrificed Changeling's last-known types include every creature type") {
                search.options shouldContainExactlyInAnyOrder listOf(ogre, perfect)
            }
            game.selectCards(listOf(ogre)).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.BATTLEFIELD) shouldContain ogre
        }

        test("can't be activated outside a main phase with an empty stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pyre of Heroes")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Elvish Visionary")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            val pyre = game.findPermanent("Pyre of Heroes")!!
            val elves = game.findPermanent("Llanowar Elves")!!

            (game.execute(
                ActivateAbility(
                    game.player1Id, pyre, pyreAbility,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(elves)),
                )
            ).error != null) shouldBe true
            game.isOnBattlefield("Llanowar Elves") shouldBe true
        }
    }
}
