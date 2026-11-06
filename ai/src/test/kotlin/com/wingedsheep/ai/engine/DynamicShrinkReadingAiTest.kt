package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The end-of-turn **shrink** reading ([AiProfile.refuseWastedPayoffs]) for a `ModifyStatsEffect`
 * whose loss is counted off the board — "target creature gets -1/-1 until end of turn for each
 * Swamp you control" (Defile). A printed shrink (Icy Reception's -5/-0) was read; a counted one fell
 * back to the leaf, which prices the lost stats as if they outlived the turn.
 *
 * The position is game `aa989771`'s turn 17 (see [RefuseUnspendableShrinkTest]) with Defile in
 * Icy Reception's seat: at 6 life, in our own precombat main, Defile with two Swamps is -2/-2 on
 * the opponent's *tapped* Shivan Dragon. It kills nothing, and the Dragon can neither block nor
 * attack before cleanup. Cast, it only discounts Traxos; held, it shrinks the Dragon's next attack.
 */
class DynamicShrinkReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(): GameAction =
        AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).chooseAction(state)

    private fun TestGame.defileInHand() = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Defile"
    }

    private fun TestGame.defileVerdict(target: String): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Defile",
            cast = CastSpell(player1Id, defileInHand(), targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!))),
        )

    private fun position(theirCreature: String) = scenario()
        .withPlayers()
        .withTurnNumber(17)
        .withActivePlayer(1)
        .withLifeTotal(1, 6)
        .withLifeTotal(2, 8)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInHand(1, "Defile")
        .withCardInHand(1, "Traxos, Academy Guardian")
        .withCardOnBattlefield(2, theirCreature, tapped = true)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("a counted -X/-X that kills nothing on a creature with no fight left is a card thrown away") {
            val game = position("Shivan Dragon")

            withClue("dynamic shrink reading: Defile's -2/-2 leaves the tapped 5/5 Shivan Dragon alive and fightless") {
                game.defileVerdict("Shivan Dragon") shouldBe TimingVerdict.NoWindow
                ((game.choice() as? CastSpell)?.cardId == game.defileInHand()) shouldBe false
            }
        }

        test("the same count that kills is removal, cast whenever") {
            val game = position("Grizzly Bears")

            withClue("dynamic shrink reading: two Swamps make Defile -2/-2, which kills the 2/2 Grizzly Bears") {
                game.defileVerdict("Grizzly Bears") shouldNotBe TimingVerdict.NoWindow
                ((game.choice() as? CastSpell)?.cardId == game.defileInHand()) shouldBe true
            }
        }

        test("a counted sweep over every creature is removal even with no creature of ours to shrink") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Drag to the Bottom")
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val drag = game.state.getZone(game.player1Id, Zone.HAND).single()

            withClue("dynamic shrink reading: a group shrink is a sweeper, never a wasted shrink on our own (empty) board") {
                HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
                    game.state, game.player1Id, "Drag to the Bottom", cast = CastSpell(game.player1Id, drag),
                ) shouldNotBe TimingVerdict.NoWindow
            }
        }

        test("a base power set with no toughness beside it is judged without a shrink to sum") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Island of Wak-Wak")
                .withCardOnBattlefield(2, "Serra Angel", tapped = true)
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val island = game.findPermanent("Island of Wak-Wak")!!
            val ability = intents.definition("Island of Wak-Wak")!!.script.activatedAbilities.single().id

            withClue("set-base reading: base power 0 on a tapped flier on our turn changes no fight") {
                HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
                    game.state, game.player1Id, "Island of Wak-Wak",
                    activation = ActivateAbility(
                        game.player1Id, island, ability,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Serra Angel")!!)),
                    ),
                ) shouldBe TimingVerdict.NoWindow
            }
        }
    }
}
