package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Curse of Leeches // Leeching Lurker (MID #94).
 *
 * "As this permanent transforms into Curse of Leeches, attach it to a player." (CR 712.20)
 */
class CurseOfLeechesScenarioTest : ScenarioTestBase() {

    /** Pass priority until a decision is pending or the game reaches [step]. */
    private fun TestGame.passUntilDecisionOr(step: Step) {
        var guard = 0
        while (state.pendingDecision == null && state.step != step && guard++ < 50) {
            execute(PassPriority(state.priorityPlayerId ?: error("no priority player at ${state.step}")))
        }
    }

    private fun TestGame.permanentId(): EntityId =
        state.getBattlefield().single {
            state.getEntity(it)?.get<CardComponent>()?.name in setOf("Curse of Leeches", "Leeching Lurker")
        }

    /**
     * Leeching Lurker on the battlefield during the day, so the next settle (after casting Grizzly
     * Bears) transforms it into Curse of Leeches outside the untap step.
     */
    private fun lurkerAtDaybreak(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Leeching Lurker")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.state = game.state.copy(dayNight = DayNight.DAY)
        return game
    }

    private fun TestGame.attachedTo(): EntityId? = state.getEntity(permanentId())?.get<AttachedToComponent>()?.targetId

    init {
        context("Curse of Leeches") {

            test("day breaking in the untap step attaches it to the chosen player, whose upkeep it drains that turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leeching Lurker")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                // Night, and the Lurker's controller cast two spells this turn: the opponent's untap
                // step makes it day (CR 731.2b).
                game.state = game.state.copy(
                    dayNight = DayNight.NIGHT,
                    playerSpellsCastThisTurn = mapOf(game.player1Id to 2)
                )

                game.passUntilDecisionOr(Step.DRAW)

                game.state.dayNight shouldBe DayNight.DAY
                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the choice is the Curse's controller's, made inside the untap step") {
                    decision.playerId shouldBe game.player1Id
                    game.state.step shouldBe Step.UNTAP
                }
                decision.options shouldBe listOf("Opponent", "Player (you)")
                game.state.getEntity(game.permanentId())?.get<CardComponent>()?.name shouldBe "Curse of Leeches"

                game.submitDecision(OptionChosenResponse(decision.id, 0))

                game.attachedTo() shouldBe game.player2Id
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                withClue("the enchanted opponent's upkeep trigger fires the same turn") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("its controller may attach it to themselves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leeching Lurker")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.state = game.state.copy(
                    dayNight = DayNight.NIGHT,
                    playerSpellsCastThisTurn = mapOf(game.player1Id to 2)
                )

                game.passUntilDecisionOr(Step.DRAW)
                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 1))

                game.attachedTo() shouldBe game.player1Id
                game.isOnBattlefield("Curse of Leeches") shouldBe true
            }

            test("a transform outside the untap step is attached before the unattached-Aura check") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leeching Lurker")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                // A nightbound face up during the day is out of step with the designation; the engine
                // reconciles it (CR 702.145f) the next time it settles — after this cast.
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.castSpell(1, "Grizzly Bears").error shouldBe null

                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the Aura waits for its player rather than going to the graveyard") {
                    game.isOnBattlefield("Curse of Leeches") shouldBe true
                }
                game.submitDecision(OptionChosenResponse(decision.id, 0))

                game.attachedTo() shouldBe game.player2Id
                game.isOnBattlefield("Curse of Leeches") shouldBe true
            }

            test("becoming night turns it into Leeching Lurker, which comes off the player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Curse of Leeches")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Curse of Leeches", 2).error shouldBe null
                game.resolveStack()
                game.state.dayNight shouldBe DayNight.DAY
                game.attachedTo() shouldBe game.player2Id

                // Nobody on the active side cast a spell: the opponent's untap step makes it night.
                game.state = game.state.copy(playerSpellsCastThisTurn = emptyMap())
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                game.state.dayNight shouldBe DayNight.NIGHT
                game.isOnBattlefield("Leeching Lurker") shouldBe true
                withClue("a creature attached to a player becomes unattached (CR 704.5p)") {
                    game.attachedTo() shouldBe null
                }
                withClue("the Lurker has no upkeep trigger") {
                    game.state.stack.size shouldBe 0
                    game.getLifeTotal(2) shouldBe 20
                }
                game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).size shouldBe 0
            }

            test("attaching doesn't target, so an opponent with hexproof can still be chosen") {
                val game = lurkerAtDaybreak()
                game.state = game.state.updateEntity(game.player2Id) { it.with(PlayerHexproofComponent()) }

                game.castSpell(1, "Grizzly Bears").error shouldBe null

                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                decision.options shouldBe listOf("Opponent", "Player (you)")
                game.submitDecision(OptionChosenResponse(decision.id, 0))
                game.attachedTo() shouldBe game.player2Id
            }

            test("a player with protection from black can't be chosen, leaving its controller") {
                val game = lurkerAtDaybreak()
                game.state = game.state.updateEntity(game.player2Id) {
                    it.with(PlayerProtectionComponent(listOf(ProtectionScope.Color(Color.BLACK))))
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null

                withClue("one legal player is no choice at all (CR 702.16c)") {
                    game.state.pendingDecision shouldBe null
                }
                game.attachedTo() shouldBe game.player1Id
                game.isOnBattlefield("Curse of Leeches") shouldBe true
            }

            test("with no player it could enchant, it's an unattached Aura and goes to the graveyard") {
                val game = lurkerAtDaybreak()
                for (player in listOf(game.player1Id, game.player2Id)) {
                    game.state = game.state.updateEntity(player) {
                        it.with(PlayerProtectionComponent(listOf(ProtectionScope.Color(Color.BLACK))))
                    }
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null

                game.state.pendingDecision shouldBe null
                game.isOnBattlefield("Curse of Leeches") shouldBe false
                game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).size shouldBe 1
            }
        }
    }
}
