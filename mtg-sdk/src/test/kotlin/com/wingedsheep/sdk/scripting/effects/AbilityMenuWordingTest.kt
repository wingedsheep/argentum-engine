package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * The generated ability text players read in the action menu (`offer.description`). Each case is
 * a shape that once shipped garbled: a flag rendered from its enum name ("gains cant be blocked"),
 * a bound target with no noun ("target gets +1/+1"), and a tap cost that printed "you control"
 * twice.
 */
class AbilityMenuWordingTest : DescribeSpec({

    describe("granting a keyword or flag") {
        val creature = EffectTarget.BoundVariable("t0", "target creature you control")

        it("a keyword is gained, by its display name") {
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature).description shouldBe
                "target creature you control gains first strike until end of turn"
        }

        it("a predicate-shaped flag is the subject's verb, not a gained noun") {
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature).description shouldBe
                "target creature you control can't be blocked until end of turn"
            Effects.GrantKeyword(AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS, creature, Duration.EndOfTurn)
                .description shouldBe
                "target creature you control assigns combat damage equal to its toughness rather than its power until end of turn"
        }

        it("a flag that already says 'this turn' doesn't repeat the duration") {
            Effects.GrantKeyword(AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE, creature).description shouldBe
                "target creature you control assigns no combat damage this turn"
        }

        it("a flag that isn't a predicate is quoted as a granted ability") {
            Effects.GrantKeyword(AbilityFlag.MAY_NOT_UNTAP, creature).description shouldBe
                "target creature you control gains \"You may choose not to untap\" until end of turn"
        }
    }

    describe("a bound target names what it targets") {
        it("an activated ability reads its target requirement, not a bare 'target'") {
            val pump = card("Pump Rock") {
                manaCost = "{2}"
                typeLine = "Artifact"
                activatedAbility {
                    cost = Costs.Tap
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.ModifyStats(1, 1, creature)
                }
            }
            pump.script.activatedAbilities.single().description shouldBe
                "{T}: target creature gets +1/+1 until end of turn"
        }

        it("the label is display only — a handle built from its name alone still equals it") {
            EffectTarget.BoundVariable("t0", "target creature") shouldBe EffectTarget.BoundVariable("t0")
            EffectTarget.BoundVariable("t0").description shouldBe "target"
        }
    }

    describe("a tap-permanents cost") {
        it("names the permanents once, spelled-out and pluralized, with 'other' kept") {
            val halflings = GameObjectFilter.Creature.withSubtype("Halfling").youControl()
            Costs.TapPermanents(2, halflings, excludeSelf = true).description shouldBe
                "Tap two other untapped Halfling creatures you control"
            Costs.TapPermanents(3, GameObjectFilter.Creature).description shouldBe
                "Tap three untapped creatures you control"
            Costs.TapPermanents(1, halflings).description shouldBe
                "Tap an untapped Halfling creature you control"
        }
    }
})
