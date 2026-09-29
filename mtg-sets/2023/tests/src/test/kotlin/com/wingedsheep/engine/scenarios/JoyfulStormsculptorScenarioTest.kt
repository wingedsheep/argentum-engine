package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Joyful Stormsculptor (MOM #243) — "When this creature enters, create two 1/1 blue and red
 * Elemental creature tokens. Whenever you cast a spell that has convoke, this creature deals 1
 * damage to each opponent and each battle they protect."
 */
class JoyfulStormsculptorScenarioTest : ScenarioTestBase() {

    private fun defenseOf(game: TestGame, battle: EntityId): Int =
        game.state.getEntity(battle)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) ?: 0

    init {
        test("enters: creates two 1/1 blue and red Elemental tokens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Joyful Stormsculptor")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Joyful Stormsculptor").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.findPermanents("Elemental Token").size shouldBe 2
        }

        test("casting a convoke spell deals 1 to each opponent and each battle they protect, not yours") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Joyful Stormsculptor")
                .withCardInHand(1, "Gather Courage")
                .withLandsOnBattlefield(1, "Forest", 1)
                // A Siege you cast is protected by the opponent; theirs is protected by you.
                .withCardOnBattlefield(1, "Invasion of Innistrad")
                .withCardOnBattlefield(2, "Invasion of Innistrad")
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()

            val stormsculptor = game.findPermanent("Joyful Stormsculptor")!!
            val sieges = game.findPermanents("Invasion of Innistrad")
            val opponentProtects = sieges.single { game.state.projectedState.getController(it) == game.player1Id }
            val youProtect = sieges.single { it != opponentProtects }
            defenseOf(game, opponentProtects) shouldBe 5
            defenseOf(game, youProtect) shouldBe 5

            game.castSpell(1, "Gather Courage", stormsculptor).error shouldBe null
            game.resolveStack()

            withClue("opponent takes 1") { game.getLifeTotal(2) shouldBe 19 }
            withClue("you take nothing") { game.getLifeTotal(1) shouldBe 20 }
            withClue("battle the opponent protects loses a defense counter") {
                defenseOf(game, opponentProtects) shouldBe 4
            }
            withClue("battle you protect is untouched") { defenseOf(game, youProtect) shouldBe 5 }
        }

        test("a spell without convoke does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Joyful Stormsculptor")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLifeTotal(2, 20)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val stormsculptor = game.findPermanent("Joyful Stormsculptor")!!
            game.castSpell(1, "Giant Growth", stormsculptor).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
        }
    }
}
