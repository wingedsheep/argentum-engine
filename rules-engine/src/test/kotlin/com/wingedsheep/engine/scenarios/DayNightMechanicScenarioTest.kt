package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the **day/night** designation (CR 731) and the **daybound/nightbound** keywords
 * (CR 702.145) — Innistrad: Crimson Vow.
 *
 * The engine is the source of truth, so every clause of the rules gets a paired test that would fail
 * if that clause were implemented wrong:
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 731.1 | the game starts with no day/night designation | "the game starts as neither day nor night" |
 * | 731.2a | untap step, it's day, prev active player cast 0 spells ⇒ night | "day + previous player cast no spells becomes night" (+ wiring test) |
 * | 731.2a (neg) | it's day, prev player cast ≥1 ⇒ stays day | "day + a spell stays day" |
 * | 731.2b | untap step, it's night, prev active player cast ≥2 spells ⇒ day | "night + previous player cast two becomes day" |
 * | 731.2b (neg) | it's night, prev player cast 1 ⇒ stays night | "night + a single spell stays night" |
 * | 731.2c | it's neither ⇒ no check happens | "neither day nor night skips the untap check" |
 * | 502.2 wiring | the check runs as the untap turn-based action | "the untap step performs the day/night check" |
 * | 702.145d | control a daybound permanent while neither ⇒ day | "a daybound permanent makes it day" |
 * | 702.145g | control a nightbound (no daybound anywhere) while neither ⇒ night | "a lone nightbound permanent makes it night" |
 * | 702.145g gate | daybound present ⇒ day wins even with a nightbound out | "daybound wins the designation-start tie" |
 * | 702.145b#2 / e#1 | a designation change transforms out-of-step permanents | asserted inside the untap tests via the cascade |
 * | 702.145c | while it's night a daybound front face transforms to back | "night reconciles a daybound permanent to its back" |
 * | 702.145f | while it's day a nightbound back face transforms to front | "day reconciles a nightbound permanent to its front" |
 * | 702.145b#3 | a daybound/nightbound permanent can't transform except via its keyword | "a transform effect can't flip a daybound permanent" |
 */
class DayNightMechanicScenarioTest : ScenarioTestBase() {

    // ── Test cards ───────────────────────────────────────────────────────────────
    // A single werewolf DFC carries both keywords: daybound on the front, nightbound on the back —
    // the real VOW shape. Behaviour under test is the engine's, not the card's.

    private val werewolf: CardDefinition = CardDefinition.doubleFacedCreature(
        frontFace = card("Test Daybound Werewolf") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Human Werewolf"
            power = 2
            toughness = 2
            daybound()
        },
        backFace = card("Test Nightbound Werewolf") {
            manaCost = ""
            colorIndicator = "G"
            typeLine = "Creature — Werewolf"
            power = 3
            toughness = 3
            nightbound()
            triggeredAbility {
                trigger = Triggers.TransformsToBack
                effect = Effects.GainLife(1, EffectTarget.Controller)
            }
        },
    )

    /** No day/night keyword at all — the control, and library filler that can't deck anyone. */
    private val plainCreature = card("Test Plain Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    private val reanimate = card("Test Daybound Reanimate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val target = target(
                "target creature card in your graveyard",
                TargetObject(filter = TargetFilter.CreatureInYourGraveyard),
            )
            effect = Effects.Move(target, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        }
    }

    init {
        listOf(werewolf, plainCreature, reanimate).forEach { cardRegistry.register(it) }

        context("The game's starting designation (CR 731.1)") {

            test("the game starts as neither day nor night") {
                val game = board { withCardOnBattlefield(1, "Test Plain Creature") }
                withClue("CR 731.1 — a game begins with no day or night designation") {
                    game.state.dayNight shouldBe null
                }
            }
        }

        context("The untap-step turn-based action (CR 502.2 / 731.2)") {

            test("day + previous active player cast no spells becomes night (731.2a)") {
                val game = board { withCardOnBattlefield(1, "Test Daybound Werewolf") }
                val wolf = game.findPermanent("Test Daybound Werewolf")!!
                game.state = game.state.copy(
                    dayNight = DayNight.DAY,
                    previousTurnActiveTeamSpellCounts = mapOf(game.player1Id to 0),
                )

                val (after, events) = DayNightService.checkUntapStepDesignation(game.state, cardRegistry)

                withClue("731.2a — no spells last turn while it's day flips it to night") {
                    after.dayNight shouldBe DayNight.NIGHT
                }
                withClue("the flip cascades the daybound → back transform (702.145b#2)") {
                    after.faceOf(wolf) shouldBe DoubleFacedComponent.Face.BACK
                    after.nameOf(wolf) shouldBe "Test Nightbound Werewolf"
                }
                withClue("the designation change and the transform are both announced") {
                    events.size shouldBe 2
                }
            }

            test("day + a spell stays day (731.2a negative)") {
                val game = board { withCardOnBattlefield(1, "Test Daybound Werewolf") }
                game.state = game.state.copy(
                    dayNight = DayNight.DAY,
                    previousTurnActiveTeamSpellCounts = mapOf(game.player1Id to 1),
                )

                val (after, events) = DayNightService.checkUntapStepDesignation(game.state, cardRegistry)

                withClue("a single spell last turn is enough to keep it day") {
                    after.dayNight shouldBe DayNight.DAY
                    events.shouldBeEmpty()
                }
            }

            test("night + previous active player cast two becomes day (731.2b)") {
                val game = board { withCardOnBattlefield(1, "Test Nightbound Werewolf") }
                val wolf = game.findPermanent("Test Nightbound Werewolf")!!
                game.state = game.state.copy(
                    dayNight = DayNight.NIGHT,
                    previousTurnActiveTeamSpellCounts = mapOf(game.player1Id to 2),
                )

                val (after, events) = DayNightService.checkUntapStepDesignation(game.state, cardRegistry)

                withClue("731.2b — two or more spells last turn while it's night flips it to day") {
                    after.dayNight shouldBe DayNight.DAY
                }
                withClue("the flip cascades the nightbound → front transform (702.145e#1)") {
                    after.faceOf(wolf) shouldBe DoubleFacedComponent.Face.FRONT
                    after.nameOf(wolf) shouldBe "Test Daybound Werewolf"
                }
            }

            test("night + a single spell stays night (731.2b boundary)") {
                val game = board { withCardOnBattlefield(1, "Test Nightbound Werewolf") }
                game.state = game.state.copy(
                    dayNight = DayNight.NIGHT,
                    previousTurnActiveTeamSpellCounts = mapOf(game.player1Id to 1),
                )

                val (after, events) = DayNightService.checkUntapStepDesignation(game.state, cardRegistry)

                withClue("731.2b needs two spells — one leaves it night") {
                    after.dayNight shouldBe DayNight.NIGHT
                    events.shouldBeEmpty()
                }
            }

            test("neither day nor night skips the untap check entirely (731.2c)") {
                val game = board { withCardOnBattlefield(1, "Test Plain Creature") }
                game.state = game.state.copy(
                    dayNight = null,
                    previousTurnActiveTeamSpellCounts = mapOf(game.player1Id to 0),
                )

                val (after, events) = DayNightService.checkUntapStepDesignation(game.state, cardRegistry)

                withClue("731.2c — with no designation the check doesn't happen, it stays neither") {
                    after.dayNight shouldBe null
                    events.shouldBeEmpty()
                }
            }

            test("the untap step performs the day/night check (CR 502.2 wiring)") {
                // It's day, player 1 is ending a turn in which they cast no spells. Crossing player 2's
                // untap step must run the turn-based action against player 1's spell count (731.2a).
                val game = board(phase = Phase.ENDING, step = Step.END) {
                    withCardOnBattlefield(1, "Test Daybound Werewolf")
                }
                val wolf = game.findPermanent("Test Daybound Werewolf")!!
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("arriving through a real untap step is what proves the check is in the game loop") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.dayNight shouldBe DayNight.NIGHT
                }
                withClue("and the werewolf transformed with the flip") {
                    game.faceOf(wolf) shouldBe DoubleFacedComponent.Face.BACK
                }
            }
        }

        context("Designation start (CR 702.145d / 702.145g)") {

            test("a daybound permanent makes it day (702.145d)") {
                val game = board { withCardOnBattlefield(1, "Test Daybound Werewolf") }
                val wolf = game.findPermanent("Test Daybound Werewolf")!!
                game.state.dayNight shouldBe null

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("702.145d — controlling a daybound permanent while neither makes it day") {
                    game.state.dayNight shouldBe DayNight.DAY
                }
                withClue("day + a daybound front face is in step, so it doesn't transform") {
                    game.faceOf(wolf) shouldBe DoubleFacedComponent.Face.FRONT
                }
            }

            test("a lone nightbound permanent makes it night (702.145g)") {
                val game = board { withCardOnBattlefield(1, "Test Nightbound Werewolf") }
                val wolf = game.findPermanent("Test Nightbound Werewolf")!!
                game.state.dayNight shouldBe null

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("702.145g — a nightbound permanent with no daybound anywhere makes it night") {
                    game.state.dayNight shouldBe DayNight.NIGHT
                }
                withClue("night + a nightbound back face is in step, so it doesn't transform") {
                    game.faceOf(wolf) shouldBe DoubleFacedComponent.Face.BACK
                }
            }

            test("daybound wins the designation-start tie (702.145g gate)") {
                // A daybound front and a nightbound back on the battlefield at once, neither designation
                // yet. 702.145g's "and there are no permanents with daybound on the battlefield" gate means
                // daybound wins: it becomes day, then 702.145f reconciles the out-of-step nightbound to its
                // front.
                val game = board {
                    withCardOnBattlefield(1, "Test Daybound Werewolf")
                    withCardOnBattlefield(2, "Test Nightbound Werewolf")
                }
                val daybound = game.findPermanent("Test Daybound Werewolf")!!
                val nightbound = game.findPermanent("Test Nightbound Werewolf")!!

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("daybound present ⇒ it becomes day, not night") {
                    game.state.dayNight shouldBe DayNight.DAY
                }
                withClue("the daybound front stays front") {
                    game.faceOf(daybound) shouldBe DoubleFacedComponent.Face.FRONT
                }
                withClue("the nightbound back is out of step in day, so 702.145f flips it to front") {
                    game.faceOf(nightbound) shouldBe DoubleFacedComponent.Face.FRONT
                }
            }
        }

        context("Transform reconciliation once a designation holds (CR 702.145c / 702.145f)") {

            test("a daybound card returned from a graveyard at night enters transformed without transforming") {
                val game = board(
                    dayNight = DayNight.NIGHT,
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                ) {
                    withCardInGraveyard(1, "Test Daybound Werewolf")
                    withCardInHand(1, "Test Daybound Reanimate")
                }
                val lifeBefore = game.getLifeTotal(1)
                val werewolfId = game.findCardsInGraveyard(1, "Test Daybound Werewolf").single()
                val reanimateId = game.findCardsInHand(1, "Test Daybound Reanimate").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = reanimateId,
                        targets = listOf(ChosenTarget.Card(werewolfId, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("daybound modifies how the permanent enters; it must already be back-face up") {
                    val enteredWerewolf =
                        game.findPermanent("Test Nightbound Werewolf")
                            ?: game.findPermanent("Test Daybound Werewolf")!!
                    game.nameOf(enteredWerewolf) shouldBe "Test Nightbound Werewolf"
                    game.faceOf(enteredWerewolf) shouldBe DoubleFacedComponent.Face.BACK
                }
                withClue("entering transformed is not transforming, so the transform trigger must not fire") {
                    game.getLifeTotal(1) shouldBe lifeBefore
                }
            }

            test("night reconciles a daybound permanent to its back (702.145c)") {
                val game = board { withCardOnBattlefield(1, "Test Daybound Werewolf") }
                val wolf = game.findPermanent("Test Daybound Werewolf")!!

                val (after, events) = DayNightService.becomeNight(game.state, cardRegistry, "test")

                withClue("702.145c — a daybound front face is transformed while it's night") {
                    after.faceOf(wolf) shouldBe DoubleFacedComponent.Face.BACK
                    after.nameOf(wolf) shouldBe "Test Nightbound Werewolf"
                }
                withClue("the change and the transform are both announced") {
                    events.size shouldBe 2
                }
            }

            test("day reconciles a nightbound permanent to its front (702.145f)") {
                val game = board { withCardOnBattlefield(1, "Test Nightbound Werewolf") }
                val wolf = game.findPermanent("Test Nightbound Werewolf")!!

                val (after, _) = DayNightService.becomeDay(game.state, cardRegistry, "test")

                withClue("702.145f — a nightbound back face is transformed while it's day") {
                    after.faceOf(wolf) shouldBe DoubleFacedComponent.Face.FRONT
                    after.nameOf(wolf) shouldBe "Test Daybound Werewolf"
                }
            }

            test("re-declaring the current designation is a no-op (idempotent set)") {
                val game = board { withCardOnBattlefield(1, "Test Daybound Werewolf") }
                game.state = game.state.copy(dayNight = DayNight.DAY)

                val (after, events) = DayNightService.becomeDay(game.state, cardRegistry, "test")

                withClue("setting day while already day changes nothing and emits nothing") {
                    after.dayNight shouldBe DayNight.DAY
                    events.shouldBeEmpty()
                }
            }
        }

        context("Can't transform except via the keyword (CR 702.145b#3 / e#2)") {

            test("a transform effect can't flip a daybound permanent") {
                // It's day (in step), so nothing cascades. A plain "transform target creature" effect
                // (the Test Cards' {1}{U} sorcery) reaching a daybound permanent is a disallowed cause
                // and does nothing.
                val game = board(phase = Phase.PRECOMBAT_MAIN, step = Step.PRECOMBAT_MAIN) {
                    withCardOnBattlefield(1, "Test Daybound Werewolf")
                    withCardInHand(1, "Transform Target Creature")
                    withLandsOnBattlefield(1, "Island", 2)
                }
                game.state = game.state.copy(dayNight = DayNight.DAY)
                val wolf = game.findPermanent("Test Daybound Werewolf")!!

                val cast = game.castSpell(1, "Transform Target Creature", wolf)
                withClue("casting the transform spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("702.145b#3 — the daybound permanent can't transform this way, so it stays front") {
                    game.faceOf(wolf) shouldBe DoubleFacedComponent.Face.FRONT
                    game.nameOf(wolf) shouldBe "Test Daybound Werewolf"
                }
            }
        }
    }

    /**
     * Two-player board with stocked libraries so crossing draw steps can't deck anyone. Built at the
     * given [phase]/[step] (upkeep by default) so [passUntilPhase] can advance through real steps and
     * poll the daybound/nightbound SBA. [dayNight] is applied straight onto the built state.
     */
    private fun board(
        activePlayer: Int = 1,
        dayNight: DayNight? = null,
        phase: Phase = Phase.BEGINNING,
        step: Step = Step.UPKEEP,
        extra: ScenarioBuilder.() -> Unit,
    ): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(12) {
            builder.withCardInLibrary(1, "Test Plain Creature")
            builder.withCardInLibrary(2, "Test Plain Creature")
        }
        builder.extra()
        val game = builder
            .withActivePlayer(activePlayer)
            .inPhase(phase, step)
            .build()
        if (dayNight != null) game.state = game.state.copy(dayNight = dayNight)
        return game
    }

    /** Current face of the double-faced permanent [id], read straight off a [GameState]. */
    private fun GameState.faceOf(id: EntityId): DoubleFacedComponent.Face =
        getEntity(id)!!.get<DoubleFacedComponent>()!!.currentFace

    /** Current up-face name of the permanent [id] (changes across a transform). */
    private fun GameState.nameOf(id: EntityId): String =
        getEntity(id)!!.get<CardComponent>()!!.name

    /** [GameState.faceOf] against the game's current state, for the loop-driven tests. */
    private fun TestGame.faceOf(id: EntityId): DoubleFacedComponent.Face = state.faceOf(id)

    /** [GameState.nameOf] against the game's current state. */
    private fun TestGame.nameOf(id: EntityId): String = state.nameOf(id)
}
