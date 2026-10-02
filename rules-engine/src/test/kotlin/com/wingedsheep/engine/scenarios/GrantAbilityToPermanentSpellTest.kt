package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

/**
 * "When you cast this spell, … it gains '<triggered ability>'" — a grant made to a permanent spell
 * on the stack. CR 400.7a: an effect from a triggered ability that changes the characteristics of a
 * permanent spell continues to apply to the permanent that spell becomes. Any other way off the
 * stack makes a new object (CR 400.7), so the grant ends there.
 */
class GrantAbilityToPermanentSpellTest : ScenarioTestBase() {
    init {
        fun gainsLeavesAbility(name: String, typeLine: String) = card(name) {
            manaCost = "{U}"
            this.typeLine = typeLine
            if (typeLine.startsWith("Creature")) {
                power = 2
                toughness = 2
            }
            triggeredAbility {
                trigger = Triggers.self.isCast()
                effect = Effects.GrantTriggeredAbility(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.self.leaves(),
                        effect = Effects.GainLife(3),
                        descriptionOverride = "When this creature leaves the battlefield, you gain 3 life."
                    ),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
            }
        }
        val golem = "Gifted Golem"
        val insight = "Gifted Insight"
        cardRegistry.register(gainsLeavesAbility(golem, "Creature — Golem"))
        cardRegistry.register(gainsLeavesAbility(insight, "Instant"))

        fun board() = scenario().withPlayers("Player1", "Player2")
            .withCardInHand(1, golem)
            .withCardInHand(1, insight)
            .withCardInHand(1, "Counterspell")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Island", 4)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun TestGame.grantsOn(name: String): List<GrantedTriggeredAbility> {
            val ids = (state.stack + state.getBattlefield()).filter {
                state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name
            }.toSet()
            return state.grantedTriggeredAbilities.filter { it.entityId in ids }
        }

        /** Resolve only the cast trigger, leaving the spell itself on the stack. */
        fun TestGame.resolveTopOnly() {
            passPriority().error shouldBe null
            passPriority().error shouldBe null
        }

        test("the grant made to the creature spell is still on the permanent it becomes") {
            val game = board()
            game.castSpell(1, golem).error shouldBe null
            game.resolveTopOnly()
            game.grantsOn(golem).size shouldBe 1

            game.resolveStack()
            game.isOnBattlefield(golem) shouldBe true
            game.grantsOn(golem).size shouldBe 1

            game.castSpell(1, "Lightning Bolt", game.findPermanent(golem)!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, golem) shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }

        test("a permanent spell countered after the grant loses it with the stack object") {
            val game = board()
            game.castSpell(1, golem).error shouldBe null
            game.resolveTopOnly()
            game.grantsOn(golem).size shouldBe 1

            game.castSpellTargetingStackSpell(1, "Counterspell", golem).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, golem) shouldBe true
            game.state.grantedTriggeredAbilities shouldBe emptyList()
        }

        test("an instant spell is not a permanent spell and receives no grant") {
            val game = board()
            game.castSpell(1, insight).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, insight) shouldBe true
            game.state.grantedTriggeredAbilities shouldBe emptyList()
        }

        test("casting a card drops a grant left over from its earlier object") {
            val game = board()
            val card = game.findCardsInHand(1, golem).single()
            val stale = GrantedTriggeredAbility(
                entityId = card,
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.leaves(),
                    effect = Effects.GainLife(10),
                    id = com.wingedsheep.sdk.scripting.AbilityId("stale-grant")
                ),
                duration = Duration.Permanent
            )
            game.state = game.state.copy(grantedTriggeredAbilities = listOf(stale))

            game.castSpell(1, golem).error shouldBe null
            game.state.grantedTriggeredAbilities.none { it.ability.id == stale.ability.id } shouldBe true
        }
    }
}
