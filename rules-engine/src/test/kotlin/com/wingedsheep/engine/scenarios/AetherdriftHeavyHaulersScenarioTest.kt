package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * The Aetherdrift batch that leans on the trickier shared vocabulary rather than on new effects:
 *
 * | Card | What's actually under test |
 * |---|---|
 * | Spikeshell Harrier | "greater than each other player's speed" as a count-of-leaders comparison, and the reduction floor |
 * | Lightwheel Enhancements | `MayCastSelfFromZones` gated by max speed — a permission that has to be live *in the graveyard* |
 * | March of the World Ooze | one `CompositeStaticAbility` spanning Layer 4 + Layer 7b, and an intervening-if on the casting player |
 * | Riptide Gearhulk | the positional library move ("third from the top") |
 * | Boommobile | a restricted-spend mana ETB, and an exhaust ability with X |
 */
class AetherdriftHeavyHaulersScenarioTest : ScenarioTestBase() {

    init {
        context("Spikeshell Harrier") {

            test("bounces the target and slows the sole speed leader") {
                val game = harrierGame()
                val giant = game.findPermanent("Hill Giant")!!
                // Opponent alone at speed 3; the Harrier's controller sits at 1.
                game.state = SpeedService.set(game.state, game.player2Id, 3, "test").first

                game.castHarrierAt(giant)

                withClue("The creature is returned to its owner's hand") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
                withClue("Nobody else matches their speed, so it drops by 1") {
                    game.state.speed(game.player2Id) shouldBe 2
                }
            }

            test("leaves speed alone when another player is tied for the lead") {
                val game = harrierGame()
                val giant = game.findPermanent("Hill Giant")!!
                game.state = SpeedService.set(game.state, game.player1Id, 3, "test").first
                game.state = SpeedService.set(game.state, game.player2Id, 3, "test").first

                game.castHarrierAt(giant)

                withClue("The bounce is unconditional") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("\"greater than each other player's speed\" is strict — a tie is not greater") {
                    game.state.speed(game.player2Id) shouldBe 3
                }
            }

            test("can't push the leader below 1") {
                val game = harrierGame()
                val giant = game.findPermanent("Hill Giant")!!
                // Opponent leads at 1 because the controller has no speed at all (reads as 0).
                game.state = SpeedService.set(game.state, game.player2Id, Speed.STARTING, "test").first

                game.castHarrierAt(giant)

                withClue("The printed floor holds: \"can't reduce their speed below 1\"") {
                    game.state.speed(game.player2Id) shouldBe Speed.STARTING
                }
            }
        }

        context("Lightwheel Enhancements") {

            test("is castable from the graveyard only at max speed") {
                val game = lightwheelGame()
                val bear = game.findPermanent("Grizzly Bears")!!

                val blocked = game.castSpellFromGraveyard(1, "Lightwheel Enhancements", bear)
                withClue("Below max speed the graveyard permission isn't there at all") {
                    (blocked.error != null).shouldBeTrue()
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val allowed = game.castSpellFromGraveyard(1, "Lightwheel Enhancements", bear)
                withClue("At max speed it casts from the graveyard: ${allowed.error}") {
                    allowed.error shouldBe null
                }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("It attaches and grants +1/+1 and vigilance") {
                    game.isOnBattlefield("Lightwheel Enhancements") shouldBe true
                    game.state.projectedState.getPower(bear) shouldBe 3
                    game.state.projectedState.getToughness(bear) shouldBe 3
                    game.state.projectedState.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
                }
            }
        }

        context("March of the World Ooze") {

            test("makes your creatures base 6/6 Oozes without touching your opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "March of the World Ooze")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val projected = game.state.projectedState

                withClue("Layer 7b sets base P/T, Layer 4 adds the subtype — both from one ability") {
                    projected.getPower(bear) shouldBe 6
                    projected.getToughness(bear) shouldBe 6
                    projected.hasSubtype(bear, "Ooze") shouldBe true
                }
                withClue("\"in addition to their other types\" keeps the printed one") {
                    projected.hasSubtype(bear, "Bear") shouldBe true
                }
                withClue("\"Creatures you control\" excludes the opponent's board") {
                    projected.getPower(giant) shouldBe 3
                    projected.hasSubtype(giant, "Ooze") shouldBe false
                }
            }

            test("an opponent casting during your turn makes an Elephant, but not on their own turn") {
                val onYourTurn = marchTriggerGame(activePlayer = 1)
                onYourTurn.castSpell(2, "Giant Growth", onYourTurn.findPermanent("Hill Giant")!!)
                onYourTurn.autoPayIfAsked()
                onYourTurn.resolveStack()
                withClue("It isn't the caster's turn, so the intervening-if holds") {
                    onYourTurn.elephantTokens() shouldBe 1
                }

                val onTheirTurn = marchTriggerGame(activePlayer = 2)
                onTheirTurn.castSpell(2, "Giant Growth", onTheirTurn.findPermanent("Hill Giant")!!)
                onTheirTurn.autoPayIfAsked()
                onTheirTurn.resolveStack()
                withClue("On the caster's own turn the trigger is suppressed") {
                    onTheirTurn.elephantTokens() shouldBe 0
                }
            }
        }

        context("Riptide Gearhulk") {

            test("buries a targeted permanent third from the top of its owner's library") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Riptide Gearhulk")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Plains", 3)
                repeat(6) {
                    builder.withCardInLibrary(1, "Grizzly Bears")
                    builder.withCardInLibrary(2, "Grizzly Bears")
                }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Riptide Gearhulk")
                withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()
                if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(listOf(giant))
                game.resolveStack()

                val library = game.state.getLibrary(game.player2Id)
                withClue("Index 0 is the top of the library, so third from the top is index 2") {
                    library.indexOf(giant) shouldBe 2
                }
                withClue("It goes to its owner's library, not the caster's") {
                    game.state.getLibrary(game.player1Id).contains(giant) shouldBe false
                }
            }
        }

        context("Boommobile") {

            test("its enter trigger fills the pool with four mana of one chosen color") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Boommobile")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Boommobile")
                withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()
                val colorPick = game.getPendingDecision()
                if (colorPick is ChooseColorDecision) {
                    game.submitDecision(ColorChosenResponse(colorPick.id, Color.RED))
                }
                game.resolveStack()

                withClue("\"Add four mana of any one color\" is four of a single pick, not a mix") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.total shouldBe 4
                }
            }

            test("its exhaust ability shoots for X once, then never again") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Boommobile")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 10)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boommobile = game.findPermanent("Boommobile")!!
                val giant = game.findPermanent("Hill Giant")!!
                val abilityId = cardRegistry.getCard("Boommobile")!!.script.activatedAbilities.single().id

                val fire = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boommobile,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                        xValue = 3
                    )
                )
                withClue("Activation failed: ${fire.error}") { fire.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("X damage kills the 3/3, and the Vehicle grows regardless") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.state.projectedState.getPower(boommobile) shouldBe 6
                    game.state.projectedState.getToughness(boommobile) shouldBe 6
                }

                val again = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = boommobile, abilityId = abilityId, xValue = 1)
                )
                withClue("Exhaust means \"Activate only once\" per object") {
                    (again.error != null).shouldBeTrue()
                }
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────────

    /** Player 1 holding Spikeshell Harrier with five Islands, facing a Hill Giant. */
    private fun harrierGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Spikeshell Harrier")
            .withCardOnBattlefield(2, "Hill Giant")
            .withLandsOnBattlefield(1, "Island", 5)
        stockLibraries(builder)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Cast the Harrier, point its enter trigger at [victim], and let everything resolve. */
    private fun TestGame.castHarrierAt(victim: EntityId) {
        val cast = castSpell(1, "Spikeshell Harrier")
        withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
        autoPayIfAsked()
        resolveStack()
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(victim))
        resolveStack()
    }

    /**
     * Lightwheel Enhancements already in player 1's graveyard, one Plains to recast it, and a
     * Grizzly Bears to enchant. Built in the upkeep step so the CR 704.5z start-your-engines
     * state-based action has a step change to run on.
     */
    private fun lightwheelGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Lightwheel Enhancements")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 1)
        stockLibraries(builder)
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /**
     * March of the World Ooze under player 1, with player 2 holding a Giant Growth and the mana for
     * it. [activePlayer] decides whose turn the opponent casts on — the whole point of the
     * intervening-if.
     */
    private fun marchTriggerGame(activePlayer: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "March of the World Ooze")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(2, "Giant Growth")
            .withLandsOnBattlefield(2, "Forest", 1)
        stockLibraries(builder)
        return builder
            .withActivePlayer(activePlayer)
            .withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────

    private fun stockLibraries(builder: ScenarioBuilder) {
        repeat(10) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
    }

    /** Elephant creature tokens on the battlefield. */
    private fun TestGame.elephantTokens(): Int =
        state.getBattlefield().count { id ->
            val container = state.getEntity(id) ?: return@count false
            container.has<TokenComponent>() &&
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Elephant" } == true
        }

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }
}
