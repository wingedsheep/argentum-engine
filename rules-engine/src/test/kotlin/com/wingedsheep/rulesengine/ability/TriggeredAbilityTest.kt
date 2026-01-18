package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TriggeredAbilityTest : FunSpec({

    context("TriggeredAbility creation") {
        test("creates ability with trigger and effect") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = GainLifeEffect(amount = 2, target = EffectTarget.Controller)

            val ability = TriggeredAbility.create(trigger, effect)

            ability.trigger shouldBe trigger
            ability.effect shouldBe effect
            ability.optional shouldBe false
        }

        test("creates optional ability") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller)

            val ability = TriggeredAbility.create(trigger, effect, optional = true)

            ability.optional shouldBe true
        }

        test("generates unique ability IDs") {
            val trigger = OnDeath(selfOnly = true)
            val effect = DealDamageEffect(amount = 2, target = EffectTarget.EachOpponent)

            val ability1 = TriggeredAbility.create(trigger, effect)
            val ability2 = TriggeredAbility.create(trigger, effect)

            (ability1.id != ability2.id) shouldBe true
        }

        test("description combines trigger and effect") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = GainLifeEffect(amount = 3, target = EffectTarget.Controller)

            val ability = TriggeredAbility.create(trigger, effect)

            ability.description shouldContain "When this enters the battlefield"
            ability.description shouldContain "gain 3 life"
        }

        test("optional ability description includes 'you may'") {
            val trigger = OnDeath(selfOnly = true)
            val effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller)

            val ability = TriggeredAbility.create(trigger, effect, optional = true)

            ability.description shouldContain "you may"
        }
    }

    context("PendingTrigger") {
        test("creates pending trigger with context") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = GainLifeEffect(amount = 2, target = EffectTarget.Controller)
            val ability = TriggeredAbility.create(trigger, effect)

            val cardId = CardId("card_123")
            val controllerId = PlayerId.of("player1")

            val pendingTrigger = PendingTrigger(
                ability = ability,
                sourceId = cardId,
                sourceName = "Soul Warden",
                controllerId = controllerId,
                triggerContext = TriggerContext.None
            )

            pendingTrigger.sourceId shouldBe cardId
            pendingTrigger.controllerId shouldBe controllerId
            pendingTrigger.sourceName shouldBe "Soul Warden"
        }

        test("pending trigger description includes source name") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            val ability = TriggeredAbility.create(trigger, effect)

            val pendingTrigger = PendingTrigger(
                ability = ability,
                sourceId = CardId("card_123"),
                sourceName = "Soul Warden",
                controllerId = PlayerId.of("player1"),
                triggerContext = TriggerContext.None
            )

            pendingTrigger.description shouldContain "Soul Warden"
        }
    }

    context("TriggerContext") {
        test("ZoneChange context stores zone information") {
            val context = TriggerContext.ZoneChange(
                cardId = CardId("card_456"),
                cardName = "Grizzly Bears",
                fromZone = "HAND",
                toZone = "BATTLEFIELD"
            )

            context.cardId.value shouldBe "card_456"
            context.cardName shouldBe "Grizzly Bears"
            context.fromZone shouldBe "HAND"
            context.toZone shouldBe "BATTLEFIELD"
        }

        test("DamageDealt context stores damage information") {
            val context = TriggerContext.DamageDealt(
                sourceId = CardId("attacker_123"),
                targetId = "player2",
                amount = 3,
                isPlayer = true,
                isCombat = true
            )

            context.amount shouldBe 3
            context.isPlayer shouldBe true
            context.isCombat shouldBe true
        }

        test("PhaseStep context stores turn information") {
            val context = TriggerContext.PhaseStep(
                phase = "BEGINNING",
                step = "UPKEEP",
                activePlayerId = PlayerId.of("player1")
            )

            context.phase shouldBe "BEGINNING"
            context.step shouldBe "UPKEEP"
        }
    }

    context("StackedTrigger") {
        test("creates stacked trigger from pending trigger") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = GainLifeEffect(amount = 2, target = EffectTarget.Controller)
            val ability = TriggeredAbility.create(trigger, effect)

            val pendingTrigger = PendingTrigger(
                ability = ability,
                sourceId = CardId("card_123"),
                sourceName = "Soul Warden",
                controllerId = PlayerId.of("player1"),
                triggerContext = TriggerContext.None
            )

            val stackedTrigger = StackedTrigger(
                pendingTrigger = pendingTrigger,
                chosenTargets = emptyList()
            )

            stackedTrigger.sourceId shouldBe CardId("card_123")
            stackedTrigger.controllerId shouldBe PlayerId.of("player1")
        }

        test("stacked trigger can have chosen targets") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            val effect = DealDamageEffect(amount = 2, target = EffectTarget.AnyTarget)
            val ability = TriggeredAbility.create(trigger, effect)

            val pendingTrigger = PendingTrigger(
                ability = ability,
                sourceId = CardId("card_123"),
                sourceName = "Flametongue Kavu",
                controllerId = PlayerId.of("player1"),
                triggerContext = TriggerContext.None
            )

            val stackedTrigger = StackedTrigger(
                pendingTrigger = pendingTrigger,
                chosenTargets = listOf(ChosenTarget.CardTarget(CardId("target_456")))
            )

            stackedTrigger.chosenTargets.size shouldBe 1
        }
    }

    context("ChosenTarget") {
        test("PlayerTarget stores player ID") {
            val target = ChosenTarget.PlayerTarget(PlayerId.of("player2"))
            target.playerId.value shouldBe "player2"
        }

        test("CardTarget stores card ID") {
            val target = ChosenTarget.CardTarget(CardId("card_789"))
            target.cardId.value shouldBe "card_789"
        }
    }
})
