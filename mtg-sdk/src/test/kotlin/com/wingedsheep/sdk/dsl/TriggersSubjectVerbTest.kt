package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * The subject of a trigger fixes its binding, and each verb lowers to the same [TriggerSpec] the
 * engine has always read — the corpus migration to this vocabulary was proven byte-identical by the
 * card snapshots, and these cases pin the lowering itself.
 */
class TriggersSubjectVerbTest : DescribeSpec({

    describe("the subject decides the binding") {
        it("self / attached / a / another") {
            Triggers.self.dies().binding shouldBe TriggerBinding.SELF
            Triggers.attached.dies().binding shouldBe TriggerBinding.ATTACHED
            Triggers.a(GameObjectFilter.Creature).dies().binding shouldBe TriggerBinding.ANY
            Triggers.another(GameObjectFilter.Creature).dies().binding shouldBe TriggerBinding.OTHER
        }

        it("batch and player subjects are ANY") {
            Triggers.oneOrMore(GameObjectFilter.Creature).die().binding shouldBe TriggerBinding.ANY
            Triggers.oneOrMoreOther(GameObjectFilter.Creature).die().binding shouldBe TriggerBinding.ANY
            Triggers.you.draws().binding shouldBe TriggerBinding.ANY
            Triggers.anOpponent.casts().binding shouldBe TriggerBinding.ANY
        }

        it("\"sacrifices another\" is the one player verb bound OTHER") {
            Triggers.you.sacrificesAnother(GameObjectFilter.Creature).binding shouldBe TriggerBinding.OTHER
        }
    }

    describe("verbs lower to the event the engine reads") {
        it("dies is battlefield to graveyard, with the subject filter on the event") {
            Triggers.another(GameObjectFilter.Creature.youControl()).dies() shouldBe TriggerSpec(
                EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl(),
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD,
                ),
                TriggerBinding.OTHER,
            )
        }

        it("an unfiltered subject leaves a nullable event filter null") {
            (Triggers.self.attacks().event as EventPattern.AttackEvent).filter shouldBe null
            (Triggers.a(GameObjectFilter.Creature).attacks().event as EventPattern.AttackEvent).filter shouldBe
                GameObjectFilter.Creature
        }

        it("dealsCombatDamage is dealsDamage with the combat damage type") {
            Triggers.self.dealsCombatDamage(Recipient.AnyPlayer) shouldBe
                Triggers.self.dealsDamage(Recipient.AnyPlayer, DamageType.Combat)
        }

        it("a player's step and a batch's excludeSource") {
            Triggers.you.beginningOf(Step.UPKEEP).event shouldBe EventPattern.StepEvent(Step.UPKEEP, Player.You)
            Triggers.anyPlayer.beginningOf(Step.END).event shouldBe EventPattern.StepEvent(Step.END, Player.Each)
            (Triggers.oneOrMoreOther(GameObjectFilter.Creature).enter().event as EventPattern.PermanentsEnteredEvent)
                .excludeSource shouldBe true
        }

        it("isAttacked picks the you / opponent event") {
            Triggers.you.isAttacked().event shouldBe EventPattern.CreaturesAttackYouEvent(minAttackers = 1)
            Triggers.anOpponent.isAttacked().event shouldBe EventPattern.CreaturesAttackYourOpponentEvent(minAttackers = 1)
        }
    }

    describe("a verb never drops what it can't carry") {
        it("rejects a filtered subject on an event with no filter axis") {
            shouldThrow<IllegalArgumentException> { Triggers.a(GameObjectFilter.Creature).trains() }
            shouldThrow<IllegalArgumentException> { Triggers.a(GameObjectFilter.Creature).crews() }
        }

        it("rejects a subject the verb can't mean") {
            shouldThrow<IllegalArgumentException> { Triggers.a().isCast() }
            shouldThrow<IllegalArgumentException> { Triggers.anyPlayer.attacks() }
            shouldThrow<IllegalArgumentException> { Triggers.self.beginningOf(Step.UPKEEP) }
        }

        it("keeps one spelling for each zone change") {
            shouldThrow<IllegalArgumentException> { Triggers.self.leaves(to = Zone.GRAVEYARD) }
            shouldThrow<IllegalArgumentException> { Triggers.self.changesZone(to = Zone.BATTLEFIELD) }
        }

        it("blocks keeps its SELF-only axes") {
            shouldThrow<IllegalArgumentException> {
                Triggers.a(GameObjectFilter.Creature).blocks(attackerFilter = GameObjectFilter.Creature)
            }
        }
    }
})
