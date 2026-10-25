package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.TheBookOfExaltedDeeds
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Book of Exalted Deeds (AFR #4) — {W}{W}{W} Legendary Artifact — Book.
 *
 *   At the beginning of your end step, if you gained 3 or more life this turn, create a 3/3 white
 *   Angel creature token with flying.
 *   {W}{W}{W}, {T}, Exile The Book of Exalted Deeds: Put an enlightened counter on target Angel. It
 *   gains "You can't lose the game and your opponents can't win the game." Activate only as a sorcery.
 */
class TheBookOfExaltedDeedsScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.angelTokens(): Int =
        findPermanents("Angel Token").size

    private fun ScenarioTestBase.TestGame.activateBook(angel: EntityId): com.wingedsheep.engine.core.ExecutionResult =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("The Book of Exalted Deeds")!!,
                abilityId = TheBookOfExaltedDeeds.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(angel))
            )
        )

    init {
        test("gaining 3 life makes an Angel at your end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Book of Exalted Deeds")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(1, "Reviving Dose")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Reviving Dose").error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.angelTokens() shouldBe 1
        }

        test("gaining only 2 life makes no Angel") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Book of Exalted Deeds")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Peach Garden Oath")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Peach Garden Oath").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.angelTokens() shouldBe 0
        }

        test("the exalted Angel's controller can't lose until the Angel leaves the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Book of Exalted Deeds")
                .withCardOnBattlefield(1, "Serra Angel")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLifeTotal(1, 3)
                .withLandsOnBattlefield(2, "Mountain", 5)
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInHand(2, "Lava Axe")
                .withCardInHand(2, "Murder")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findPermanent("Serra Angel")!!
            game.activateBook(angel).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "The Book of Exalted Deeds") shouldBe true
            game.state.getEntity(angel)?.get<CountersComponent>()?.getCount(CounterType.ENLIGHTENED) shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe -2
            game.state.gameOver shouldBe false

            game.castSpell(2, "Murder", angel).error shouldBe null
            game.resolveStack()
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player2Id
        }

        test("activate only as a sorcery") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Book of Exalted Deeds")
                .withCardOnBattlefield(1, "Serra Angel")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateBook(game.findPermanent("Serra Angel")!!).error shouldNotBe null
            game.isOnBattlefield("The Book of Exalted Deeds") shouldBe true
        }
    }
}
