package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.EachOpponentDiscardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.triggers.OnOtherCreatureEnters
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that card definitions can be serialized to JSON and deserialized back
 * using the @SerialName annotations for polymorphic type discrimination.
 */
class CardSerializationRoundTripTest : DescribeSpec({

    val json = CardSerialization.json

    describe("Effect round-trip serialization") {

        it("should round-trip a simple damage spell") {
            val card = card("Lightning Bolt") {
                manaCost = "{R}"
                typeLine = "Instant"
                spell {
                    target = Targets.CreatureOrPlayer
                    effect = DealDamageEffect(
                        amount = DynamicAmount.Fixed(3),
                        target = EffectTarget.ContextTarget(0)
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "\"type\""
            serialized shouldContain "DealDamage"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.name shouldBe "Lightning Bolt"
            deserialized.script.spellEffect.shouldBeInstanceOf<DealDamageEffect>()
        }

        it("should round-trip a creature with triggered ability") {
            val card = card("Siege-Gang Commander") {
                manaCost = "{3}{R}{R}"
                typeLine = "Creature — Goblin"
                power = 2
                toughness = 2

                triggeredAbility {
                    trigger = Triggers.EntersBattlefield
                    effect = CreateTokenEffect(
                        name = "Goblin",
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Goblin"),
                        count = 3
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "EntersBattlefield"
            serialized shouldContain "CreateToken"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.name shouldBe "Siege-Gang Commander"
            deserialized.script.triggeredAbilities.size shouldBe 1
            deserialized.script.triggeredAbilities[0].effect.shouldBeInstanceOf<CreateTokenEffect>()
        }

        it("should round-trip a composite effect") {
            val card = card("Syphon Mind") {
                manaCost = "{3}{B}"
                typeLine = "Sorcery"
                spell {
                    effect = EachOpponentDiscardsEffect(count = 1) then
                            DrawCardsEffect(
                                count = DynamicAmount.Fixed(1),
                                target = EffectTarget.Controller
                            )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "Composite"
            serialized shouldContain "EachOpponentDiscards"
            serialized shouldContain "DrawCards"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.script.spellEffect.shouldBeInstanceOf<CompositeEffect>()
            val composite = deserialized.script.spellEffect as CompositeEffect
            composite.effects.size shouldBe 2
        }

        it("should round-trip a creature with upkeep trigger") {
            val card = card("Wretched Anurid") {
                manaCost = "{1}{B}"
                typeLine = "Creature — Zombie Frog Beast"
                power = 3
                toughness = 3

                triggeredAbility {
                    trigger = OnOtherCreatureEnters(youControlOnly = false)
                    effect = LoseLifeEffect(
                        amount = DynamicAmount.Fixed(1),
                        target = EffectTarget.Controller
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            val deserialized = CardLoader.fromJson(serialized)
            deserialized.name shouldBe "Wretched Anurid"
            deserialized.script.triggeredAbilities[0].trigger.shouldBeInstanceOf<OnOtherCreatureEnters>()
        }
    }

    describe("Predicate round-trip serialization") {

        it("should round-trip card predicates") {
            val filter = GameObjectFilter.Creature
                .withColor(Color.BLACK)
                .tapped()
                .youControl()

            val card = card("Test Card") {
                manaCost = "{B}"
                typeLine = "Sorcery"
                spell {
                    effect = ForEachInGroupEffect(
                        filter = GroupFilter(filter),
                        effect = MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "HasColor"
            serialized shouldContain "IsTapped"
            serialized shouldContain "ControlledByYou"

            val deserialized = CardLoader.fromJson(serialized)
            val effect = deserialized.script.spellEffect as ForEachInGroupEffect
            effect.filter.baseFilter.cardPredicates.any { it is CardPredicate.HasColor } shouldBe true
            effect.filter.baseFilter.statePredicates.any { it is StatePredicate.IsTapped } shouldBe true
            effect.filter.baseFilter.controllerPredicate shouldBe ControllerPredicate.ControlledByYou
        }
    }

    describe("KeywordAbility round-trip serialization") {

        it("should round-trip keyword abilities") {
            val card = card("Morphling") {
                manaCost = "{3}{U}{U}"
                typeLine = "Creature — Shapeshifter"
                power = 3
                toughness = 3
                keywordAbility(KeywordAbility.Simple(Keyword.FLYING))
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "Simple"
            serialized shouldContain "FLYING"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.keywordAbilities.size shouldBe 1
            deserialized.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Simple>()
        }

        it("should round-trip morph keyword") {
            val card = card("Blistering Firecat") {
                manaCost = "{1}{R}{R}{R}"
                typeLine = "Creature — Elemental Cat"
                power = 7
                toughness = 1
                keywordAbility(KeywordAbility.Simple(Keyword.TRAMPLE))
                keywordAbility(KeywordAbility.Simple(Keyword.HASTE))
                keywordAbility(KeywordAbility.Morph(ManaCost.parse("{R}{R}")))
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "Morph"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.keywordAbilities.any { it is KeywordAbility.Morph } shouldBe true
        }
    }

    describe("Duration round-trip serialization") {

        it("should round-trip duration types") {
            val card = card("Threaten") {
                manaCost = "{2}{R}"
                typeLine = "Sorcery"
                spell {
                    target = Targets.Creature
                    effect = GainControlEffect(
                        target = EffectTarget.ContextTarget(0),
                        duration = Duration.EndOfTurn
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "EndOfTurn"

            val deserialized = CardLoader.fromJson(serialized)
            val effect = deserialized.script.spellEffect as GainControlEffect
            effect.duration shouldBe Duration.EndOfTurn
        }
    }

    describe("Static ability round-trip serialization") {

        it("should round-trip static abilities") {
            val card = card("Glorious Anthem") {
                manaCost = "{1}{W}{W}"
                typeLine = "Enchantment"
                staticAbility {
                    ability = ModifyStatsForCreatureGroup(
                        powerBonus = 1,
                        toughnessBonus = 1,
                        filter = GroupFilter.AllCreaturesYouControl
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "ModifyStatsForCreatureGroup"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.script.staticAbilities.size shouldBe 1
            deserialized.script.staticAbilities[0].shouldBeInstanceOf<ModifyStatsForCreatureGroup>()
        }
    }

    describe("Replacement effect round-trip serialization") {

        it("should round-trip replacement effects") {
            val card = card("Doubling Season") {
                manaCost = "{4}{G}"
                typeLine = "Enchantment"
                replacementEffect(DoubleTokenCreation())
                replacementEffect(DoubleCounterPlacement())
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "DoubleTokenCreation"
            serialized shouldContain "DoubleCounterPlacement"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.script.replacementEffects.size shouldBe 2
        }
    }

    describe("Condition round-trip serialization") {

        it("should round-trip activated ability costs") {
            val card = card("Ancestor's Prophet") {
                manaCost = "{4}{W}"
                typeLine = "Creature — Human Cleric"
                power = 1
                toughness = 5

                activatedAbility {
                    cost = AbilityCost.TapPermanents(
                        count = 5,
                        filter = GameObjectFilter.Creature.withSubtype("Cleric")
                    )
                    effect = GainLifeEffect(
                        amount = DynamicAmount.Fixed(10),
                        target = EffectTarget.Controller
                    )
                }
            }

            val serialized = CardLoader.toJson(card)
            serialized shouldContain "CostTapPermanents"
            serialized shouldContain "GainLife"

            val deserialized = CardLoader.fromJson(serialized)
            deserialized.script.activatedAbilities.size shouldBe 1
        }
    }
})
