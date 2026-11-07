package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A targeted triggered ability gated by an optional *mana* payment — "you may pay {2}. If you do,
 * destroy target creature an opponent controls" (Lightning Rift, Jubilant Mascot, Frenzied Goblin).
 *
 * It is placed like every other targeted trigger: targets are chosen as it goes on the stack
 * (CR 603.3d), and the payment is offered only as it resolves, against those locked targets — so
 * opponents get to respond before any mana is spent, an ability whose only target became illegal
 * doesn't resolve and never asks (CR 608.2b), and an ability with no legal target never goes on the
 * stack. The payment keeps manual mana-source selection.
 */
class OptionalManaPaymentTargetedTriggerTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Mana-Gated Slayer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Assassin")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId("OptionalManaPaymentTargetedTriggerTest_slayer"),
                            trigger = Triggers.self.enters().event,
                            binding = Triggers.self.enters().binding,
                            effect = Effects.MayPay(
                                cost = ManaCost.parse("{2}"),
                                then = Effects.Destroy(EffectTarget.ContextTarget(0))
                            ),
                            targetRequirement = TargetObject(filter = TargetFilter.Creature.opponentControls())
                        )
                    )
                )
            )
        )

        fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true
        fun TestGame.tappedMountains() = findAllPermanents("Mountain").count { isTapped(it) }

        fun game(mountains: Int = 2) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Mana-Gated Slayer")
            .withCardInHand(1, "Lightning Bolt")
            .withCardOnBattlefield(2, "Savannah Lions")
            .apply { if (mountains > 0) withLandsOnBattlefield(1, "Mountain", mountains) }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("targets lock as the trigger goes on the stack; the payment waits for resolution") {

            test("the target is chosen first, then paying {2} at resolution destroys it") {
                val game = game()
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Mana-Gated Slayer").error shouldBe null
                game.resolveStack()

                withClue("the target is chosen before any pay question (CR 603.3d)") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(lions)).error shouldBe null
                withClue("no mana is spent putting the trigger on the stack") { game.tappedMountains() shouldBe 0 }

                game.resolveStack()
                withClue("the pay question comes as the trigger resolves") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true).error shouldBe null
                withClue("paying keeps manual mana-source selection") {
                    game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                }
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                game.tappedMountains() shouldBe 2
                game.isInGraveyard(2, "Savannah Lions") shouldBe true
            }

            test("declining at resolution spends nothing and leaves the locked target alone") {
                val game = game()
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Mana-Gated Slayer").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(lions)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.tappedMountains() shouldBe 0
                game.isOnBattlefield("Savannah Lions") shouldBe true
            }

            test("a target removed in response makes the ability fizzle without asking to pay (CR 608.2b)") {
                val game = game(mountains = 3)
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Mana-Gated Slayer").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(lions)).error shouldBe null

                // With the trigger on the stack, its target dies to a Bolt cast in response.
                game.castSpell(1, "Lightning Bolt", lions).error shouldBe null
                game.resolveStack()

                withClue("the fizzled ability never offers the payment") {
                    game.getPendingDecision() shouldBe null
                }
                game.isInGraveyard(2, "Savannah Lions") shouldBe true
                withClue("only the Bolt's mana was spent") { game.tappedMountains() shouldBe 1 }
            }
        }

        context("when the payment or the target is impossible") {

            test("no legal target: the trigger never goes on the stack and nothing is asked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mana-Gated Slayer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mana-Gated Slayer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
                game.tappedMountains() shouldBe 0
            }

            test("an unaffordable payment still targets, then resolves without a prompt") {
                val game = game(mountains = 1)
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Mana-Gated Slayer").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(lions)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.isOnBattlefield("Savannah Lions") shouldBe true
                game.tappedMountains() shouldBe 0
            }
        }
    }
}
