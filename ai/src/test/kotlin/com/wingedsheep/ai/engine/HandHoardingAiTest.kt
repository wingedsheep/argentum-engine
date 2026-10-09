package com.wingedsheep.ai.engine

import com.wingedsheep.ai.insight.AiDecisionInsight
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Affordable cards left in hand with the mana to cast them sitting idle — the hand-hoarding
 * misplays from the 2026-10-09 AI-vs-AI game logs, one position each, rebuilt small.
 *
 * Two different defects, which is why they are asserted against different profiles:
 *  - **The hand curve outbidding the board** ([AiProfile.permanentCastIsNotCardLoss]). Casting the
 *    last card cost 3.0 of `CardAdvantage` at `concave-hand-2`, and a summoning-sick 1/1 is worth
 *    ~1.8 on the board, so passing won. Asserted on [AiProfile.PRODUCTION_CANDIDATE_DEPLOY]; the
 *    agent the logs were taken with keeps the old behaviour by construction.
 *  - **Craft never materialized**. The engine wants the exiled material named on the action and the
 *    AI named none, so every Craft was rejected and dropped. A bug, fixed for every profile.
 */
class HandHoardingAiTest : ScenarioTestBase() {

    /** A library each, so a rollout's draw step finds cards — see `PuzzleRunner.stockLibraries`. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(20) { withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
    }

    private fun decide(game: TestGame, profile: AiProfile): Pair<GameAction, List<AiDecisionInsight>> {
        val insights = mutableListOf<AiDecisionInsight>()
        val ai = AIPlayer.create(cardRegistry, game.player1Id, profile) { _, insight -> insights += insight }
        return ai.chooseAction(game.state) to insights
    }

    /** The scored options, for the failure message — the number that lost is the diagnosis. */
    private fun describe(insights: List<AiDecisionInsight>): String =
        insights.flatMap { it.options }.joinToString("\n") {
            "  ${it.label}: score=${it.score} advantage=${it.advantage} ${it.note.orEmpty()}"
        }

    private fun castName(game: TestGame, action: GameAction): String? =
        (action as? CastSpell)?.let { game.state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    /**
     * [profile] casts [cardName] here, and the agent the logs were taken with does not — so each
     * position is shown to reproduce the logged misplay, not merely to be a position the fix plays.
     */
    private fun shouldCast(game: TestGame, profile: AiProfile, cardName: String) {
        val (action, insights) = decide(game, profile)
        withClue("${profile.id} should cast $cardName, chose $action\n${describe(insights)}") {
            castName(game, action) shouldBe cardName
        }
        val (logged, loggedInsights) = decide(game, AiProfile.PRODUCTION_CANDIDATE_EXPIRING)
        withClue("the logged agent should still hold $cardName here\n${describe(loggedInsights)}") {
            castName(game, logged) shouldBe null
        }
    }

    init {
        val deploy = AiProfile.PRODUCTION_CANDIDATE_DEPLOY

        // Game 21 T19–T23: Wose Pathfinder {1}{G} held for five turns with ten lands while six
        // attackers took the seat from 21 to dead.
        test("the last card in hand is a 1/1 — deploy it rather than sit on it with ten lands") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Forest", 10)
                .withCardInHand(1, "Wose Pathfinder")
                .withCardOnBattlefield(1, "Esquire of the King")
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardOnBattlefield(2, "Regal Unicorn")
                .withCardOnBattlefield(2, "Starlit Angel")
                .withCardOnBattlefield(2, "Charging Paladin")
                .stocked().build()
            shouldCast(game, deploy, "Wose Pathfinder")
        }

        // Game 23 T21: at 2 life, eight lands, Hecteyes the only card — and the only extra blocker.
        test("at 2 life the last card is a blocker — cast it") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withCardInHand(1, "Hecteyes")
                .withCardOnBattlefield(1, "Qutrub Forayer")
                .withLifeTotal(1, 2)
                .withCardOnBattlefield(2, "Snowmelt Stag")
                .withCardOnBattlefield(2, "Champion of Dusan")
                .withCardOnBattlefield(2, "Sagu Pummeler")
                .stocked().build()
            shouldCast(game, deploy, "Hecteyes")
        }

        // Game 9 T12–T20: Dragonstorm Globe {3} never cast, held next to a five-drop it can't reach.
        test("a mana rock next to an uncastable five-drop — cast the rock") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(1, "Dragonstorm Globe")
                .withCardInHand(1, "Whirlwing Stormbrood")
                .withCardOnBattlefield(1, "Abzan Devotee")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .stocked().build()
            shouldCast(game, deploy, "Dragonstorm Globe")
        }

        // The control: the refund is for permanents only. A last-card instant keeps its hand value,
        // so a Counterspell with nothing to counter is still held in our own main phase.
        test("the last card is a Counterspell — still held, the refund is for permanents") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(1, "Counterspell")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .stocked().build()
            val (action, insights) = decide(game, deploy)
            withClue("expected a pass, chose $action\n${describe(insights)}") {
                (action is CastSpell) shouldBe false
            }
        }

        // Game 11 T15–T39: empty hand, 8–13 lands, Inverted Iceberg's Craft never used.
        for (profile in listOf(AiProfile.PRODUCTION, AiProfile.PRODUCTION_CANDIDATE_EXPIRING)) {
            test("Craft names its artifact material and is activated [${profile.id}]") {
                val game = scenario().withPlayers()
                    .withLandsOnBattlefield(1, "Island", 9)
                    .withCardOnBattlefield(1, "Inverted Iceberg")
                    .withCardOnBattlefield(1, "Tinker's Tote")
                    .withCardOnBattlefield(1, "Oaken Siren")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .stocked().build()
                val (action, insights) = decide(game, profile)
                withClue("expected the Craft, chose $action\n${describe(insights)}") {
                    val activation = action.shouldBeInstanceOf<ActivateAbility>()
                    game.state.getEntity(activation.sourceId)?.get<CardComponent>()?.name shouldBe
                        "Inverted Iceberg"
                    val exiled = activation.costPayment?.exiledCards.orEmpty()
                    exiled shouldHaveSize 1
                    game.state.getEntity(exiled.single())?.get<CardComponent>()?.name shouldBe "Tinker's Tote"
                }
                game.execute(action).error shouldBe null
            }
        }
    }
}
