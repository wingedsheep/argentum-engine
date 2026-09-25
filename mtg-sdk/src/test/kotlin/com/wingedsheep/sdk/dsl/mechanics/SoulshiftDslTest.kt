package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Shape of the soulshift DSL (CR 702.46): keyword + an optional dies trigger per instance. */
class SoulshiftDslTest : DescribeSpec({

    describe("soulshift(n)") {

        it("adds the Numeric keyword and one dies trigger targeting a Spirit card in your graveyard") {
            val def = card("Soulshift Probe") {
                manaCost = "{3}{B}"
                typeLine = "Creature — Spirit"
                power = 2
                toughness = 2
                soulshift(3)
            }

            def.keywordAbilities shouldContainExactly listOf(KeywordAbility.Numeric(Keyword.SOULSHIFT, 3))
            val trigger = def.script.triggeredAbilities.single()
            trigger.trigger shouldBe Triggers.self.dies().event
            val requirement = trigger.targetRequirement.shouldBeInstanceOf<TargetObject>()
            requirement.filter.zone shouldBe Zone.GRAVEYARD
            val predicates = requirement.filter.baseFilter.cardPredicates
            predicates shouldContain CardPredicate.HasSubtype(com.wingedsheep.sdk.core.Subtype("Spirit"))
            predicates shouldContain CardPredicate.ManaValueAtMost(3)
        }

        it("multiple instances trigger separately (CR 702.46b)") {
            val def = card("Double Soulshift Probe") {
                manaCost = "{4}"
                typeLine = "Creature — Spirit"
                power = 1
                toughness = 1
                soulshift(2)
                soulshift(4)
            }

            def.keywordAbilities shouldHaveSize 2
            val triggers = def.script.triggeredAbilities
            triggers shouldHaveSize 2
            triggers[0].id shouldNotBe triggers[1].id
        }
    }
})
