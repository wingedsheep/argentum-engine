package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.YennaRedtoothRegent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Granted read ahead (CR 702.155) on the entry seams besides a cast spell. Read ahead is a
 * replacement effect on entering, so a Saga granted it ("Sagas you control have read ahead" —
 * Barbara Wright) chooses its chapter however it enters:
 *  - played as a land (Urza's Saga): the question comes before the lore counters, so it ends with
 *    exactly the chosen number, not the intrinsic one plus the choice;
 *  - put onto the battlefield by an effect (Open the Vaults): each of your Sagas asks, an
 *    opponent's returning under their control doesn't;
 *  - created as a token copy (Yenna, Redtooth Regent): the token asks as it arrives.
 * A Saga with read ahead printed asks once, never again for the grant (redundant instances).
 */
class GrantedReadAheadScenarioTest : ScenarioTestBase() {

    init {
        context("granted read ahead") {

            fun TestGame.lore(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

            fun TestGame.chooseChapter(chapter: Int) {
                val decision = getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                decision.playerId shouldBe player1Id
                decision.maxValue shouldBe 3
                submitDecision(NumberChosenResponse(decision.id, chapter)).error shouldBe null
            }

            test("a Saga land played under Barbara asks the chapter before its lore counters land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInHand(1, "Urza's Saga")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Urza's Saga").single()

                game.execute(PlayLand(game.player1Id, card)).error shouldBe null
                game.chooseChapter(2)
                game.resolveStack()

                val saga = game.findPermanent("Urza's Saga")!!
                withClue("exactly the chosen two counters — none placed before the question") {
                    game.lore(saga) shouldBe 2
                }
            }
            test("a Saga an effect returns to the battlefield chooses its chapter as it enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInGraveyard(1, "History of Benalia")
                    .withCardInGraveyard(2, "History of Benalia")
                    .withCardInHand(1, "Open the Vaults")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Open the Vaults").error shouldBe null
                game.resolveStack()
                game.chooseChapter(2)
                game.resolveStack()

                val sagas = game.findPermanents("History of Benalia")
                sagas.size shouldBe 2
                val mine = sagas.single { game.state.projectedState.getController(it) == game.player1Id }
                val theirs = sagas.single { it != mine }
                withClue("mine started at chapter II") { game.lore(mine) shouldBe 2 }
                withClue("the opponent's entered with the single intrinsic counter") { game.lore(theirs) shouldBe 1 }
                withClue("each made one Knight: my chapter II, their chapter I") {
                    game.findPermanents("Knight Token").size shouldBe 2
                }
            }

            test("a token copy of a Saga chooses its chapter as it arrives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                    .withCardOnBattlefield(1, "History of Benalia")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
                val original = game.findPermanent("History of Benalia")!!
                val copyAbilityId = YennaRedtoothRegent.activatedAbilities.first { !it.isManaAbility }.id

                game.execute(
                    ActivateAbility(game.player1Id, yenna, copyAbilityId, listOf(ChosenTarget.Permanent(original)))
                ).error shouldBe null
                game.resolveStack()
                game.chooseChapter(2)
                game.resolveStack()

                val token = game.findPermanents("History of Benalia").single { it != original }
                withClue("the token entered with the chosen two lore counters") { game.lore(token) shouldBe 2 }
                withClue("only its chapter II triggered: one Knight") {
                    game.findPermanents("Knight Token").size shouldBe 1
                }
            }

            test("a Saga with read ahead printed is asked once, not again for the grant") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInHand(1, "Founding the Third Path")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Founding the Third Path").error shouldBe null
                game.resolveStack()
                game.chooseChapter(2)

                withClue("no second chapter question") {
                    (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
                }
                val saga = game.findPermanent("Founding the Third Path")!!
                game.lore(saga) shouldBe 2
            }
        }
    }
}
