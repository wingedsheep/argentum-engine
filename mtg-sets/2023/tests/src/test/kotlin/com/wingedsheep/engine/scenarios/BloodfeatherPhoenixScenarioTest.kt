package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bloodfeather Phoenix (MOM #132) — "Flying. This creature can't block. Whenever an instant or
 * sorcery spell you control deals damage to an opponent or battle, you may pay {R}. If you do,
 * return this card from your graveyard to the battlefield. It gains haste until end of turn."
 *
 * The trigger is a damage observer that functions from the graveyard — the shape the trigger
 * index used to file only for battlefield permanents.
 */
class BloodfeatherPhoenixScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int = 2, vararg extra: Pair<Int, String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInGraveyard(1, "Bloodfeather Phoenix")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .apply { extra.forEach { (player, name) -> withCardOnBattlefield(player, name) } }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.payAndReturn() {
        withClue("the graveyard trigger offers the {R} payment") {
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }
        answerYesNo(true)
        getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
        submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("Shock to an opponent lets you pay {R} to return it with haste") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.payAndReturn()

            val phoenix = game.findPermanent("Bloodfeather Phoenix")
            withClue("the phoenix is back on the battlefield") { (phoenix != null) shouldBe true }
            withClue("it gained haste until end of turn") {
                game.state.projectedState.hasKeyword(phoenix!!, Keyword.HASTE) shouldBe true
            }
            game.getLifeTotal(2) shouldBe 18
        }

        test("damage to a battle triggers it too") {
            val game = board(2, 1 to "Invasion of Innistrad")
            game.checkStateBasedActions()
            val siege = game.findPermanent("Invasion of Innistrad")!!
            game.castSpell(1, "Shock", siege).error shouldBe null
            game.resolveStack()
            game.payAndReturn()

            withClue("Shock removed 2 defense counters") {
                game.state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) shouldBe 3
            }
            (game.findPermanent("Bloodfeather Phoenix") != null) shouldBe true
        }

        test("declining the payment leaves it in the graveyard") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Bloodfeather Phoenix") shouldBe true
        }

        test("damage to a creature or to yourself doesn't trigger it") {
            val game = board(1, 2 to "Hill Giant")
            game.castSpell(1, "Shock", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            withClue("a creature is neither an opponent nor a battle") {
                (game.getPendingDecision() is YesNoDecision) shouldBe false
                game.isInGraveyard(1, "Bloodfeather Phoenix") shouldBe true
            }

            val selfGame = board()
            selfGame.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            selfGame.resolveStack()
            withClue("you aren't your own opponent") {
                (selfGame.getPendingDecision() is YesNoDecision) shouldBe false
                selfGame.isInGraveyard(1, "Bloodfeather Phoenix") shouldBe true
            }
        }

        test("an opponent's spell damaging you doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Bloodfeather Phoenix")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() is YesNoDecision) shouldBe false
            game.isInGraveyard(1, "Bloodfeather Phoenix") shouldBe true
        }

        test("it can't block") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Bloodfeather Phoenix")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Bloodfeather Phoenix" to listOf("Grizzly Bears"))).error shouldNotBe null
        }
    }
}
