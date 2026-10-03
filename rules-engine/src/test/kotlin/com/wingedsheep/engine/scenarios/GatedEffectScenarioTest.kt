package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Scenario tests for the unified [GatedEffect] resolution frame (phase-rs Lesson 1).
 *
 * Exercised through inline test cards whose ETB triggers carry gated effects:
 *  - [Gate.MayPay] (the lowering target for `Effects.MayPay`) — pay / decline / can't-afford.
 *  - The may-vs-target canonical order: a targeted `then` locks its target at trigger time
 *    (CR 603.3d), *before* the may-pay decision is offered at resolution (CR 117.3a). This is
 *    the timing the old wrapper nesting (`Effects.May(Effects.IfYouDo(...))`) had to get right by
 *    hand; the single frame makes it correct by construction.
 *  - [Gate.MayDecide] — the pure yes/no gate new cards can opt into, and the `Effects.May` facade
 *    that lowers onto it (yes/no end-to-end, plus the `sourceRequiredZone` skip the wrapper owned).
 *  - [Gate.DoAction] — the action-outcome gate (the lowering target for `Effects.IfYouDo`): an action
 *    runs, then its outcome (not a decision) gates `then` vs `otherwise`, and the `Effects.IfYouDo`
 *    facade that lowers onto it.
 */
class GatedEffectScenarioTest : ScenarioTestBase() {

    init {
        // "This creature enters with two charge counters on it. When it enters, you may remove X
        // charge counters from it, where X is the number of cards in your hand. If you do, draw a
        // card." → Gate.MayPay over a dynamic-count RemoveCounters cost (Magnanimous Magistrate's shape)
        cardRegistry.register(
            CardDefinition.creature(
                name = "Counter Spender",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    replacementEffects = listOf(
                        EntersWithCounters(counterType = CounterType.CHARGE, count = 2, selfOnly = true)
                    ),
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_counters"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.MayPay(
                                cost = Effects.RemoveCounters(
                                    CounterType.CHARGE, DynamicAmounts.cardsInYourHand(), EffectTarget.Self
                                ),
                                then = DrawCardsEffect(1)
                            )
                        )
                    )
                )
            )
        )

        // "When this enters, you may pay 2 life. If you do, draw a card." → Gate.MayPay
        cardRegistry.register(
            CardDefinition.creature(
                name = "Gated Drawer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_1"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.MayPay(
                                cost = PayLifeEffect(2),
                                then = DrawCardsEffect(1)
                            )
                        )
                    )
                )
            )
        )

        // "When this enters, you may pay 2 life. If you do, destroy target creature." → Gate.MayPay
        // with a TARGETED payoff: the target is locked when the trigger goes on the stack.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Gated Slayer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Assassin")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_2"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.MayPay(
                                cost = PayLifeEffect(2),
                                then = Effects.Destroy(EffectTarget.ContextTarget(0))
                            ),
                            targetRequirement = TargetObject(filter = TargetFilter.Creature)
                        )
                    )
                )
            )
        )

        // "When this enters, you may draw a card." → Gate.MayDecide (pure yes/no, no cost).
        cardRegistry.register(
            CardDefinition.creature(
                name = "Gated Seer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_3"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = GatedEffect(
                                gate = Gate.MayDecide(),
                                then = DrawCardsEffect(1)
                            )
                        )
                    )
                )
            )
        )

        cardRegistry.register(
            CardDefinition.creature(
                name = "Target Dummy",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype("Golem")),
                power = 0,
                toughness = 3
            )
        )

        // "When this enters, you may draw a card." authored via the Effects.May facade — proves the
        // facade lowers to a Gate.MayDecide that resolves end-to-end through the GatedEffect frame.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Maybe Seer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_4"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.May(DrawCardsEffect(1))
                        )
                    )
                )
            )
        )

        // A Effects.May whose source must be in the graveyard to act. On an ETB trigger the source is
        // on the battlefield, so the gate is skipped silently — no prompt, no draw.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Graveyard-Gated Seer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_5"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.May(DrawCardsEffect(1), sourceRequiredZone = Zone.GRAVEYARD)
                        )
                    )
                )
            )
        )

        // "When this enters, mill a card. If you do, draw a card. Otherwise, lose 1 life." authored
        // directly with a Gate.DoAction: the milling action's outcome (did the graveyard grow?) gates
        // `then` vs `otherwise` — no yes/no decision, the Auto criterion's zone-delta probe decides.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Outcome Seer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_6"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = GatedEffect(
                                gate = Gate.DoAction(action = Patterns.Library.mill(1)),
                                then = DrawCardsEffect(1),
                                otherwise = Effects.LoseLife(1, EffectTarget.Controller)
                            )
                        )
                    )
                )
            )
        )

        // Same behavior authored via the Effects.IfYouDo facade — proves the facade lowers to a
        // Gate.DoAction that resolves end-to-end through the GatedEffect frame.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Outcome Drawer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("GatedEffectScenarioTest_7"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.IfYouDo(
                                action = Patterns.Library.mill(1),
                                then = DrawCardsEffect(1),
                                otherwise = Effects.LoseLife(1, EffectTarget.Controller)
                            )
                        )
                    )
                )
            )
        )

        context("Gate.MayPay (Effects.MayPay lowering)") {

            test("paying the cost runs the payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gated Drawer")
                    .withCardInLibrary(1, "Target Dummy")
                    .withCardInLibrary(1, "Target Dummy")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gated Drawer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                val handBefore = game.handSize(1)
                game.answerYesNo(true)
                game.resolveStack()

                withClue("paid 2 life") { game.getLifeTotal(1) shouldBe 18 }
                withClue("drew a card") { game.handSize(1) shouldBe handBefore + 1 }
            }

            test("declining the cost runs neither cost nor payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gated Drawer")
                    .withCardInLibrary(1, "Target Dummy")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gated Drawer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                val handBefore = game.handSize(1)
                game.answerYesNo(false)
                game.resolveStack()

                withClue("no life paid") { game.getLifeTotal(1) shouldBe 20 }
                withClue("no card drawn") { game.handSize(1) shouldBe handBefore }
            }

            test("an unaffordable cost skips the prompt entirely") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gated Drawer")
                    .withCardInLibrary(1, "Target Dummy")
                    .withLifeTotal(1, 1) // can't pay 2 life
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Gated Drawer").error shouldBe null
                game.resolveStack()

                withClue("no yes/no prompt for an unpayable cost") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("life untouched") { game.getLifeTotal(1) shouldBe 1 }
                withClue("no card drawn (it cast the creature, so hand is one smaller)") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }

        context("Gate.MayPay over a dynamic RemoveCounters cost") {

            fun charge(game: TestGame): Int {
                val id = game.findPermanent("Counter Spender")!!
                return game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0
            }

            test("the full amount on the permanent offers the payment and removes exactly that many") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Counter Spender")
                    .withCardInHand(1, "Target Dummy") // X = 1 once the Spender is cast
                    .withCardInLibrary(1, "Target Dummy")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Counter Spender").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("removed X = 1 of its 2 charge counters") { charge(game) shouldBe 1 }
                withClue("drew a card") { game.handSize(1) shouldBe 2 }
            }

            test("fewer counters than X is unpayable: no prompt, nothing removed, no payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Counter Spender")
                    .withCardInHand(1, "Target Dummy")
                    .withCardInHand(1, "Target Dummy")
                    .withCardInHand(1, "Target Dummy") // X = 3 > 2 counters
                    .withCardInLibrary(1, "Target Dummy")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Counter Spender").error shouldBe null
                game.resolveStack()

                withClue("no yes/no prompt for a partial removal") { game.hasPendingDecision() shouldBe false }
                withClue("both charge counters stay") { charge(game) shouldBe 2 }
                withClue("no card drawn") { game.handSize(1) shouldBe 3 }
            }
        }

        context("canonical order: targets lock before the gate decision") {

            test("target is chosen at trigger time, then the may-pay decision resolves it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gated Slayer")
                    .withCardOnBattlefield(2, "Target Dummy")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dummy = game.findPermanent("Target Dummy")!!

                game.castSpell(1, "Gated Slayer").error shouldBe null
                game.resolveStack()

                // 1. Targets are locked FIRST — before any may-pay prompt.
                withClue("targets are chosen at trigger time, before the gate") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(dummy))
                game.resolveStack()

                // 2. Only now is the may-pay gate offered, at resolution.
                withClue("the gate decision comes after the target is locked") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("paid 2 life") { game.getLifeTotal(1) shouldBe 18 }
                withClue("the pre-locked target was destroyed") {
                    game.isInGraveyard(2, "Target Dummy") shouldBe true
                }
            }

            test("declining the gate leaves the locked target unharmed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gated Slayer")
                    .withCardOnBattlefield(2, "Target Dummy")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dummy = game.findPermanent("Target Dummy")!!

                game.castSpell(1, "Gated Slayer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(dummy))
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("no life paid") { game.getLifeTotal(1) shouldBe 20 }
                withClue("the target survives") {
                    game.isInGraveyard(2, "Target Dummy") shouldBe false
                    game.isOnBattlefield("Target Dummy") shouldBe true
                }
            }
        }

        context("Gate.MayDecide (pure yes/no)") {

            test("yes runs the effect, no skips it") {
                fun run(choice: Boolean): Pair<Int, Int> {
                    val game = scenario()
                        .withPlayers("Player1", "Player2")
                        .withCardInHand(1, "Gated Seer")
                        .withCardInLibrary(1, "Target Dummy")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    game.castSpell(1, "Gated Seer").error shouldBe null
                    game.resolveStack()
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                    val handBefore = game.handSize(1)
                    game.answerYesNo(choice)
                    game.resolveStack()
                    return handBefore to game.handSize(1)
                }

                val (yesBefore, yesAfter) = run(true)
                withClue("yes draws a card") { yesAfter shouldBe yesBefore + 1 }

                val (noBefore, noAfter) = run(false)
                withClue("no draws nothing") { noAfter shouldBe noBefore }
            }
        }

        context("Effects.May facade (lowered to Gate.MayDecide)") {

            test("the facade resolves a yes/no end-to-end through the gated frame") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maybe Seer")
                    .withCardInLibrary(1, "Target Dummy")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maybe Seer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                val handBefore = game.handSize(1)
                game.answerYesNo(true)
                game.resolveStack()

                withClue("yes draws a card") { game.handSize(1) shouldBe handBefore + 1 }
            }

            test("sourceRequiredZone skips the prompt when the source isn't in the required zone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graveyard-Gated Seer")
                    .withCardInLibrary(1, "Target Dummy")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Graveyard-Gated Seer").error shouldBe null
                game.resolveStack()

                withClue("source is on the battlefield, not the graveyard, so no may prompt") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("nothing drawn (hand is one smaller from casting the creature)") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }

        context("Gate.DoAction (Effects.IfYouDo lowering)") {

            test("the action accomplishing its work runs `then`, not `otherwise`") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Outcome Seer")
                    .withCardInLibrary(1, "Target Dummy") // milled
                    .withCardInLibrary(1, "Target Dummy") // drawn
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Outcome Seer").error shouldBe null
                game.resolveStack()

                withClue("no decision — DoAction is an outcome gate, not a yes/no") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the mill grew the graveyard, so `then` drew a card") {
                    // hand = handBefore - 1 (cast Outcome Seer) + 1 (drawn by `then`)
                    game.handSize(1) shouldBe handBefore
                }
                withClue("`otherwise` did not run") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("the action doing nothing runs `otherwise`, not `then`") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Outcome Seer")
                    // empty library: the mill moves nothing, so the graveyard never grows
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Outcome Seer").error shouldBe null
                game.resolveStack()

                withClue("the mill found nothing, so `otherwise` ran") { game.getLifeTotal(1) shouldBe 19 }
            }

            test("the Effects.IfYouDo facade lowers and branches the same way") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Outcome Drawer")
                    .withCardInLibrary(1, "Target Dummy") // milled
                    .withCardInLibrary(1, "Target Dummy") // drawn
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Outcome Drawer").error shouldBe null
                game.resolveStack()

                withClue("the facade ran `then` (drew a card) on a successful mill") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("`otherwise` did not run") { game.getLifeTotal(1) shouldBe 20 }
            }
        }
    }
}
