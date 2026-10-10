package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.ai.engine.AiProfile.refuseDeadSearches]: an activation whose whole payoff is a
 * search our library cannot satisfy is floored below passing.
 *
 * The position is the 2026-10-10 AI-vs-AI log's game 3, turn 17: a Forest/Plains deck cracked
 * Seething Landscape (basic Island, Swamp or Mountain) in response to Spell Stutter and lost a land
 * for nothing. The policy cases pin the verdict and its controls; the last case runs the whole
 * agent on that board.
 */
class DeadSearchTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val off by lazy { HoldPolicy(intents) }
    private val on by lazy { HoldPolicy(intents, refuseDeadSearches = true) }

    /** Seat 1 activating the first non-mana ability of its [cardName]. */
    private fun TestGame.activation(cardName: String): ActivateAbility {
        val ability = cardRegistry.getCard(cardName)!!.script.activatedAbilities
            .first { !it.isManaAbility }
        return ActivateAbility(player1Id, findPermanent(cardName)!!, ability.id)
    }

    private fun TestGame.verdict(policy: HoldPolicy, cardName: String) =
        policy.verdictFor(state, player1Id, cardName, activation = activation(cardName))

    private fun ScenarioBuilder.withLibrary(vararg names: String): ScenarioBuilder =
        apply { names.forEach { withCardInLibrary(1, it) } }

    init {

        test("Seething Landscape over a Forest/Plains library finds nothing: no window") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Seething Landscape")
                .withLibrary("Plains", "Forest", "Plains", "Forest", "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(off, "Seething Landscape") shouldBe TimingVerdict.Neutral
            game.verdict(on, "Seething Landscape") shouldBe TimingVerdict.NoWindow
        }

        test("one matching basic left in the library keeps the fetch live") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Seething Landscape")
                .withLibrary("Plains", "Forest", "Swamp", "Forest")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(on, "Seething Landscape") shouldBe TimingVerdict.Neutral
        }

        test("an opponent's matching basics are not ours to find") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Seething Landscape")
                .withLibrary("Plains", "Forest")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Swamp")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(on, "Seething Landscape") shouldBe TimingVerdict.NoWindow
        }

        test("Evolving Wilds with no basic land of any kind left: no window") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Evolving Wilds")
                .withLibrary("Grizzly Bears", "Hill Giant")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(on, "Evolving Wilds") shouldBe TimingVerdict.NoWindow
        }

        test("a creature source declines: its death can be a payoff of its own") {
            // Sakura-Tribe Elder: sacrificing it after it blocks, or to feed a death trigger, is a
            // play whatever the search finds.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Sakura-Tribe Elder")
                .withLibrary("Grizzly Bears", "Hill Giant")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.verdict(on, "Sakura-Tribe Elder") shouldBe TimingVerdict.Neutral
        }

        test("game 3 turn 17: the agent floors the dead fetch in response to Spell Stutter") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Seething Landscape")
                .withCardOnBattlefield(1, "Voltstorm Angel")
                .withCardOnBattlefield(1, "Aerie Auxiliary")
                .withCardOnBattlefield(1, "Mandibular Kite")
                .withCardInHand(1, "Evolution Witness")
                .withCardInHand(1, "Devourer of Destiny")
                .withCardInHand(1, "Petrifying Meddler")
                .withCardInHand(1, "Solstice Zealot")
                .withLibrary(
                    "Plains", "Forest", "Perilous Landscape", "Unfathomable Truths", "Static Prison",
                    "Thraben Charm", "Hexgold Slith", "Plains", "Sheltering Landscape", "Signature Slam",
                    "Emrakul's Messenger", "Faithful Watchdog",
                )
                .withLifeTotal(1, 14)
                .withLandsOnBattlefield(2, "Island", 4)
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardOnBattlefield(2, "Old Flitterfang")
                .withCardOnBattlefield(2, "Aquatic Alchemist")
                .withCardInHand(2, "Spell Stutter")
                .withCardInHand(2, "Island")
                .withCardInHand(2, "Swamp")
                .withCardInHand(2, "Rowdy Research")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withTurnNumber(17)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castSpell(1, "Evolution Witness").error shouldBe null
            withClue("the board needs the fetch untapped after paying for Evolution Witness") {
                game.state.getEntity(game.findPermanent("Seething Landscape")!!)!!.has<TappedComponent>() shouldBe false
            }
            game.advanceToStackResponse(2)
            game.castSpellTargetingStackSpell(2, "Spell Stutter", "Evolution Witness").error shouldBe null
            game.advanceToStackResponse(1)

            // What the Strategist made of the fetch, under a profile without and with the flag.
            fun fetchOption(profile: AiProfile): Pair<GameAction, AiActionOption> {
                var option: AiActionOption? = null
                val sink = AiInsightSink { _, insight ->
                    option = insight.options.firstOrNull { it.cardName == "Seething Landscape" } ?: option
                }
                val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink)
                    .chooseAction(game.state)
                return action to (option ?: error("${profile.id} never scored the fetch"))
            }

            // The shipped profile scores the fetch on its board value alone: in this reconstruction
            // that is below passing, but nothing stops a noisy rollout from lifting it above.
            val (_, before) = fetchOption(AiProfile.PRODUCTION_CANDIDATE_EXPIRING)
            before.note shouldBe null

            for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_DEADSEARCH, AiProfile.LIVE)) {
                val (action, after) = fetchOption(profile)
                withClue("${profile.id} chose $action") {
                    after.note shouldBe "hold policy: wrong window — floored below passing"
                    (action is ActivateAbility) shouldBe false
                }
            }
        }
    }
}
