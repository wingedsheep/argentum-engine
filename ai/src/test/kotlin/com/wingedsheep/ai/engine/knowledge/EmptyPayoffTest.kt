package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ChosenLinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.ai.engine.AiProfile.refuseEmptyPayoffs]: an activation whose whole payoff lands
 * on nothing is floored below passing.
 *
 * Each shape is a position from the 2026-10-10 `live`-profile AI-vs-AI logs: Oakhollow Village
 * putting counters on "each Frog, Rabbit, Raccoon, or Squirrel that entered this turn" when none
 * had (game 1, turns 19–25), Mandibular Kite re-equipped to the creature already wearing it (game 3
 * turn 19), and Dúnedain Blade moved three times in one postcombat main phase (game 1 turn 32).
 * The precombat shape is from the 2026-10-11 BLB-vs-LTR log: Dúnedain Blade shuffled between Shire
 * Shirriff and Mushroom Watchdogs on turns 22 and 24 with nothing attacking.
 */
class EmptyPayoffTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val off by lazy { HoldPolicy(intents) }
    private val on by lazy { HoldPolicy(intents, refuseEmptyPayoffs = true) }

    private fun nonManaAbility(cardName: String): ActivatedAbility =
        cardRegistry.getCard(cardName)!!.script.activatedAbilities.first { !it.isManaAbility }

    /** The last equip ability on [cardName] — the unrestricted "Equip {N}" on Dúnedain Blade. */
    private fun equipAbility(cardName: String): ActivatedAbility =
        cardRegistry.getCard(cardName)!!.script.activatedAbilities.last { it.isEquipAbility }

    private fun TestGame.villageVerdict(policy: HoldPolicy): TimingVerdict {
        val activation = ActivateAbility(player1Id, findPermanent("Oakhollow Village")!!, nonManaAbility("Oakhollow Village").id)
        return policy.verdictFor(state, player1Id, "Oakhollow Village", activation = activation)
    }

    private fun TestGame.equipVerdict(policy: HoldPolicy, equipment: String, onto: String): TimingVerdict {
        val activation = ActivateAbility(
            player1Id, findPermanent(equipment)!!, equipAbility(equipment).id,
            targets = listOf(ChosenTarget.Permanent(findPermanent(onto)!!)),
        )
        return policy.verdictFor(state, player1Id, equipment, activation = activation)
    }

    private val koh = "Koh, the Face Stealer"

    private fun TestGame.kohVerdict(policy: HoldPolicy): TimingVerdict {
        val activation = ActivateAbility(player1Id, findPermanent(koh)!!, nonManaAbility(koh).id)
        return policy.verdictFor(state, player1Id, koh, activation = activation)
    }

    /**
     * Builds Koh with [pile] exiled with it (in order), the card at [chosenIndex] already stamped as
     * the last chosen card, and the opponent holding a board that is winning the race.
     */
    private fun kohGame(pile: List<String>, chosenIndex: Int?, step: Step = Step.PRECOMBAT_MAIN, theirTurn: Boolean = false): TestGame {
        var builder = scenario().withPlayers().withCardOnBattlefield(1, koh)
        for (card in pile) builder = builder.withCardInExile(1, card)
        if (theirTurn) builder = builder.withActivePlayer(2)
        val game = builder
            .withCardOnBattlefield(2, "Hill Giant")
            .build().advanceToPriority(1, step)
        val exiled = game.state.getZone(game.player1Id, Zone.EXILE)
        game.state = game.state.updateEntity(game.findPermanent(koh)!!) { koh ->
            val piled = koh.with(LinkedExileComponent(exiled))
            if (chosenIndex == null) piled else piled.with(ChosenLinkedExileComponent(exiled[chosenIndex]))
        }
        return game
    }

    /** Marks [name] as having had an ability activated this turn, as an earlier equip would have. */
    private fun TestGame.markActivatedThisTurn(name: String) {
        val id = findPermanent(name)!!
        state = state.updateEntity(id) { it.with(AbilityActivatedThisTurnComponent().withAnyActivated()) }
    }

    init {

        // --- Group effect over an empty group ---------------------------------------------------

        test("Oakhollow Village with no qualifying creature entered this turn: no window") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oakhollow Village")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Intrepid Rabbit")
                .withCardOnBattlefield(1, "Grizzly Bears", enteredThisTurn = true)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.villageVerdict(off) shouldBe TimingVerdict.Neutral
            game.villageVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("a Rabbit that entered this turn keeps the Village live") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oakhollow Village")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Intrepid Rabbit", enteredThisTurn = true)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.villageVerdict(on) shouldBe TimingVerdict.Neutral
        }

        test("an opponent's Rabbit that entered this turn is not ours to grow") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oakhollow Village")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(2, "Intrepid Rabbit", enteredThisTurn = true)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.villageVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("game 1 turn 19: the agent floors the empty Village activation") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oakhollow Village")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Intrepid Rabbit")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withTurnNumber(19)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_EMPTYPAYOFF, AiProfile.LIVE)) {
                val (action, option) = scoredOption(game, profile, "Oakhollow Village") { true }
                withClue("${profile.id} chose $action") {
                    option.note shouldBe "hold policy: wrong window — floored below passing"
                    ((action as? ActivateAbility)?.abilityId == nonManaAbility("Oakhollow Village").id) shouldBe false
                }
            }
        }

        // --- Attach to the current host ---------------------------------------------------------

        test("Mandibular Kite re-equipped to the creature already wearing it: no window") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Eldrazi Repurposer")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Mandibular Kite", "Eldrazi Repurposer")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.equipVerdict(off, "Mandibular Kite", onto = "Eldrazi Repurposer") shouldBe TimingVerdict.Neutral
            game.equipVerdict(on, "Mandibular Kite", onto = "Eldrazi Repurposer") shouldBe TimingVerdict.NoWindow
            // Moving it precombat onto a creature that is no better an attacker is floored too (see
            // the precombat section below); a move onto a creature that can attack while the holder
            // can't is a real choice the leaf score keeps pricing.
            game.equipVerdict(on, "Mandibular Kite", onto = "Grizzly Bears") shouldBe TimingVerdict.NoWindow
        }

        test("game 3 turn 19: the agent floors equipping the Kite to its own host") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Eldrazi Repurposer")
                .withCardAttachedTo(1, "Mandibular Kite", "Eldrazi Repurposer")
                .withLandsOnBattlefield(2, "Island", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withTurnNumber(19)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_EMPTYPAYOFF, AiProfile.LIVE)) {
                val (action, option) = scoredOption(game, profile, "Mandibular Kite") { true }
                withClue("${profile.id} chose $action") {
                    option.note shouldBe "hold policy: wrong window — floored below passing"
                    ((action as? ActivateAbility)?.sourceId == game.findPermanent("Mandibular Kite")) shouldBe false
                }
            }
        }

        // --- A second Equipment move after combat -----------------------------------------------

        test("Dúnedain Blade moved again in the postcombat main phase: no window") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.markActivatedThisTurn("Dúnedain Blade")

            game.equipVerdict(off, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.Neutral
            game.equipVerdict(on, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.NoWindow
        }

        test("the first postcombat move is left to the score") {
            val post = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            post.equipVerdict(on, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.Neutral
        }

        // --- A precombat Equipment move onto no better an attacker ------------------------------

        test("precombat, moving the Blade between two equally able attackers: no window") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.equipVerdict(off, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.Neutral
            game.equipVerdict(on, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.NoWindow
        }

        test("precombat, moving the Blade onto a creature that can't attack this turn: no window") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = true)
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.equipVerdict(on, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.NoWindow
        }

        test("precombat, a move that buys an attack or evasion is left to the score") {
            // The holder is summoning sick; the new holder can attack.
            val sick = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            sick.equipVerdict(on, "Dúnedain Blade", onto = "Hill Giant") shouldBe TimingVerdict.Neutral

            // Both can attack, but the new holder flies and the old one doesn't.
            val flier = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Wax-Wane Witness")
                .withCardAttachedTo(1, "Dúnedain Blade", "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            flier.equipVerdict(on, "Dúnedain Blade", onto = "Wax-Wane Witness") shouldBe TimingVerdict.Neutral
        }

        test("precombat, equipping an unattached Blade is left to the score") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Dúnedain Blade")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.equipVerdict(on, "Dúnedain Blade", onto = "Grizzly Bears") shouldBe TimingVerdict.Neutral
        }

        test("BLB vs LTR game 1 turn 22: the agent floors shuffling the Blade with nothing attacking") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 10)
                .withCardOnBattlefield(1, "Shire Shirriff")
                .withCardOnBattlefield(1, "Mushroom Watchdogs")
                .withCardAttachedTo(1, "Dúnedain Blade", "Shire Shirriff")
                .withLandsOnBattlefield(2, "Plains", 8)
                .withCardOnBattlefield(2, "Wax-Wane Witness")
                .withCardOnBattlefield(2, "Brightblade Stoat")
                .withTurnNumber(22)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_EMPTYPAYOFF, AiProfile.LIVE)) {
                val (action, option) = scoredOption(game, profile, "Dúnedain Blade") { true }
                withClue("${profile.id} chose $action") {
                    option.note shouldBe "hold policy: wrong window — floored below passing"
                    ((action as? ActivateAbility)?.sourceId == game.findPermanent("Dúnedain Blade")) shouldBe false
                }
            }
        }

        test("an unattached Blade is still equipped onto an attacker precombat") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Dúnedain Blade")
                .withLandsOnBattlefield(2, "Island", 3)
                .withTurnNumber(10)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_EMPTYPAYOFF, AiProfile.LIVE)) {
                val action = AIPlayer.create(cardRegistry, game.player1Id, profile).chooseAction(game.state)
                withClue("${profile.id} chose $action") {
                    val activation = action as? ActivateAbility
                    activation?.sourceId shouldBe game.findPermanent("Dúnedain Blade")
                    activation?.targets shouldBe listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                }
            }
        }

        // --- Choosing again from a pile with nothing new to choose (Koh, the Face Stealer) -------

        test("Koh with a creature in the pile and nothing chosen yet: the first choice is live") {
            val game = kohGame(listOf("Grizzly Bears"), chosenIndex = null)

            game.kohVerdict(off) shouldBe TimingVerdict.Neutral
            game.kohVerdict(on) shouldBe TimingVerdict.Neutral
        }

        test("Koh with an empty pile: nothing to choose, no window") {
            val game = kohGame(emptyList(), chosenIndex = null)

            game.kohVerdict(off) shouldBe TimingVerdict.Neutral
            game.kohVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("Koh whose only creature is already the chosen card: choosing again is a no-op") {
            val game = kohGame(listOf("Grizzly Bears"), chosenIndex = 0)

            game.kohVerdict(off) shouldBe TimingVerdict.Neutral
            game.kohVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("Koh with another creature to swap to keeps the choice live") {
            val game = kohGame(listOf("Grizzly Bears", "Hill Giant"), chosenIndex = 0)

            game.kohVerdict(on) shouldBe TimingVerdict.Neutral
        }

        test("a pile of noncreature cards offers nothing to choose") {
            val game = kohGame(listOf("Forest"), chosenIndex = null)

            game.kohVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("Koh that already activated this turn does not swap faces a second time") {
            val game = kohGame(listOf("Grizzly Bears", "Hill Giant"), chosenIndex = 0)
            game.markActivatedThisTurn(koh)

            game.kohVerdict(off) shouldBe TimingVerdict.Neutral
            game.kohVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("2026-11-08 game, turn 31: the agent no longer pays life to re-choose the one creature it has") {
            // The recorded position scored the payment +0.79 above passing in the opponent's
            // declare-blockers step of another player's turn, and the AI paid 38 times, from 40 life
            // down to 2. The toy board here is not that losing position, so this pins the verdict
            // the agent acts on; the unit tests above pin the shape.
            val game = kohGame(listOf("Draconautics Engineer"), chosenIndex = 0, step = Step.END, theirTurn = true)

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_EMPTYPAYOFF, AiProfile.LIVE)) {
                val (action, option) = scoredOption(game, profile, koh) { true }
                withClue("${profile.id} chose $action") {
                    option.note shouldBe "hold policy: wrong window — floored below passing"
                    ((action as? ActivateAbility)?.abilityId == nonManaAbility(koh).id) shouldBe false
                }
            }
        }
    }

    /** What the Strategist made of [cardName]'s ability matching [which], and the action it chose. */
    private fun scoredOption(
        game: TestGame,
        profile: AiProfile,
        cardName: String,
        which: (AiActionOption) -> Boolean,
    ): Pair<GameAction, AiActionOption> {
        val options = mutableListOf<AiActionOption>()
        val sink = AiInsightSink { _, insight ->
            options += insight.options.filter { it.cardName == cardName && which(it) }
        }
        val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink).chooseAction(game.state)
        val floored = options.firstOrNull { it.note != null } ?: options.firstOrNull()
        return action to (floored ?: error("${profile.id} never scored $cardName"))
    }
}
