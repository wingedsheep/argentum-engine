package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.GrowthPotential
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A permanent with a **repeatable activated ability** that adds to its controller's board — a
 * token every turn (Kiora of Fire and Ashes' "{8}: Create a 5/5 red Dragon creature token with
 * flying"), a +1/+1 counter every turn (Unholy Officiant's "{4}{W}: Put a +1/+1 counter on this
 * creature") — is worth what it will keep making with the mana its controller has
 * ([GrowthPotential]), not only its body.
 *
 * The production game (2026-11-02, games 13c4c7a7 and ff5f5856): the AI's removal took a 5/5 Dragon
 * token while Kiora stood beside it on nine and eleven lands, and the Dragons kept coming until
 * they won.
 */
class RepeatableProducerAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val growth by lazy { intents.growth!! }

    /** The permanent [profile]'s AI aims its removal at, or what it did instead. */
    private fun TestGame.removalTarget(profile: AiProfile = AiProfile.LIVE): String {
        val action = AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)
        val cast = action as? CastSpell ?: return "no cast: $action"
        val id = (cast.targets.single() as ChosenTarget.Permanent).entityId
        return state.getEntity(id)?.get<CardComponent>()?.name ?: "?"
    }

    /** Resolve Kiora's own "{8}: create a Dragon" once, so the token on the board is the real one. */
    private fun TestGame.kioraMakesADragon() {
        val kiora = findPermanent("Kiora of Fire and Ashes")!!
        val effect = cardRegistry.getCard("Kiora of Fire and Ashes")!!.script.activatedAbilities.single().effect
        state = EngineServices(cardRegistry).effectExecutorRegistry
            .execute(state, effect, EffectContext(sourceId = kiora, controllerId = player2Id))
            .state
    }

    /**
     * The 13c4c7a7 turn-17 position, simplified: Kiora and one of her Dragons across the table,
     * the opponent on [opponentLands] lands, and the AI holding Terminal Criticism (destroy target
     * blue or red creature), which can hit either.
     */
    private fun kioraBoard(opponentLands: Int): TestGame {
        val game = scenario().withPlayers()
            .withTurnNumber(17)
            .withActivePlayer(1)
            .withLifeTotal(1, 19)
            .withLifeTotal(2, 18)
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardInHand(1, "Terminal Criticism")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Mountain", opponentLands)
            .withCardOnBattlefield(2, "Kiora of Fire and Ashes")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Mountain")
            .build()
            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
        game.kioraMakesADragon()
        return game
    }

    init {
        test("a mana- or tap-only, untargeted activation that makes tokens or counters is read as production") {
            growth.producersOf("Kiora of Fire and Ashes") shouldHaveSize 1
            growth.producersOf("Unholy Officiant") shouldHaveSize 1
            // A vanilla body, and an activation that only pumps until end of turn (Shivan Dragon's
            // firebreathing) — nothing it leaves behind.
            growth.producersOf("Grizzly Bears").shouldBeEmpty()
            growth.producersOf("Shivan Dragon").shouldBeEmpty()
        }

        test("Kiora on nine lands is worth her stream of Dragons; on four lands, about her body") {
            val rich = kioraBoard(opponentLands = 9)
            val poor = kioraBoard(opponentLands = 4)
            fun value(game: TestGame) = growth.value(
                game.state, game.state.projectedState, game.findPermanent("Kiora of Fire and Ashes")!!,
                "Kiora of Fire and Ashes",
            )
            value(rich) shouldBeGreaterThan 8.0
            value(poor) shouldBeLessThan 1.0
        }

        // The misplay: Terminal Criticism into the Dragon while Kiora makes another every turn.
        test("removal aims at Kiora, not her Dragon, when her controller can keep paying {8}") {
            val game = kioraBoard(opponentLands = 9)
            val without = game.removalTarget(AiProfile.LIVE_WITHOUT_GROWTH)
            val with = game.removalTarget()
            withClue("with production read: $with; without: $without") {
                // The misplay, still there without the reading.
                without shouldNotBe "Kiora of Fire and Ashes"
                with shouldBe "Kiora of Fire and Ashes"
            }
        }

        // The 13c4c7a7 shape itself: Memory Trap's enters trigger picks its target on resolution
        // (a ChooseTargetsDecision), not at cast.
        test("Memory Trap's enters trigger exiles Kiora, not her Dragon") {
            fun trapTarget(profile: AiProfile): String {
                val game = scenario().withPlayers()
                    .withTurnNumber(17)
                    .withActivePlayer(1)
                    .withLifeTotal(1, 19)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(1, "Memory Trap")
                    .withLandsOnBattlefield(2, "Mountain", 9)
                    .withCardOnBattlefield(2, "Kiora of Fire and Ashes")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .build()
                    .advanceToPriority(1, Step.PRECOMBAT_MAIN)
                game.kioraMakesADragon()
                game.castSpell(1, "Memory Trap").error shouldBe null
                game.resolveStack()
                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val response = AIPlayer.create(cardRegistry, game.player1Id, profile).respondToDecision(game.state, decision)
                val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().single()
                return game.state.getEntity(target)?.get<CardComponent>()?.name ?: "?"
            }
            val without = trapTarget(AiProfile.LIVE_WITHOUT_GROWTH)
            val with = trapTarget(AiProfile.LIVE)
            withClue("with production read: $with; without: $without") {
                without shouldNotBe "Kiora of Fire and Ashes"
                with shouldBe "Kiora of Fire and Ashes"
            }
        }

        // Control: four lands are a long way from {8}. Kiora is a 2/2 there, and the 5/5 flier
        // beside her is the threat — reading production must not force the removal onto her.
        test("on four lands Kiora is not the removal target over her Dragon") {
            kioraBoard(opponentLands = 4).removalTarget() shouldNotBe "Kiora of Fire and Ashes"
        }

        // A counter engine: Unholy Officiant (1/2 vigilance, "{4}{W}: put a +1/+1 counter on this
        // creature") on ten lands grows twice a turn; Grizzly Bears beside it stays a 2/2.
        test("removal aims at a counter engine its controller can feed, over a bigger idle body") {
            fun board(opponentLands: Int) = scenario().withPlayers()
                .withTurnNumber(12)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Eviscerate")
                .withLandsOnBattlefield(2, "Plains", opponentLands)
                .withCardOnBattlefield(2, "Unholy Officiant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Plains")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val base = AiProfile.LIVE.copy(holdRemovalForBetterTargets = false)
            val fed = board(opponentLands = 10)
            val without = fed.removalTarget(AiProfile.LIVE_WITHOUT_GROWTH.copy(holdRemovalForBetterTargets = false))
            val with = fed.removalTarget(base)
            withClue("with production read: $with; without: $without") {
                without shouldBe "Grizzly Bears"
                with shouldBe "Unholy Officiant"
            }
            withClue("on two lands the Officiant cannot grow") {
                board(opponentLands = 2).removalTarget(base) shouldBe "Grizzly Bears"
            }
        }
    }
}
