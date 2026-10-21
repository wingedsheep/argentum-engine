package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Moonsilver Key (MID) — "Search your library for an artifact card with a mana ability or a basic
 * land card": the library cards answer `withManaAbility()` from their printed abilities.
 */
class MoonsilverKeyScenarioTest : ScenarioTestBase() {

    private val searchAbilityId by lazy {
        cardRegistry.requireCard("Moonsilver Key").activatedAbilities[0].id
    }

    init {
        test("finds an artifact with a mana ability or a basic land, not other artifacts or lands") {
            val g = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Moonsilver Key")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Mind Stone")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Ornithopter")
                .withCardInLibrary(1, "Ancient Den")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val key = g.findPermanent("Moonsilver Key")!!

            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = key, abilityId = searchAbilityId))
                .error shouldBe null
            withClue("the Key is sacrificed as a cost") { g.isOnBattlefield("Moonsilver Key") shouldBe false }
            g.resolveStack()

            val decision = g.getPendingDecision() as SelectCardsDecision
            val mindStone = g.findCardsInLibrary(1, "Mind Stone").single()
            val forest = g.findCardsInLibrary(1, "Forest").single()
            val den = g.findCardsInLibrary(1, "Ancient Den").single()
            withClue("Ornithopter has no mana ability and Grizzly Bears is neither; artifact land Ancient Den qualifies") {
                decision.options shouldContainExactlyInAnyOrder listOf(mindStone, forest, den)
            }

            g.selectCards(listOf(mindStone)).error shouldBe null
            g.resolveStack()
            g.isInHand(1, "Mind Stone") shouldBe true
        }
    }
}
