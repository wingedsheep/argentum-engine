package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.withCount
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * A card declares a target by what it may be — `target(TargetFilter.Creature)` — and never names
 * it: the DSL mints the binding id, and the prompt the player sees is derived from the requirement.
 * These cases pin both halves, plus the one sequencing operator, `then`.
 */
class TargetDeclarationsTest : DescribeSpec({

    describe("binding ids are minted unique across the card") {
        it("each target gets the next positional id, and effects read it through the handle") {
            val bolt = card("Twin Bolt") {
                manaCost = "{1}{R}"
                typeLine = "Instant"
                spell {
                    val first = target(Targets.Any)
                    val second = target(TargetFilter.Creature, optional = true)
                    effect = Effects.DealDamage(1, first) then Effects.DealDamage(1, second)
                }
            }
            val requirements = bolt.script.targetRequirements
            requirements.map { it.id } shouldBe listOf("t0", "t1")
            val sequence = bolt.script.spellEffect as CompositeEffect
            sequence.effects.map { (it as com.wingedsheep.sdk.scripting.effects.DealDamageEffect).target } shouldBe
                listOf(EffectTarget.BoundVariable("t0"), EffectTarget.BoundVariable("t1"))
        }

        it("a nested block's targets never reuse the enclosing ability's ids") {
            lateinit var outer: EffectTarget.BoundVariable
            lateinit var inner: EffectTarget.BoundVariable
            card("Nested") {
                manaCost = "{R}"
                typeLine = "Instant"
                spell {
                    outer = target(TargetFilter.Creature)
                    effect = Effects.Tap(outer) then ModalEffect.chooseOne(
                        mode("Destroy target artifact.") {
                            inner = target(TargetFilter.Artifact)
                            effect = Effects.Destroy(inner)
                        },
                    )
                }
            }
            outer shouldBe EffectTarget.BoundVariable("t0")
            inner shouldBe EffectTarget.BoundVariable("t1")
        }

        it("targets() hands out one handle per slot") {
            lateinit var handles: List<EffectTarget.BoundVariable>
            card("Two Shots") {
                manaCost = "{R}"
                typeLine = "Instant"
                spell {
                    handles = targets(TargetFilter.Creature, count = 2)
                    effect = Effects.DealDamage(1, handles[0]) then Effects.DealDamage(1, handles[1])
                }
            }
            handles shouldBe listOf(EffectTarget.BoundVariable("t0[0]"), EffectTarget.BoundVariable("t0[1]"))
        }

        it("targets(exactly = X) is the exact dynamic count: floor and cap both X, no static floor") {
            val definition = card("Exact Tap") {
                manaCost = "{X}{U}"
                typeLine = "Sorcery"
                spell {
                    targets(TargetFilter.Creature, exactly = DynamicAmount.XValue)
                    effect = Effects.TapEachTarget()
                }
            }
            val requirement = definition.script.targetRequirements.single() as TargetObject
            requirement.dynamicMaxCount shouldBe DynamicAmount.XValue
            requirement.dynamicMinCount shouldBe DynamicAmount.XValue
            requirement.exactDynamicCount shouldBe true
            requirement.optional shouldBe false
            // X may be 0 (zero targets), so until X is read the static floor is 0.
            requirement.effectiveMinCount shouldBe 0
            requirement.requiresExactlyOneTarget shouldBe false
        }

        it("targets(exactly = …) refuses to be combined with an up-to spelling") {
            io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                card("Confused Tap") {
                    manaCost = "{X}{U}"
                    typeLine = "Sorcery"
                    spell {
                        targets(TargetFilter.Creature, optional = true, exactly = DynamicAmount.XValue)
                        effect = Effects.TapEachTarget()
                    }
                }
            }
        }

        it("withCount settles an exact dynamic count into the static exact shape (CR 601.2c)") {
            val x = DynamicAmount.XValue
            val bound = TargetObject(filter = TargetFilter.Creature, dynamicMaxCount = x, dynamicMinCount = x)
                .withCount(3) as TargetObject
            bound.count shouldBe 3
            bound.minCount shouldBe 3
            bound.effectiveMinCount shouldBe 3
            bound.dynamicMinCount shouldBe null
            bound.dynamicMaxCount shouldBe null
        }
    }

    describe("the targeting prompt is derived from the requirement") {
        fun prompt(filter: TargetFilter) = TargetObject(filter = filter).description

        it("puts the controller after the noun and qualifiers after the controller") {
            prompt(TargetFilter.CreatureYouControl) shouldBe "target creature you control"
            prompt(TargetFilter.CreatureOpponentControls) shouldBe "target creature an opponent controls"
            prompt(TargetFilter.CreatureYouControl.powerAtMost(2)) shouldBe
                "target creature you control with power 2 or less"
            prompt(TargetFilter.Creature.withKeyword(Keyword.FLYING)) shouldBe "target creature with flying"
        }

        it("reads adjectives ahead of the noun, including a heterogeneous or") {
            prompt(TargetFilter.TappedCreature) shouldBe "target tapped creature"
            prompt(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature.tapped())) shouldBe
                "target artifact or tapped creature"
            prompt(TargetFilter.NonlandPermanent) shouldBe "target nonland permanent"
            prompt(TargetFilter(GameObjectFilter.NonbasicLand)) shouldBe "target nonbasic land"
        }

        it("names the zone: a card in a graveyard, a spell on the stack") {
            prompt(TargetFilter.CreatureInYourGraveyard) shouldBe "target creature card in your graveyard"
            prompt(TargetFilter.CardInGraveyard) shouldBe "target card in a graveyard"
            prompt(TargetFilter.SpellOnStack) shouldBe "target spell"
            prompt(TargetFilter.NoncreatureSpellOnStack) shouldBe "target noncreature spell"
            prompt(TargetFilter.ActivatedOrTriggeredAbilityOnStack) shouldBe "target activated or triggered ability"
        }

        it("lets a subtype name the object on its own, except a creature type") {
            prompt(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl())) shouldBe
                "target Vampire you control"
            prompt(TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN))) shouldBe
                "target Goblin creature"
        }

        it("says another / other for a filter that excludes its source") {
            prompt(TargetFilter.OtherCreatureYouControl) shouldBe "another target creature you control"
            TargetObject(filter = TargetFilter.OtherCreature, optional = true).description shouldBe
                "up to one other target creature"
            TargetOther(Targets.Any).description shouldBe "any other target"
        }

        it("spells the count the way Oracle text does") {
            TargetObject(filter = TargetFilter.Creature, count = 2).description shouldBe "two target creatures"
            TargetObject(filter = TargetFilter.Creature, count = 2, optional = true).description shouldBe
                "up to two target creatures"
            TargetObject(filter = TargetFilter.Creature, count = 2, minCount = 1).description shouldBe
                "one or two target creatures"
            TargetObject(filter = TargetFilter.Creature, count = 3, minCount = 1).description shouldBe
                "one, two, or three target creatures"
            TargetObject(filter = TargetFilter.CreatureYouControl, unlimited = true).description shouldBe
                "any number of target creatures you control"
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard, count = 2, optional = true).description shouldBe
                "up to two target creature cards in your graveyard"
            TargetObject(filter = TargetFilter.Creature, optional = true, dynamicMaxCount = DynamicAmount.XValue)
                .description shouldBe "up to X target creatures"
            TargetObject(filter = TargetFilter.Creature, dynamicMaxCount = DynamicAmount.XValue, dynamicMinCount = DynamicAmount.XValue)
                .description shouldBe "X target creatures"
            TargetObject(filter = TargetFilter(GameObjectFilter.Any, zone = Zone.GRAVEYARD), count = 2, optional = true, sameOwner = true)
                .description shouldBe "up to two target cards from a single graveyard"
            TargetObject(
                filter = TargetFilter.CreatureOpponentControls,
                optional = true,
                dynamicMaxCount = DynamicAmount.PlayerCount(Player.EachOpponent),
                differentControllers = true,
            ).description shouldBe "up to one target creature per opponent"
        }
    }

    describe("then is the one sequencing operator") {
        it("a chain builds one flat sequence") {
            val chain = Effects.DrawCards(1) then Effects.GainLife(1) then Effects.LoseLife(1)
            chain.effects.size shouldBe 3
        }

        it("keeps a described sequence whole instead of dropping its override") {
            val described = Effects.Composite(
                listOf(Effects.DrawCards(1), Effects.GainLife(1)),
                descriptionOverride = "Draw a card and gain 1 life.",
            )
            val chain = described then Effects.LoseLife(1)
            chain.effects.size shouldBe 2
            chain.effects.first() shouldBe described
        }

        it("Nothing is the empty sequence") {
            Effects.Nothing shouldBe CompositeEffect(emptyList())
        }
    }
})
