package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The 2026-11-02 user report on `{X}` spells and a do-nothing shrink, as [AiProfile.LIVE] plays them:
 *
 *  - Finale of Revelation cast for X=0 — "draw zero cards, exile this" — because a spell whose mana
 *    reached no X went out bare, and the bare action is cast at X=0.
 *  - Ajani's Anguish "cast as soon as it can" — an X spell fired for X=1 with two more land drops in
 *    hand ([AiProfile.holdXSpellsForBiggerX]).
 *  - Perfected Theory making an opposing creature 1/1 until end of turn where it can fight nothing —
 *    an end-of-turn base-P/T set read as the shrink it is ([AiProfile.refuseWastedPayoffs]).
 */
class XSpellPatienceTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.inHand(name: String) = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == name
    }

    /** [AiProfile.LIVE] without this report's two Ajani's Anguish readings. */
    private val beforeTheReport = AiProfile.LIVE.copy(
        holdXSpellsForBiggerX = false,
        creatureValuation = AiProfile.LIVE.creatureValuation.copy(entryTriggersAreSpent = false),
    )

    /** The AI's choice once it has made the land drop it would make first. */
    private fun TestGame.choiceAfterLandDrop(profile: AiProfile = AiProfile.LIVE): GameAction {
        val first = choice(profile)
        if (first !is PlayLand) return first
        execute(first)
        return choice(profile)
    }

    /** The verdict on Perfected Theory's 1/1 mode aimed at [target]. */
    private fun TestGame.theoryVerdict(target: String): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Perfected Theory",
            cast = CastSpell(
                player1Id, inHand("Perfected Theory"),
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
                chosenModes = listOf(0),
            ),
        )

    private fun TestGame.casts(action: GameAction, name: String): Boolean =
        (action as? CastSpell)?.cardId == inHand(name)

    init {
        // With a full hand the card a X=0 cast throws away is priced as the discard it would be
        // anyway, and the last-main-phase idle-draw nudge made "draw zero cards" beat passing.
        test("Finale of Revelation is not cast off exactly its two blue pips") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Finale of Revelation")
                .apply { repeat(7) { withCardInHand(1, "Serra Angel") } }
                .build()
                .advanceToPriority(1, Step.POSTCOMBAT_MAIN)

            game.casts(game.choice(), "Finale of Revelation") shouldBe false
        }

        test("Ajani's Anguish is not cast for X=0 off a single red") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(2)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Ajani's Anguish")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.POSTCOMBAT_MAIN)

            game.casts(game.choice(), "Ajani's Anguish") shouldBe false
        }

        test("Ajani's Anguish is held for a bigger X while land drops are still in hand") {
            fun turnTwo() = scenario()
                .withPlayers()
                .withTurnNumber(3)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Ajani's Anguish")
                .build()
                .advanceToPriority(1, Step.POSTCOMBAT_MAIN)

            turnTwo().let { it.casts(it.choiceAfterLandDrop(), "Ajani's Anguish") } shouldBe false
            // Before: the X=1 ping went out, scored as a removal spell's worth of enchantment.
            turnTwo().let { it.casts(it.choiceAfterLandDrop(beforeTheReport), "Ajani's Anguish") } shouldBe true
        }

        test("Ajani's Anguish still kills the creature it can reach once the mana has stopped growing") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Ajani's Anguish")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val action = game.choice()
            game.casts(action, "Ajani's Anguish") shouldBe true
            (action as CastSpell).xValue shouldBe 3
        }

        test("Perfected Theory's 1/1 on an opposing creature that can fight nothing this turn is refused") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Perfected Theory")
                .withCardOnBattlefield(2, "Serra Angel", tapped = true)
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val shrink = CastSpell(
                game.player1Id, game.inHand("Perfected Theory"),
                targets = listOf(ChosenTarget.Permanent(game.findPermanent("Serra Angel")!!)),
                chosenModes = listOf(0),
            )
            HoldPolicy(intents, refuseWastedPayoffs = true)
                .verdictFor(game.state, game.player1Id, "Perfected Theory", cast = shrink) shouldBe TimingVerdict.NoWindow
            HoldPolicy(intents, refuseWastedPayoffs = false)
                .verdictFor(game.state, game.player1Id, "Perfected Theory", cast = shrink) shouldNotBe TimingVerdict.NoWindow
            game.casts(game.choice(), "Perfected Theory") shouldBe false
        }

        test("Perfected Theory waits for the fight: not on a blocker before our attack, yes on their attacker") {
            val beforeOurAttack = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Perfected Theory")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            beforeOurAttack.theoryVerdict("Serra Angel") shouldBe TimingVerdict.NoWindow

            val theirAttack = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Perfected Theory")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Serra Angel" to 1)) }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            theirAttack.theoryVerdict("Serra Angel") shouldNotBe TimingVerdict.NoWindow
        }

        test("Perfected Theory's 4/5 saves our creature from burn on the stack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInHand(1, "Perfected Theory")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            game.castSpell(2, "Shock", game.findPermanent("Grizzly Bears"))
            game.advanceToStackResponse(1)

            val action = game.choice() as? CastSpell
            action?.cardId shouldBe game.inHand("Perfected Theory")
            action?.chosenModes shouldBe listOf(1)
        }
    }
}
