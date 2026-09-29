package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfNewCapenna
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of New Capenna // Holy Frazzle-Cannon.
 *
 * Front: an optional sacrifice of an artifact or creature; when you do, exile target artifact or
 * creature an opponent controls (target chosen only after the sacrifice). Back: whenever the
 * equipped creature attacks, it and each other creature you control sharing a creature type with it
 * get exactly one +1/+1 counter.
 */
class InvasionOfNewCapennaScenarioTest : ScenarioTestBase() {

    private fun plusOne(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("front: sacrificing a creature exiles target artifact or creature an opponent controls") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of New Capenna")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Invasion of New Capenna").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            // Bears is the only artifact or creature you control, so the sacrifice auto-picks it.
            if (game.getPendingDecision() is SelectCardsDecision) {
                game.selectCards(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.findPermanent("Ornithopter")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Ornithopter") shouldBe true
        }

        test("front: declining the sacrifice exiles nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of New Capenna")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of New Capenna").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }

        test("back: the equipped attacker and each creature sharing a type with it get one counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of New Capenna")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of New Capenna")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val cannon = game.findPermanent("Holy Frazzle-Cannon")
            cannon shouldNotBe null

            val myBears = game.findAllPermanents("Grizzly Bears")
                .filter { game.state.projectedState.getController(it) == game.player1Id }
            val opponentBears = game.findAllPermanents("Grizzly Bears")
                .single { game.state.projectedState.getController(it) == game.player2Id }
            val (attacker, otherBears) = myBears[0] to myBears[1]
            val giant = game.findPermanent("Hill Giant")!!

            val equipId = InvasionOfNewCapenna.backFace!!.activatedAbilities[0].id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = cannon!!,
                    abilityId = equipId,
                    targets = listOf(ChosenTarget.Permanent(attacker)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(
                com.wingedsheep.engine.core.DeclareAttackers(
                    game.player1Id,
                    mapOf(attacker to game.player2Id),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the equipped attacker gets exactly one counter") { plusOne(game, attacker) shouldBe 1 }
            withClue("the other Bear you control shares a type") { plusOne(game, otherBears) shouldBe 1 }
            withClue("Hill Giant shares no type") { plusOne(game, giant) shouldBe 0 }
            withClue("the opponent's Bear is not yours") { plusOne(game, opponentBears) shouldBe 0 }
        }
    }
}
