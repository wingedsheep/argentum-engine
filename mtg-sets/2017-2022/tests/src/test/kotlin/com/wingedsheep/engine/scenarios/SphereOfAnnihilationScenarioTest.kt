package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sphere of Annihilation (AFR #121) — {X}{B} Artifact.
 *
 *   This artifact enters with X void counters on it.
 *   At the beginning of your upkeep, exile this artifact, all creatures and planeswalkers with
 *   mana value less than or equal to the number of void counters on it, and all creature and
 *   planeswalker cards in graveyards with mana value less than or equal to the number of void
 *   counters on it.
 */
class SphereOfAnnihilationScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.voidCounters(): Int =
        state.getEntity(findPermanent("Sphere of Annihilation")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.VOID) ?: 0

    init {
        test("it enters with X void counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sphere of Annihilation")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Sphere of Annihilation", xValue = 2).error shouldBe null
            game.resolveStack()

            game.voidCounters() shouldBe 2
        }

        test("on your upkeep it exiles itself and every creature, on the battlefield or in a graveyard, with small enough mana value") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sphere of Annihilation")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Raging Goblin")
                .withCardInGraveyard(2, "Serra Angel")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()
            val sphere = game.findPermanent("Sphere of Annihilation")!!
            game.state = game.state.updateEntity(sphere) {
                it.with(CountersComponent().withAdded(CounterType.VOID, 2))
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            withClue("the Sphere exiles itself") { game.isInExile(1, "Sphere of Annihilation") shouldBe true }
            withClue("creatures with mana value 2 or less are exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Llanowar Elves") shouldBe true
            }
            withClue("a creature with greater mana value stays") { game.findPermanent("Hill Giant") shouldNotBe null }
            withClue("creature cards in graveyards with mana value 2 or less are exiled") {
                game.isInExile(2, "Raging Goblin") shouldBe true
            }
            withClue("bigger creature cards and noncreature cards stay in the graveyard") {
                game.isInGraveyard(2, "Serra Angel") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }
        }

        test("it does nothing on an opponent's upkeep") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sphere of Annihilation")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.resolveStack()

            game.findPermanent("Sphere of Annihilation") shouldNotBe null
            game.findPermanent("Llanowar Elves") shouldNotBe null
        }
    }
}
