package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.refuseWastedPayoffs], as [AiProfile.LIVE] ships it: an end-of-turn shrink on a
 * creature that has no fight left this turn — from an activation and from a spell.
 *
 * Game `3f847e6d` (2026-10-15), turn 8: Proft, Sinister Mastermind ("{B}, Discard this card: Target
 * creature gets -3/-1 until end of turn") discarded from hand after the AI declared no attack, to
 * shrink an opposing 2/4 that the cleanup step restored.
 *
 * Game `aa989771` (2026-10-29), turn 17: the spell twin. At 6 life, in its own precombat main, the
 * AI cast Icy Reception's "target creature gets -5/-0 until end of turn" on the opponent's *tapped*
 * 5/5 flying Dragon — a creature that could neither block nor attack before cleanup — then declared
 * no attack. Held, the same card would have cut the Dragon's attack next turn; it took 8 and died.
 */
class RefuseUnspendableShrinkTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val proftAbility by lazy {
        intents.definition("Proft, Sinister Mastermind")!!.script.activatedAbilities.single().id
    }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.proftInHand() = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Proft, Sinister Mastermind"
    }

    private fun TestGame.shrinkVerdict(target: String, refuseWastedPayoffs: Boolean = true): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs).verdictFor(
            state, player1Id, "Proft, Sinister Mastermind",
            activation = ActivateAbility(
                player1Id, proftInHand(), proftAbility,
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
            ),
        )

    private fun position(activePlayer: Int, step: Step, theirCreature: String = "Serra Angel") = scenario()
        .withPlayers()
        .withTurnNumber(8)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInHand(1, "Proft, Sinister Mastermind")
        .withCardOnBattlefield(2, theirCreature)
        .build()
        .advanceToPriority(1, step)

    private fun TestGame.icyReceptionInHand() = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Icy Reception"
    }

    /** Icy Reception's second mode — "target creature gets -5/-0 until end of turn" — on [target]. */
    private fun TestGame.icyShrink(target: String) = CastSpell(
        player1Id, icyReceptionInHand(),
        targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
        chosenModes = listOf(1),
    )

    private fun TestGame.icyVerdict(target: String, refuseWastedPayoffs: Boolean = true): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs)
            .verdictFor(state, player1Id, "Icy Reception", cast = icyShrink(target))

    private fun TestGame.castsIcyReception(action: GameAction): Boolean =
        (action as? CastSpell)?.cardId == icyReceptionInHand()

    /**
     * Game `aa989771`, turn 17, our precombat main, at 6 life: a 1-loyalty Jace token and a prowess
     * Tomik at home, Traxos (two less after a noncreature spell) in hand beside Icy Reception. Both
     * of the opponent's attackers from last turn are still tapped — Shivan Dragon stands in for the
     * 5/5 flying Dragon token, Heartstring Puller carries its +1/+1 counter — and Kiora is home.
     */
    private fun aa98Turn17(): TestGame {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(17)
            .withActivePlayer(1)
            .withLifeTotal(1, 6)
            .withLifeTotal(2, 8)
            .withLandsOnBattlefield(1, "Island", 2)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardOnBattlefield(1, "Tomik, Izzet Sparkmage")
            .withCardOnBattlefield(1, "Jace", isToken = true)
            .withCardInHand(1, "Icy Reception")
            .withCardInHand(1, "Traxos, Academy Guardian")
            .withCardOnBattlefield(2, "Shivan Dragon", tapped = true)
            .withCardOnBattlefield(2, "Heartstring Puller", tapped = true)
            .withCardOnBattlefield(2, "Kiora of Fire and Ashes")
            .build()
            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
        game.withCounters("Jace", CounterType.LOYALTY, 1)
        game.withCounters("Heartstring Puller", CounterType.PLUS_ONE_PLUS_ONE, 1)
        return game
    }

    private fun TestGame.withCounters(cardName: String, type: CounterType, count: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(type, count))
        }
    }

    init {
        test("aa98 T17: a power cut on a tapped opposing creature on our own turn has no fight to change") {
            val game = aa98Turn17()

            game.icyVerdict("Shivan Dragon") shouldBe TimingVerdict.NoWindow
            game.icyVerdict("Shivan Dragon", refuseWastedPayoffs = false) shouldNotBe TimingVerdict.NoWindow
            // Before the floor, the shrink won as the spell that discounts Traxos.
            game.castsIcyReception(game.choice()) shouldBe false
        }

        test("before our attack, an instant power cut on an untapped blocker waits for it to block") {
            val game = aa98Turn17()

            game.icyVerdict("Kiora of Fire and Ashes") shouldBe TimingVerdict.NoWindow
        }

        test("with no creature of ours that can attack, an untapped blocker has nothing to block") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(17)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Icy Reception")
                .withCardOnBattlefield(2, "Kiora of Fire and Ashes")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.icyVerdict("Kiora of Fire and Ashes") shouldBe TimingVerdict.NoWindow
        }

        test("on the opponent's turn, an instant power cut waits for the creature to attack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(18)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Icy Reception")
                .withCardOnBattlefield(2, "Shivan Dragon")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.icyVerdict("Shivan Dragon") shouldBe TimingVerdict.NoWindow
        }

        test("T8: after our combat, a shrink that kills nothing has no fight to change") {
            val game = position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN)

            game.shrinkVerdict("Serra Angel") shouldBe TimingVerdict.NoWindow
            game.shrinkVerdict("Serra Angel", refuseWastedPayoffs = false) shouldBe TimingVerdict.Neutral
            (game.choice() as? ActivateAbility)?.sourceId shouldNotBe game.proftInHand()
        }

        test("a shrink that kills its target is removal, and stays the leaf's call") {
            val game = position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN, theirCreature = "Raging Goblin")

            game.shrinkVerdict("Raging Goblin") shouldNotBe TimingVerdict.NoWindow
        }

        test("before their attack, a creature that can still attack is a fight to shrink") {
            val game = position(activePlayer = 2, step = Step.PRECOMBAT_MAIN)

            game.shrinkVerdict("Serra Angel") shouldNotBe TimingVerdict.NoWindow
        }
    }
}
