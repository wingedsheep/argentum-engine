package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the Card Definition DSL.
 *
 * These tests verify that we can define cards using the fluent DSL syntax
 * described in card-definition-guide.md.
 */
class CardDslTest : DescribeSpec({

    describe("Basic Lands") {

        it("should define Forest with implicit mana ability") {
            val forest = card("Forest") {
                typeLine = "Basic Land — Forest"

                metadata {
                    rarity = Rarity.COMMON
                    collectorNumber = "289"
                }
            }

            forest.name shouldBe "Forest"
            forest.typeLine.isLand shouldBe true
            forest.typeLine.supertypes shouldContain Supertype.BASIC
            forest.typeLine.subtypes shouldContain Subtype.FOREST
            forest.metadata.rarity shouldBe Rarity.COMMON
            forest.metadata.collectorNumber shouldBe "289"
        }
    }

    describe("Vanilla Creatures") {

        it("should define Grizzly Bears") {
            val bears = card("Grizzly Bears") {
                manaCost = "{1}{G}"
                typeLine = "Creature — Bear"
                power = 2
                toughness = 2

                metadata {
                    flavorText = "We cannot go to the woods today..."
                }
            }

            bears.name shouldBe "Grizzly Bears"
            bears.manaCost.toString() shouldBe "{1}{G}"
            bears.cmc shouldBe 2
            bears.typeLine.isCreature shouldBe true
            bears.typeLine.subtypes shouldContain Subtype.BEAR
            bears.creatureStats shouldNotBe null
            bears.creatureStats!!.basePower shouldBe 2
            bears.creatureStats!!.baseToughness shouldBe 2
            bears.metadata.flavorText shouldBe "We cannot go to the woods today..."
        }
    }

    describe("Creatures with Keywords") {

        it("should define Serra Angel with flying and vigilance") {
            val angel = card("Serra Angel") {
                manaCost = "{3}{W}{W}"
                typeLine = "Creature — Angel"
                power = 4
                toughness = 4
                keywords(Keyword.FLYING, Keyword.VIGILANCE)
            }

            angel.name shouldBe "Serra Angel"
            angel.cmc shouldBe 5
            angel.keywords shouldContain Keyword.FLYING
            angel.keywords shouldContain Keyword.VIGILANCE
            angel.creatureStats?.basePower shouldBe 4
        }
    }

    describe("Creatures with Parameterized Keywords") {

        it("should define a creature with Ward mana cost") {
            val creature = card("Nimble Seafarer") {
                manaCost = "{2}{U}"
                typeLine = "Creature — Human Pirate"
                power = 2
                toughness = 2
                keywords(Keyword.FLASH)
                keywordAbility(KeywordAbility.WardMana(ManaCost.parse("{2}")))
            }

            creature.name shouldBe "Nimble Seafarer"
            creature.keywords shouldContain Keyword.FLASH
            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.WardMana>()
            (creature.keywordAbilities[0] as KeywordAbility.WardMana).cost.cmc shouldBe 2
            creature.keywordAbilities[0].description shouldBe "Ward {2}"
        }

        it("should define a creature with Protection from color") {
            val creature = card("Kor Firewalker") {
                manaCost = "{W}{W}"
                typeLine = "Creature — Kor Soldier"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.ProtectionFromColor(Color.RED))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.ProtectionFromColor>()
            (creature.keywordAbilities[0] as KeywordAbility.ProtectionFromColor).color shouldBe Color.RED
            creature.keywordAbilities[0].description shouldBe "Protection from red"
        }

        it("should define a creature with Protection from multiple colors") {
            val creature = card("Veil of Darkness") {
                manaCost = "{1}{B}{B}"
                typeLine = "Creature — Spirit"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.ProtectionFromColors(setOf(Color.WHITE, Color.GREEN)))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.ProtectionFromColors>()
            val protection = creature.keywordAbilities[0] as KeywordAbility.ProtectionFromColors
            protection.colors shouldContain Color.WHITE
            protection.colors shouldContain Color.GREEN
        }

        it("should define a creature with Ward discard") {
            val creature = card("Mind Flayer") {
                manaCost = "{3}{U}{U}"
                typeLine = "Creature — Horror"
                power = 3
                toughness = 3
                keywordAbility(KeywordAbility.WardDiscard(count = 1, random = true))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.WardDiscard>()
            val ward = creature.keywordAbilities[0] as KeywordAbility.WardDiscard
            ward.count shouldBe 1
            ward.random shouldBe true
            ward.description shouldBe "Ward—Discard a card at random"
        }

        it("should define a creature with Annihilator") {
            val creature = card("Eldrazi Titan") {
                manaCost = "{10}"
                typeLine = "Creature — Eldrazi"
                power = 10
                toughness = 10
                keywordAbility(KeywordAbility.Annihilator(6))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Annihilator>()
            (creature.keywordAbilities[0] as KeywordAbility.Annihilator).count shouldBe 6
            creature.keywordAbilities[0].description shouldBe "Annihilator 6"
        }

        it("should define a creature with multiple parameterized keywords") {
            val creature = card("Resilient Guardian") {
                manaCost = "{3}{W}{U}"
                typeLine = "Creature — Human Knight"
                power = 3
                toughness = 4
                keywords(Keyword.FLYING, Keyword.VIGILANCE)
                keywordAbilities(
                    KeywordAbility.WardMana(ManaCost.parse("{3}")),
                    KeywordAbility.ProtectionFromColor(Color.BLACK)
                )
            }

            creature.keywords shouldContain Keyword.FLYING
            creature.keywords shouldContain Keyword.VIGILANCE
            creature.keywordAbilities shouldHaveSize 2
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.WardMana>()
            creature.keywordAbilities[1].shouldBeInstanceOf<KeywordAbility.ProtectionFromColor>()
        }

        it("should define a creature with Bushido") {
            val creature = card("Samurai Warrior") {
                manaCost = "{2}{W}"
                typeLine = "Creature — Human Samurai"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Bushido(2))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Bushido>()
            (creature.keywordAbilities[0] as KeywordAbility.Bushido).count shouldBe 2
            creature.keywordAbilities[0].description shouldBe "Bushido 2"
        }

        it("should define an artifact creature with Modular") {
            val creature = card("Arcbound Worker") {
                manaCost = "{1}"
                typeLine = "Artifact Creature — Construct"
                power = 0
                toughness = 0
                keywordAbility(KeywordAbility.Modular(1))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Modular>()
            (creature.keywordAbilities[0] as KeywordAbility.Modular).count shouldBe 1
            creature.keywordAbilities[0].description shouldBe "Modular 1"
        }

        it("should define a creature with Afflict") {
            val creature = card("Ruthless Sniper") {
                manaCost = "{2}{B}"
                typeLine = "Creature — Human Assassin"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Afflict(3))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Afflict>()
            (creature.keywordAbilities[0] as KeywordAbility.Afflict).count shouldBe 3
            creature.keywordAbilities[0].description shouldBe "Afflict 3"
        }

        it("should define a vehicle with Crew") {
            val vehicle = card("Heart of Kiran") {
                manaCost = "{2}"
                typeLine = "Legendary Artifact — Vehicle"
                power = 4
                toughness = 4
                keywords(Keyword.FLYING, Keyword.VIGILANCE)
                keywordAbility(KeywordAbility.Crew(3))
            }

            vehicle.typeLine.subtypes shouldContain Subtype.VEHICLE
            vehicle.keywordAbilities shouldHaveSize 1
            vehicle.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Crew>()
            (vehicle.keywordAbilities[0] as KeywordAbility.Crew).power shouldBe 3
            vehicle.keywordAbilities[0].description shouldBe "Crew 3"
        }

        it("should define a card with Cycling") {
            val card = card("Street Wraith") {
                manaCost = "{3}{B}{B}"
                typeLine = "Creature — Wraith"
                power = 3
                toughness = 4
                keywords(Keyword.SWAMPWALK)
                keywordAbility(KeywordAbility.Cycling(ManaCost.parse("{B/P}")))
            }

            card.keywordAbilities shouldHaveSize 1
            card.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Cycling>()
        }
    }

    describe("Creatures with Triggered Abilities") {

        it("should define Flametongue Kavu with ETB trigger") {
            val kavu = card("Flametongue Kavu") {
                manaCost = "{3}{R}"
                typeLine = "Creature — Kavu"
                power = 4
                toughness = 2

                triggeredAbility {
                    trigger = Triggers.EntersBattlefield
                    effect = Effects.DealDamage(4, EffectTarget.TargetCreature)
                    target = Targets.Creature
                }
            }

            kavu.name shouldBe "Flametongue Kavu"
            kavu.triggeredAbilities shouldHaveSize 1

            val ability = kavu.triggeredAbilities.first()
            ability.trigger shouldBe OnEnterBattlefield()
            ability.effect shouldBe DealDamageEffect(4, EffectTarget.TargetCreature)
        }

        it("should define Thragtusk with multiple triggers") {
            val thragtusk = card("Thragtusk") {
                manaCost = "{4}{G}"
                typeLine = "Creature — Beast"
                power = 5
                toughness = 3

                triggeredAbility {
                    trigger = Triggers.EntersBattlefield
                    effect = Effects.GainLife(5)
                }

                triggeredAbility {
                    trigger = Triggers.LeavesBattlefield
                    effect = Effects.CreateToken(power = 3, toughness = 3, creatureTypes = setOf("Beast"))
                }
            }

            thragtusk.triggeredAbilities shouldHaveSize 2
        }
    }

    describe("Instants and Sorceries") {

        it("should define Lightning Bolt with targeted damage") {
            val bolt = card("Lightning Bolt") {
                manaCost = "{R}"
                typeLine = "Instant"

                spell {
                    effect = Effects.DealDamage(3)
                    target = Targets.Any
                }
            }

            bolt.name shouldBe "Lightning Bolt"
            bolt.typeLine.isInstant shouldBe true
            bolt.spellEffect shouldNotBe null
            bolt.targetRequirements shouldHaveSize 1
        }

        it("should define Divination drawing cards") {
            val divination = card("Divination") {
                manaCost = "{2}{U}"
                typeLine = "Sorcery"

                spell {
                    effect = Effects.DrawCards(2)
                }
            }

            divination.typeLine.isSorcery shouldBe true
            divination.spellEffect shouldBe DrawCardsEffect(2, EffectTarget.Controller)
        }
    }

    describe("Artifacts") {

        it("should define Sol Ring with mana ability") {
            val solRing = card("Sol Ring") {
                manaCost = "{1}"
                typeLine = "Artifact"

                activatedAbility {
                    cost = Costs.Tap
                    effect = Effects.AddColorlessMana(2)
                    manaAbility = true
                }
            }

            solRing.typeLine.isArtifact shouldBe true
            solRing.activatedAbilities shouldHaveSize 1

            val ability = solRing.activatedAbilities.first()
            ability.cost shouldBe AbilityCost.Tap
            ability.isManaAbility shouldBe true
        }
    }

    describe("Enchantments") {

        it("should define Glorious Anthem with static ability") {
            val anthem = card("Glorious Anthem") {
                manaCost = "{1}{W}{W}"
                typeLine = "Enchantment"

                staticAbility {
                    effect = Effects.ModifyStats(+1, +1)
                    filter = Filters.CreaturesYouControl
                }
            }

            anthem.typeLine.isEnchantment shouldBe true
            anthem.staticAbilities shouldHaveSize 1
        }

        it("should define Rancor as an Aura with recursion") {
            val rancor = card("Rancor") {
                manaCost = "{G}"
                typeLine = "Enchantment — Aura"

                auraTarget = Targets.Creature

                staticAbility {
                    effect = Effects.Composite(
                        Effects.ModifyStats(+2, +0),
                        Effects.GrantKeyword(Keyword.TRAMPLE)
                    )
                    filter = Filters.EnchantedCreature
                }

                triggeredAbility {
                    trigger = Triggers.PutIntoGraveyardFromBattlefield
                    effect = Effects.ReturnToHand(EffectTarget.Self)
                }
            }

            rancor.typeLine.isAura shouldBe true
            rancor.script.auraTarget shouldNotBe null
            rancor.staticAbilities shouldHaveSize 1
            rancor.triggeredAbilities shouldHaveSize 1
        }
    }

    describe("Equipment") {

        it("should define Loxodon Warhammer with equip ability") {
            val warhammer = card("Loxodon Warhammer") {
                manaCost = "{3}"
                typeLine = "Artifact — Equipment"

                staticAbility {
                    effect = Effects.Composite(
                        Effects.ModifyStats(+3, +0),
                        Effects.GrantKeyword(Keyword.TRAMPLE),
                        Effects.GrantKeyword(Keyword.LIFELINK)
                    )
                    filter = Filters.EquippedCreature
                }

                equipAbility("{3}")
            }

            warhammer.typeLine.isEquipment shouldBe true
            warhammer.equipCost shouldNotBe null
            warhammer.equipCost?.cmc shouldBe 3
        }
    }

    describe("Planeswalkers") {

        it("should define Liliana of the Veil with loyalty abilities") {
            val liliana = card("Liliana of the Veil") {
                manaCost = "{1}{B}{B}"
                typeLine = "Legendary Planeswalker — Liliana"
                startingLoyalty = 3

                loyaltyAbility(+1) {
                    effect = Effects.Discard(1)
                    target = Targets.AllPlayers
                }

                loyaltyAbility(-2) {
                    effect = Effects.Sacrifice(Filters.Creature)
                    target = Targets.Player
                }

                loyaltyAbility(-6) {
                    effect = Effects.SeparatePermanentsIntoPiles()
                    target = Targets.Player
                }
            }

            liliana.isPlaneswalker shouldBe true
            liliana.startingLoyalty shouldBe 3
            liliana.typeLine.supertypes shouldContain Supertype.LEGENDARY
            liliana.activatedAbilities shouldHaveSize 3
        }
    }

    describe("Advanced Sequencing") {

        it("should define Compulsive Research with composite effect") {
            val research = card("Compulsive Research") {
                manaCost = "{2}{U}"
                typeLine = "Sorcery"

                spell {
                    effect = Effects.Composite(
                        Effects.DrawCards(3),
                        Effects.Discard(2)
                    )
                }
            }

            research.spellEffect shouldNotBe null
            val compositeEffect = research.spellEffect as CompositeEffect
            compositeEffect.effects shouldHaveSize 2
        }
    }

    describe("Conditional Spells") {

        it("should define Gift of Estates with condition") {
            val gift = card("Gift of Estates") {
                manaCost = "{1}{W}"
                typeLine = "Sorcery"

                spell {
                    condition = Conditions.OpponentControlsMoreLands
                    effect = Effects.SearchLibrary(
                        filter = Filters.PlainsCard,
                        count = 3,
                        destination = SearchDestination.HAND
                    )
                }
            }

            gift.spellEffect shouldNotBe null
        }
    }
})
