package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.sdk.dsl.Patterns

/**
 * Tests for the Card Definition DSL.
 *
 * These tests verify that we can define cards using the fluent DSL syntax
 * described in docs/card-sdk-language-reference.md.
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
            bears.creatureStats.baseToughness shouldBe 2
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
                keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
            }

            creature.name shouldBe "Nimble Seafarer"
            creature.keywords shouldContain Keyword.FLASH
            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Ward>()
            val ward = creature.keywordAbilities[0] as KeywordAbility.Ward
            (ward.cost as WardCost.Mana).manaCost shouldBe "{2}"
            creature.keywordAbilities[0].description shouldBe "Ward {2}"
        }

        it("should define a creature with Protection from color") {
            val creature = card("Kor Firewalker") {
                manaCost = "{W}{W}"
                typeLine = "Creature — Kor Soldier"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Protection>()
            val protectionScope = (creature.keywordAbilities[0] as KeywordAbility.Protection).scope
            (protectionScope as ProtectionScope.Color).color shouldBe Color.RED
            creature.keywordAbilities[0].description shouldBe "Protection from red"
        }

        it("should define a creature with Protection from multiple colors") {
            val creature = card("Veil of Darkness") {
                manaCost = "{1}{B}{B}"
                typeLine = "Creature — Spirit"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Protection(ProtectionScope.Colors(setOf(Color.WHITE, Color.GREEN))))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Protection>()
            val protection = (creature.keywordAbilities[0] as KeywordAbility.Protection).scope as ProtectionScope.Colors
            protection.colors shouldContain Color.WHITE
            protection.colors shouldContain Color.GREEN
        }

        it("should define a creature with Ward discard") {
            val creature = card("Mind Flayer") {
                manaCost = "{3}{U}{U}"
                typeLine = "Creature — Horror"
                power = 3
                toughness = 3
                keywordAbility(KeywordAbility.Ward(WardCost.Discard(count = 1, random = true)))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Ward>()
            val ward = creature.keywordAbilities[0] as KeywordAbility.Ward
            val discard = ward.cost as WardCost.Discard
            discard.count shouldBe 1
            discard.random shouldBe true
            ward.description shouldBe "Ward—Discard a card at random"
        }

        it("should define a creature with Annihilator") {
            val creature = card("Eldrazi Titan") {
                manaCost = "{10}"
                typeLine = "Creature — Eldrazi"
                power = 10
                toughness = 10
                keywordAbility(KeywordAbility.Numeric(Keyword.ANNIHILATOR, 6))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Numeric>()
            val annihilator = creature.keywordAbilities[0] as KeywordAbility.Numeric
            annihilator.keyword shouldBe Keyword.ANNIHILATOR
            annihilator.n shouldBe 6
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
                    KeywordAbility.Ward(WardCost.Mana("{3}")),
                    KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK))
                )
            }

            creature.keywords shouldContain Keyword.FLYING
            creature.keywords shouldContain Keyword.VIGILANCE
            creature.keywordAbilities shouldHaveSize 2
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Ward>()
            creature.keywordAbilities[1].shouldBeInstanceOf<KeywordAbility.Protection>()
        }

        it("should define a creature with Bushido") {
            val creature = card("Samurai Warrior") {
                manaCost = "{2}{W}"
                typeLine = "Creature — Human Samurai"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Numeric(Keyword.BUSHIDO, 2))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Numeric>()
            val bushido = creature.keywordAbilities[0] as KeywordAbility.Numeric
            bushido.keyword shouldBe Keyword.BUSHIDO
            bushido.n shouldBe 2
            creature.keywordAbilities[0].description shouldBe "Bushido 2"
        }

        it("should define an artifact creature with Modular") {
            val creature = card("Arcbound Worker") {
                manaCost = "{1}"
                typeLine = "Artifact Creature — Construct"
                power = 0
                toughness = 0
                keywordAbility(KeywordAbility.Numeric(Keyword.MODULAR, 1))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Numeric>()
            val modular = creature.keywordAbilities[0] as KeywordAbility.Numeric
            modular.keyword shouldBe Keyword.MODULAR
            modular.n shouldBe 1
            creature.keywordAbilities[0].description shouldBe "Modular 1"
        }

        it("should define a creature with Afflict") {
            val creature = card("Ruthless Sniper") {
                manaCost = "{2}{B}"
                typeLine = "Creature — Human Assassin"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.Numeric(Keyword.AFFLICT, 3))
            }

            creature.keywordAbilities shouldHaveSize 1
            creature.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Numeric>()
            val afflict = creature.keywordAbilities[0] as KeywordAbility.Numeric
            afflict.keyword shouldBe Keyword.AFFLICT
            afflict.n shouldBe 3
            creature.keywordAbilities[0].description shouldBe "Afflict 3"
        }

        it("should define a vehicle with Crew") {
            val vehicle = card("Heart of Kiran") {
                manaCost = "{2}"
                typeLine = "Legendary Artifact — Vehicle"
                power = 4
                toughness = 4
                keywords(Keyword.FLYING, Keyword.VIGILANCE)
                keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 3))
            }

            vehicle.typeLine.subtypes shouldContain Subtype.VEHICLE
            vehicle.keywordAbilities shouldHaveSize 1
            vehicle.keywordAbilities[0].shouldBeInstanceOf<KeywordAbility.Numeric>()
            val crew = vehicle.keywordAbilities[0] as KeywordAbility.Numeric
            crew.keyword shouldBe Keyword.CREW
            crew.n shouldBe 3
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
                    effect = Effects.DealDamage(4, EffectTarget.ContextTarget(0))
                    target = Targets.Creature
                }
            }

            kavu.name shouldBe "Flametongue Kavu"
            kavu.triggeredAbilities shouldHaveSize 1

            val ability = kavu.triggeredAbilities.first()
            ability.trigger shouldBe EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD)
            ability.effect shouldBe DealDamageEffect(4, EffectTarget.ContextTarget(0))
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
                    effect = Effects.DealDamage(3, EffectTarget.ContextTarget(0))
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
                    ability = ModifyStats(+1, +1, Filters.Group.creaturesYouControl)
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
                    ability = ModifyStats(+2, +0, Filters.EnchantedCreature)
                }
                staticAbility {
                    ability = GrantKeyword(Keyword.TRAMPLE, Filters.EnchantedCreature)
                }

                triggeredAbility {
                    trigger = Triggers.PutIntoGraveyardFromBattlefield
                    effect = Effects.ReturnToHand(EffectTarget.Self)
                }
            }

            rancor.typeLine.isAura shouldBe true
            rancor.script.auraTarget shouldNotBe null
            rancor.staticAbilities shouldHaveSize 2
            rancor.triggeredAbilities shouldHaveSize 1
        }
    }

    describe("Equipment") {

        it("should define Loxodon Warhammer with equip ability") {
            val warhammer = card("Loxodon Warhammer") {
                manaCost = "{3}"
                typeLine = "Artifact — Equipment"

                staticAbility {
                    ability = ModifyStats(+3, +0, Filters.EquippedCreature)
                }
                staticAbility {
                    ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
                }
                staticAbility {
                    ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
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
                    effect = Patterns.Hand.discardCards(1)
                    target = Targets.AllPlayers
                }

                loyaltyAbility(-2) {
                    effect = Effects.Sacrifice(Filters.Creature)
                    target = Targets.Player
                }

                loyaltyAbility(-6) {
                    effect = Effects.Sacrifice(Filters.Creature, count = 2)
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
                        Patterns.Hand.discardCards(2)
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
                    effect = Patterns.Library.searchLibrary(
                        filter = Filters.PlainsCard,
                        count = 3,
                        destination = SearchDestination.HAND
                    )
                }
            }

            gift.spellEffect shouldNotBe null
            gift.spellEffect.shouldBeInstanceOf<GatedEffect>()
            val gated = gift.spellEffect as GatedEffect
            val gate = gated.gate.shouldBeInstanceOf<Gate.WhenCondition>()
            gate.condition shouldBe Conditions.OpponentControlsMoreLands
            gated.then.shouldBeInstanceOf<CompositeEffect>()
        }
    }

    // =========================================================================
    // NEW FEATURES: Target Binding, Modal Targeting, Dynamic Stats
    // =========================================================================

    describe("Named Target Binding") {

        it("should define Electrolyze with two named targets") {
            val electrolyze = card("Electrolyze") {
                manaCost = "{1}{U}{R}"
                typeLine = "Instant"

                spell {
                    // Named target bindings - each gets an index
                    val firstTarget = target("first target", Targets.Any)
                    val secondTarget = target("second target", Targets.Any)

                    effect = Effects.Composite(
                        Effects.DealDamage(1, firstTarget),
                        Effects.DealDamage(1, secondTarget),
                        Effects.DrawCards(1)
                    )
                }
            }

            electrolyze.name shouldBe "Electrolyze"
            electrolyze.targetRequirements shouldHaveSize 2
            electrolyze.spellEffect shouldNotBe null
            electrolyze.spellEffect.shouldBeInstanceOf<CompositeEffect>()

            val composite = electrolyze.spellEffect as CompositeEffect
            composite.effects shouldHaveSize 3

            // First effect targets BoundVariable("first target")
            val damage1 = composite.effects[0] as DealDamageEffect
            damage1.target shouldBe EffectTarget.BoundVariable("first target")

            // Second effect targets BoundVariable("second target")
            val damage2 = composite.effects[1] as DealDamageEffect
            damage2.target shouldBe EffectTarget.BoundVariable("second target")
        }
    }

    describe("Modal Spells with Per-Mode Targeting") {

        it("should define Cryptic Command with mode-specific targets") {
            val crypticCommand = card("Cryptic Command") {
                manaCost = "{1}{U}{U}{U}"
                typeLine = "Instant"

                spell {
                    modal(chooseCount = 2) {
                        mode("Counter target spell") {
                            val spell = target("spell", Targets.Spell)
                            effect = CounterEffect()
                        }
                        mode("Return target permanent to its owner's hand") {
                            val permanent = target("permanent", Targets.Permanent)
                            effect = Effects.ReturnToHand(permanent)
                        }
                        mode("Tap all creatures your opponents control") {
                            effect = TapUntapEffect(
                                EffectTarget.GroupRef(GroupFilter.AllCreaturesOpponentsControl),
                                tap = true
                            )
                        }
                        mode("Draw a card", Effects.DrawCards(1))
                    }
                }
            }

            crypticCommand.name shouldBe "Cryptic Command"
            crypticCommand.spellEffect shouldNotBe null
            crypticCommand.spellEffect.shouldBeInstanceOf<ModalEffect>()

            val modal = crypticCommand.spellEffect as ModalEffect
            modal.chooseCount shouldBe 2
            modal.modes shouldHaveSize 4

            // Mode 1 has a target requirement (spell)
            modal.modes[0].targetRequirements shouldHaveSize 1

            // Mode 2 has a target requirement (permanent)
            modal.modes[1].targetRequirements shouldHaveSize 1

            // Mode 3 has no target requirement
            modal.modes[2].targetRequirements shouldHaveSize 0

            // Mode 4 has no target requirement
            modal.modes[3].targetRequirements shouldHaveSize 0
        }

        it("should define Charm with simple mode choices") {
            val charm = card("Bant Charm") {
                manaCost = "{G}{W}{U}"
                typeLine = "Instant"

                spell {
                    modal {
                        mode("Destroy target artifact", Effects.Destroy(EffectTarget.ContextTarget(0)))
                        mode("Put target creature on the bottom of its owner's library") {
                            val creature = target("creature", Targets.Creature)
                            effect = MoveToZoneEffect(creature, Zone.LIBRARY, ZonePlacement.Top)
                        }
                        mode("Counter target instant spell") {
                            val instant = target("instant", Targets.Spell)
                            effect = CounterEffect()
                        }
                    }
                }
            }

            charm.spellEffect.shouldBeInstanceOf<ModalEffect>()
            val modal = charm.spellEffect as ModalEffect
            modal.chooseCount shouldBe 1
            modal.modes shouldHaveSize 3
        }
    }

    describe("Dynamic Creature Stats") {

        it("should define Tarmogoyf with dynamic power and toughness") {
            val tarmogoyf = card("Tarmogoyf") {
                manaCost = "{1}{G}"
                typeLine = "Creature — Lhurgoyf"

                // */*+1 where * is number of card types in all graveyards
                dynamicStats(
                    DynamicAmount.AggregateZone(
                        player = Player.Each,
                        zone = Zone.GRAVEYARD,
                        aggregation = Aggregation.DISTINCT_TYPES
                    ),
                    powerOffset = 0,
                    toughnessOffset = 1
                )
            }

            tarmogoyf.name shouldBe "Tarmogoyf"
            tarmogoyf.creatureStats shouldNotBe null
            tarmogoyf.creatureStats!!.isDynamic shouldBe true

            // Power is * (dynamic with no offset)
            tarmogoyf.creatureStats.power.shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.Dynamic>()

            // Toughness is *+1 (dynamic with +1 offset)
            val toughness = tarmogoyf.creatureStats.toughness.shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset>()
            toughness.offset shouldBe 1
        }

        it("should define Lhurgoyf with creature-count based stats") {
            val lhurgoyf = card("Lhurgoyf") {
                manaCost = "{2}{G}{G}"
                typeLine = "Creature — Lhurgoyf"

                dynamicPower(DynamicAmounts.creatureCardsInYourGraveyard())
                dynamicToughness(DynamicAmounts.creatureCardsInYourGraveyard(), offset = 1)
            }

            lhurgoyf.creatureStats shouldNotBe null
            lhurgoyf.creatureStats!!.isDynamic shouldBe true
            lhurgoyf.creatureStats.basePower shouldBe null  // No fixed base power

            lhurgoyf.creatureStats.power.shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.Dynamic>()
            val toughness = lhurgoyf.creatureStats.toughness
                .shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset>()
            toughness.offset shouldBe 1
        }

        it("should define a power-only CDA, leaving toughness printed") {
            val duelist = card("Duelist") {
                manaCost = "{1}{U}"
                typeLine = "Creature — Human Advisor"
                toughness = 3

                dynamicPower(DynamicAmount.TurnTracking(Player.You, TurnTracker.CARDS_DRAWN))
            }

            duelist.creatureStats shouldNotBe null
            duelist.creatureStats!!.isDynamic shouldBe true
            duelist.creatureStats.power.shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.Dynamic>()
            duelist.creatureStats.toughness shouldBe com.wingedsheep.sdk.model.CharacteristicValue.Fixed(3)
        }

        it("should define power and toughness from two different dynamic sources") {
            val kavu = card("Yavimaya Kavu") {
                manaCost = "{2}{R}{G}"
                typeLine = "Creature — Kavu"

                dynamicPower(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withColor(Color.RED)))
                dynamicToughness(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withColor(Color.GREEN)))
            }

            val power = kavu.creatureStats!!.power
                .shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.Dynamic>()
            val toughness = kavu.creatureStats.toughness
                .shouldBeInstanceOf<com.wingedsheep.sdk.model.CharacteristicValue.Dynamic>()
            power.source shouldNotBe toughness.source
        }

        it("dynamicStats composes dynamicPower and dynamicToughness over one source") {
            val shared = DynamicAmounts.creatureCardsInYourGraveyard()

            val viaHelper = card("Composed") {
                manaCost = "{2}{G}"
                typeLine = "Creature — Lhurgoyf"
                dynamicStats(shared, toughnessOffset = 1)
            }
            val viaParts = card("Composed") {
                manaCost = "{2}{G}"
                typeLine = "Creature — Lhurgoyf"
                dynamicPower(shared)
                dynamicToughness(shared, offset = 1)
            }

            viaHelper.creatureStats shouldBe viaParts.creatureStats
        }
    }

    describe("Replacement Effects") {

        it("should define Doubling Season with token doubling") {
            val doublingSeason = card("Doubling Season") {
                manaCost = "{4}{G}"
                typeLine = "Enchantment"

                // Note: ReplacementEffect is defined but CardScript field
                // is used to store them
            }

            doublingSeason.name shouldBe "Doubling Season"
            doublingSeason.typeLine.isEnchantment shouldBe true
        }

        it("should have compositional ReplacementEffect types available") {
            // Test that replacement effect types are properly defined with compositional filters
            val tokenDoubler = MultiplyTokenCreation()
            tokenDoubler.appliesTo.shouldBeInstanceOf<EventPattern.TokenCreationEvent>()

            // Hardened Scales - +1 counter when +1/+1 counters placed on creatures you control
            val counterAdder = ModifyCounterPlacement(
                modifier = 1,
                appliesTo = EventPattern.CounterPlacementEvent(
                    counterType = CounterTypeFilter.PlusOnePlusOne,
                    recipient = RecipientFilter.CreatureYouControl
                )
            )
            counterAdder.modifier shouldBe 1
            counterAdder.appliesTo.shouldBeInstanceOf<EventPattern.CounterPlacementEvent>()

            // Enters tapped with compositional event
            val entersTapped = EntersTapped(
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.NonlandPermanent,
                    to = Zone.BATTLEFIELD
                )
            )
            entersTapped.appliesTo.shouldBeInstanceOf<EventPattern.ZoneChangeEvent>()

            // Rest in Peace - redirect graveyard to exile
            val restInPeace = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD)
            )
            restInPeace.newDestination shouldBe Zone.EXILE

            // Combat damage from red sources to creatures you control
            val damageFilter = EventPattern.DamageEvent(
                recipient = RecipientFilter.CreatureYouControl,
                source = SourceFilter.HasColor(com.wingedsheep.sdk.core.Color.RED),
                damageType = DamageType.Combat
            )
            damageFilter.recipient shouldBe RecipientFilter.CreatureYouControl
            damageFilter.damageType shouldBe DamageType.Combat
        }

        it("should support CardScript with replacement effects") {
            val restInPeace = card("Rest in Peace") {
                manaCost = "{1}{W}"
                typeLine = "Enchantment"
            }

            // Verify script can hold replacement effects (even if empty for now)
            restInPeace.script.hasReplacementEffects shouldBe false
        }
    }

    describe("Optional Cost Effects") {

        it("should support may-pay pattern") {
            val optionalEffect = OptionalCostEffect(
                cost = PayLifeEffect(2),
                ifPaid = DrawCardsEffect(1),
                ifNotPaid = null
            )

            optionalEffect.description shouldBe "You may pay 2 life. If you do, draw a card"
        }

        it("should support may-pay-or-else pattern") {
            val optionalEffect = OptionalCostEffect(
                cost = SacrificeEffect(GameObjectFilter.Creature),
                ifPaid = DealDamageEffect(3, EffectTarget.ContextTarget(0)),
                ifNotPaid = LoseLifeEffect(3, EffectTarget.Controller)
            )

            optionalEffect.description shouldBe "You may sacrifice a creature. If you do, deal 3 damage to target. Otherwise, you lose 3 life"
        }

        it("should support reflexive triggers") {
            val reflexive = ReflexiveTriggerEffect(
                action = SacrificeEffect(GameObjectFilter.Creature),
                optional = true,
                reflexiveEffect = DealDamageEffect(5, EffectTarget.ContextTarget(0))
            )

            reflexive.description shouldBe "You may sacrifice a creature. When you do, deal 5 damage to target"
        }
    }

    describe("Conditional Static Abilities") {

        it("should define Karakyk Guardian with conditional hexproof") {
            val karakykGuardian = card("Karakyk Guardian") {
                manaCost = "{3}{G}{U}{R}"
                typeLine = "Creature — Dragon"
                power = 6
                toughness = 5
                keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.TRAMPLE)

                staticAbility {
                    ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source())
                    condition = NotCondition(Conditions.SourceHasDealtDamage)
                }
            }

            karakykGuardian.name shouldBe "Karakyk Guardian"
            karakykGuardian.keywords shouldContain Keyword.FLYING
            karakykGuardian.keywords shouldContain Keyword.VIGILANCE
            karakykGuardian.keywords shouldContain Keyword.TRAMPLE

            karakykGuardian.staticAbilities shouldHaveSize 1
            val staticAbility = karakykGuardian.staticAbilities[0]
            val conditional = staticAbility.shouldBeInstanceOf<ConditionalStaticAbility>()
            conditional.condition.shouldBeInstanceOf<NotCondition>()

            val grantKeyword = conditional.ability.shouldBeInstanceOf<GrantKeyword>()
            grantKeyword.keyword shouldBe Keyword.HEXPROOF.name
            grantKeyword.filter shouldBe GroupFilter.source()

            val notCondition = conditional.condition.shouldBeInstanceOf<NotCondition>()
            notCondition.condition shouldBe Conditions.SourceHasDealtDamage
        }

        it("should support conditional stat bonuses") {
            val feralKrushok = card("Feral Krushok") {
                manaCost = "{4}{G}"
                typeLine = "Creature — Beast"
                power = 4
                toughness = 4

                // Gets +2/+2 while attacking
                staticAbility {
                    ability = ModifyStats(2, 2, GroupFilter.source())
                    condition = Conditions.SourceIsAttacking
                }
            }

            feralKrushok.staticAbilities shouldHaveSize 1
            val staticAbility = feralKrushok.staticAbilities[0]
            val conditional = staticAbility.shouldBeInstanceOf<ConditionalStaticAbility>()
            conditional.condition shouldBe Conditions.SourceIsAttacking

            val modifyStats = conditional.ability.shouldBeInstanceOf<ModifyStats>()
            modifyStats.powerBonus shouldBe 2
            modifyStats.toughnessBonus shouldBe 2
        }

        it("should support conditional blocking restrictions") {
            val guardianShield = card("Guardian Shield-Bearer") {
                manaCost = "{1}{W}"
                typeLine = "Creature — Human Soldier"
                power = 2
                toughness = 3

                // Can't block unless you have more life than an opponent
                staticAbility {
                    ability = CantBlock(GroupFilter.source())
                    condition = Conditions.Not(Conditions.MoreLifeThanOpponent)
                }
            }

            guardianShield.staticAbilities shouldHaveSize 1
            val staticAbility = guardianShield.staticAbilities[0]
            val conditional = staticAbility.shouldBeInstanceOf<ConditionalStaticAbility>()
            conditional.ability.shouldBeInstanceOf<CantBlock>()
            conditional.condition.shouldBeInstanceOf<NotCondition>()
        }

        it("should describe conditional abilities correctly") {
            val conditional = ConditionalStaticAbility(
                ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
                condition = NotCondition(Conditions.SourceHasDealtDamage)
            )

            conditional.description shouldBe "this creature have hexproof if not (if this has dealt damage)"
        }
    }

    describe("Split-layout cards (Rooms)") {

        it("should default to NORMAL layout with no faces for ordinary cards") {
            val bolt = card("Lightning Bolt") {
                manaCost = "{R}"
                typeLine = "Instant"
                oracleText = "Lightning Bolt deals 3 damage to any target."
            }

            bolt.layout shouldBe CardLayout.NORMAL
            bolt.cardFaces shouldHaveSize 0
            bolt.isSplit shouldBe false
        }

        it("should define a SPLIT card with two faces and per-face abilities") {
            // Stand-in for Unholy Annex // Ritual Chamber. We don't yet have a "door unlock"
            // trigger, so this test exercises shape only — that face-scoped triggered abilities
            // are stored on each face's CardScript, not on the top-level script.
            val annex = card("Unholy Annex // Ritual Chamber") {
                layout = CardLayout.SPLIT

                face("Unholy Annex") {
                    manaCost = "{2}{B}"
                    typeLine = "Enchantment — Room"
                    oracleText = "At the beginning of your end step, draw a card."
                    triggeredAbility {
                        trigger = Triggers.YourEndStep
                        effect = Effects.DrawCards(1)
                    }
                }

                face("Ritual Chamber") {
                    manaCost = "{3}{B}{B}"
                    typeLine = "Enchantment — Room"
                    oracleText = "When this enters, do something."
                    triggeredAbility {
                        trigger = Triggers.EntersBattlefield
                        effect = Effects.DrawCards(1)
                    }
                }
            }

            annex.layout shouldBe CardLayout.SPLIT
            annex.isSplit shouldBe true
            annex.cardFaces shouldHaveSize 2

            // Top-level type line and oracle text are derived from faces (CR 709.4c / 709.5a).
            annex.typeLine.toString() shouldBe "Enchantment — Room"
            annex.typeLine.subtypes shouldContain Subtype.ROOM
            annex.oracleText shouldBe
                "At the beginning of your end step, draw a card.\n//\nWhen this enters, do something."

            val unholy = annex.cardFaces[0]
            val ritual = annex.cardFaces[1]
            unholy.name shouldBe "Unholy Annex"
            unholy.manaCost.toString() shouldBe "{2}{B}"
            unholy.script.triggeredAbilities shouldHaveSize 1
            ritual.name shouldBe "Ritual Chamber"
            ritual.manaCost.toString() shouldBe "{3}{B}{B}"
            ritual.script.triggeredAbilities shouldHaveSize 1

            // Face abilities must NOT bleed into the top-level script — the engine reads them
            // per-face in later phases when filtering by which door is unlocked.
            annex.script.triggeredAbilities shouldHaveSize 0
        }

        it("should reject SPLIT layout with fewer than two faces") {
            val ex = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                card("Half a card") {
                    layout = CardLayout.SPLIT
                    face("Lonely") {
                        manaCost = "{B}"
                        typeLine = "Enchantment — Room"
                    }
                }
            }
            ex.message?.contains("at least 2 faces") shouldBe true
        }

        it("should reject blocks(attackerFilter) with a non-SELF binding") {
            // attackerFilter is only honored by the SELF detector branch; the ANY branch
            // ignores it, so the combination must fail fast rather than silently misfire.
            val ex = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                Triggers.blocks(
                    binding = TriggerBinding.ANY,
                    attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
                )
            }
            ex.message?.contains("attackerFilter") shouldBe true

            // SELF binding (the default) is accepted.
            Triggers.blocks(
                attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            ).binding shouldBe TriggerBinding.SELF
        }
    }
})
