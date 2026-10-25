package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.OswaldFiddlebender
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Oswald Fiddlebender (AFR #28). The artifact sacrificed as a cost sets an *exact* search target:
 * an artifact card with mana value equal to its mana value plus 1.
 */
class OswaldFiddlebenderScenarioTest : ScenarioTestBase() {

    private val tinkering = OswaldFiddlebender.activatedAbilities.single().id

    init {
        test("sacrificing a mana value 0 artifact finds only a mana value 1 artifact card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Oswald Fiddlebender")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Short Sword")    // artifact, MV 1 — the only legal find
                .withCardInLibrary(1, "Memnite")        // artifact, MV 0 — not equal
                .withCardInLibrary(1, "Millstone")      // artifact, MV 2 — not equal
                .withCardInLibrary(1, "Llanowar Elves") // MV 1 but not an artifact
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oswald = game.findPermanent("Oswald Fiddlebender")!!
            val thopter = game.findPermanent("Ornithopter")!!
            val sword = game.findCardsInLibrary(1, "Short Sword").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, oswald, tinkering,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(thopter)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.resolveStack()

            val search = game.getPendingDecision() as SelectCardsDecision
            withClue("only the mana value 1 artifact card is findable") {
                search.options shouldContainExactlyInAnyOrder listOf(sword)
            }
            game.selectCards(listOf(sword)).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.BATTLEFIELD) shouldContain sword
            game.isOnBattlefield("Millstone") shouldBe false
            game.isOnBattlefield("Memnite") shouldBe false
        }

        test("can't be activated outside a main phase with an empty stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Oswald Fiddlebender")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Short Sword")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            val oswald = game.findPermanent("Oswald Fiddlebender")!!
            val thopter = game.findPermanent("Ornithopter")!!

            (game.execute(
                ActivateAbility(
                    game.player1Id, oswald, tinkering,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(thopter)),
                )
            ).error != null) shouldBe true
            game.isOnBattlefield("Ornithopter") shouldBe true
        }
    }
}
