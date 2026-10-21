package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for the "whenever day becomes night or night becomes day" trigger
 * ([Triggers.dayBecomesNightOrNightBecomesDay]) and the as-enters "if it's neither day nor night, it
 * becomes day" clause ([becomesDayAsEntersIfNeither]) — Innistrad: Midnight Hunt.
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 731.1a | "day becomes night" = losing day and gaining night | "day becoming night fires the trigger" |
 * | 731.1a | "night becomes day" = losing night and gaining day | "night becoming day fires the trigger" |
 * | 731.1a (neg) | the game's first designation is not a change between the two | "the first designation doesn't fire the trigger" |
 * | 731.1 (neg) | re-declaring the current designation changes nothing | "it becoming day while it's day doesn't fire" |
 * | 731.2a wiring | the untap-step check is a change like any other | "the untap step's day-to-night change fires the trigger" |
 * | 113.6b | an ability declaring a zone functions only from there | "a graveyard-zone trigger fires from the graveyard" |
 * | as-enters | neither → day as the permanent enters, with no trigger off it | "entering while neither makes it day" |
 * | as-enters (neg) | a designation already held stays | "entering at night leaves it night" |
 */
class DayNightChangeTriggerScenarioTest : ScenarioTestBase() {

    private val watcher = card("Test Day Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        becomesDayAsEntersIfNeither()
        triggeredAbility {
            trigger = Triggers.dayBecomesNightOrNightBecomesDay
            effect = Effects.GainLife(1, EffectTarget.Controller)
        }
    }

    private val graveWatcher = card("Test Grave Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.dayBecomesNightOrNightBecomesDay
            triggerZone = Zone.GRAVEYARD
            effect = Effects.GainLife(1, EffectTarget.Controller)
        }
    }

    private val dusk = card("Test Dusk") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.BecomeNight }
    }

    private val dawn = card("Test Dawn") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.BecomeDay }
    }

    private val filler = card("Test Filler") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    init {
        listOf(watcher, graveWatcher, dusk, dawn, filler).forEach { cardRegistry.register(it) }

        context("The trigger (CR 731.1a)") {

            test("day becoming night fires the trigger") {
                val game = board(DayNight.DAY) {
                    withCardOnBattlefield(1, "Test Day Watcher")
                    withCardInHand(1, "Test Dusk")
                }
                game.castSpell(1, "Test Dusk").error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.NIGHT
                withClue("day becomes night → the watcher's trigger resolves") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("night becoming day fires the trigger") {
                val game = board(DayNight.NIGHT) {
                    withCardOnBattlefield(1, "Test Day Watcher")
                    withCardInHand(1, "Test Dawn")
                }
                game.castSpell(1, "Test Dawn").error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.DAY
                withClue("night becomes day → the watcher's trigger resolves") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("the first designation doesn't fire the trigger") {
                val game = board(null) {
                    withCardOnBattlefield(1, "Test Day Watcher")
                    withCardInHand(1, "Test Dawn")
                }
                game.castSpell(1, "Test Dawn").error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.DAY
                withClue("neither → day is the game gaining a designation, not night becoming day") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("it becoming day while it's day doesn't fire") {
                val game = board(DayNight.DAY) {
                    withCardOnBattlefield(1, "Test Day Watcher")
                    withCardInHand(1, "Test Dawn")
                }
                game.castSpell(1, "Test Dawn").error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.DAY
                withClue("no designation change, no trigger") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("the untap step's day-to-night change fires the trigger") {
                // Player 1 ends a turn having cast no spells while it's day; player 2's untap step
                // makes it night (CR 731.2a), and player 1's watcher sees the change.
                val game = board(DayNight.DAY, phase = Phase.ENDING, step = Step.END) {
                    withCardOnBattlefield(1, "Test Day Watcher")
                }
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.state.activePlayerId shouldBe game.player2Id
                game.state.dayNight shouldBe DayNight.NIGHT
                withClue("the turn-based change triggers like any other") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("a graveyard-zone trigger fires from the graveyard") {
                val game = board(DayNight.DAY) {
                    withCardInGraveyard(1, "Test Grave Watcher")
                    withCardInGraveyard(1, "Test Day Watcher")
                    withCardInHand(1, "Test Dusk")
                }
                game.castSpell(1, "Test Dusk").error shouldBe null
                game.resolveStack()

                withClue("only the graveyard-zone ability fires; the battlefield one stays silent in the graveyard") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }
        }

        context("As-enters designation start") {

            test("entering while neither makes it day") {
                val game = board(null) { withCardInHand(1, "Test Day Watcher") }
                game.castSpell(1, "Test Day Watcher").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Test Day Watcher") shouldBe true
                withClue("neither → day as it enters") { game.state.dayNight shouldBe DayNight.DAY }
                withClue("its own trigger doesn't see the first designation") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("entering at night leaves it night") {
                val game = board(DayNight.NIGHT) { withCardInHand(1, "Test Day Watcher") }
                game.castSpell(1, "Test Day Watcher").error shouldBe null
                game.resolveStack()

                withClue("a designation the game already holds is kept") { game.state.dayNight shouldBe DayNight.NIGHT }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }

    private fun board(
        dayNight: DayNight?,
        phase: Phase = Phase.PRECOMBAT_MAIN,
        step: Step = Step.PRECOMBAT_MAIN,
        extra: ScenarioBuilder.() -> Unit,
    ): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(6) {
            builder.withCardInLibrary(1, "Test Filler")
            builder.withCardInLibrary(2, "Test Filler")
        }
        builder.extra()
        val game = builder.withActivePlayer(1).inPhase(phase, step).build()
        game.state = game.state.copy(dayNight = dayNight)
        return game
    }
}
