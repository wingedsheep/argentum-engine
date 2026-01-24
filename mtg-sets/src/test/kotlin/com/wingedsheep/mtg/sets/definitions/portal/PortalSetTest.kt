package com.wingedsheep.mtg.sets.definitions.portal

import com.wingedsheep.mtg.sets.definitions.portal.cards.*
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Portal Set card definitions.
 */
class PortalSetTest : DescribeSpec({

    describe("Portal Set - Cards 1-10") {

        describe("Alabaster Dragon") {
            val card = AlabasterDragon

            it("should have correct basic properties") {
                card.name shouldBe "Alabaster Dragon"
                card.manaCost.toString() shouldBe "{4}{W}{W}"
                card.cmc shouldBe 6
                card.typeLine.isCreature shouldBe true
                card.typeLine.subtypes shouldContain Subtype.DRAGON
            }

            it("should have 4/4 stats") {
                card.creatureStats shouldNotBe null
                card.creatureStats!!.basePower shouldBe 4
                card.creatureStats!!.baseToughness shouldBe 4
            }

            it("should have flying") {
                card.keywords shouldContain Keyword.FLYING
            }

            it("should have death trigger to shuffle into library") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnDeath>()
                trigger.effect.shouldBeInstanceOf<ShuffleIntoLibraryEffect>()
            }

            it("should have complete metadata") {
                card.metadata.rarity shouldBe Rarity.RARE
                card.metadata.collectorNumber shouldBe "1"
                card.metadata.artist shouldBe "Ted Naifeh"
                card.metadata.imageUri shouldStartWith "https://cards.scryfall.io/"
            }
        }

        describe("Angelic Blessing") {
            val card = AngelicBlessing

            it("should be a sorcery") {
                card.name shouldBe "Angelic Blessing"
                card.manaCost.toString() shouldBe "{2}{W}"
                card.typeLine.isSorcery shouldBe true
            }

            it("should grant +3/+3 and flying") {
                card.spellEffect shouldNotBe null
                card.spellEffect.shouldBeInstanceOf<CompositeEffect>()
                val composite = card.spellEffect as CompositeEffect
                composite.effects shouldHaveSize 2
            }

            it("should target a creature") {
                card.targetRequirements shouldHaveSize 1
            }

            it("should have complete metadata with flavor text") {
                card.metadata.rarity shouldBe Rarity.COMMON
                card.metadata.artist shouldBe "DiTerlizzi"
                card.metadata.flavorText shouldNotBe null
                card.metadata.imageUri shouldStartWith "https://cards.scryfall.io/"
            }
        }

        describe("Archangel") {
            val card = Archangel

            it("should have correct stats") {
                card.name shouldBe "Archangel"
                card.manaCost.toString() shouldBe "{5}{W}{W}"
                card.cmc shouldBe 7
                card.creatureStats?.basePower shouldBe 5
                card.creatureStats?.baseToughness shouldBe 5
            }

            it("should have flying and vigilance") {
                card.keywords shouldContain Keyword.FLYING
                card.keywords shouldContain Keyword.VIGILANCE
            }

            it("should be an Angel") {
                card.typeLine.subtypes shouldContain Subtype.ANGEL
            }

            it("should have complete metadata") {
                card.metadata.artist shouldBe "Quinton Hoover"
                card.metadata.imageUri shouldStartWith "https://cards.scryfall.io/"
            }
        }

        describe("Ardent Militia") {
            val card = ArdentMilitia

            it("should be a 2/5 with vigilance") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 5
                card.keywords shouldContain Keyword.VIGILANCE
            }

            it("should be a Human Soldier") {
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.SOLDIER
            }

            it("should have flavor text") {
                card.metadata.flavorText shouldNotBe null
            }
        }

        describe("Armageddon") {
            val card = Armageddon

            it("should be a sorcery that destroys all lands") {
                card.name shouldBe "Armageddon"
                card.typeLine.isSorcery shouldBe true
                card.spellEffect shouldBe DestroyAllLandsEffect
            }

            it("should cost 3W") {
                card.manaCost.toString() shouldBe "{3}{W}"
                card.cmc shouldBe 4
            }

            it("should have complete metadata") {
                card.metadata.artist shouldBe "John Avon"
                card.metadata.flavorText shouldNotBe null
            }
        }

        describe("Armored Pegasus") {
            val card = ArmoredPegasus

            it("should be a 1/2 flyer") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 2
                card.keywords shouldContain Keyword.FLYING
            }

            it("should be a Pegasus") {
                card.typeLine.subtypes shouldContain Subtype.PEGASUS
            }
        }

        describe("Blessed Reversal") {
            val card = BlessedReversal

            it("should be an instant") {
                card.typeLine.isInstant shouldBe true
            }

            it("should gain life per attacker") {
                card.spellEffect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = card.spellEffect as GainLifeEffect
                effect.amount.shouldBeInstanceOf<DynamicAmount.CreaturesAttackingYou>()
                val dynamic = effect.amount as DynamicAmount.CreaturesAttackingYou
                dynamic.multiplier shouldBe 3
            }
        }

        describe("Blinding Light") {
            val card = BlindingLight

            it("should tap all nonwhite creatures") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<TapAllCreaturesEffect>()
                val effect = card.spellEffect as TapAllCreaturesEffect
                effect.filter shouldBe CreatureGroupFilter.NonWhite
            }
        }

        describe("Border Guard") {
            val card = BorderGuard

            it("should be a vanilla 1/4") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 4
                card.keywords shouldHaveSize 0
                card.triggeredAbilities shouldHaveSize 0
                card.activatedAbilities shouldHaveSize 0
            }

            it("should have flavor text") {
                card.metadata.flavorText shouldNotBe null
            }
        }

        describe("Breath of Life") {
            val card = BreathOfLife

            it("should return creature from graveyard to battlefield") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<ReturnFromGraveyardEffect>()
                val effect = card.spellEffect as ReturnFromGraveyardEffect
                effect.destination shouldBe SearchDestination.BATTLEFIELD
            }

            it("should target creature card in graveyard") {
                card.targetRequirements shouldHaveSize 1
            }
        }
    }

    describe("Portal Set - Cards 11-20") {

        describe("Charging Paladin") {
            val card = ChargingPaladin

            it("should be a 2/2 Human Knight") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.KNIGHT
            }

            it("should have triggered ability when attacking") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnAttack>()
            }

            it("should get +0/+3 when attacking") {
                val effect = card.triggeredAbilities.first().effect
                effect.shouldBeInstanceOf<ModifyStatsEffect>()
                val modifyStats = effect as ModifyStatsEffect
                modifyStats.powerModifier shouldBe 0
                modifyStats.toughnessModifier shouldBe 3
            }
        }

        describe("Defiant Stand") {
            val card = DefiantStand

            it("should be an instant with cast restrictions") {
                card.typeLine.isInstant shouldBe true
                card.script.hasCastRestrictions shouldBe true
                card.script.castRestrictions shouldHaveSize 2
            }

            it("should only be castable during declare attackers step") {
                val stepRestriction = card.script.castRestrictions
                    .filterIsInstance<CastRestriction.OnlyDuringStep>()
                    .first()
                stepRestriction.step shouldBe Step.DECLARE_ATTACKERS
            }

            it("should require being attacked") {
                val conditionRestriction = card.script.castRestrictions
                    .filterIsInstance<CastRestriction.OnlyIfCondition>()
                    .first()
                conditionRestriction.condition shouldBe YouWereAttackedThisStep
            }

            it("should grant +1/+3 and untap") {
                card.spellEffect.shouldBeInstanceOf<CompositeEffect>()
                val composite = card.spellEffect as CompositeEffect
                composite.effects shouldHaveSize 2
            }
        }

        describe("Devoted Hero") {
            val card = DevotedHero

            it("should be a 1/2 vanilla Elf Soldier") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.ELF
                card.typeLine.subtypes shouldContain Subtype.SOLDIER
                card.keywords shouldHaveSize 0
            }

            it("should cost W") {
                card.manaCost.toString() shouldBe "{W}"
                card.cmc shouldBe 1
            }
        }

        describe("False Peace") {
            val card = FalsePeace

            it("should skip target player's combat phases") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<SkipCombatPhasesEffect>()
            }

            it("should target a player") {
                card.targetRequirements shouldHaveSize 1
            }
        }

        describe("Fleet-Footed Monk") {
            val card = FleetFootedMonk

            it("should be a 1/1 Human Monk") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.MONK
            }

            it("should have evasion from power 2+ creatures") {
                card.staticAbilities shouldHaveSize 1
                val ability = card.staticAbilities.first()
                ability.shouldBeInstanceOf<CantBeBlockedByPower>()
                val evasion = ability as CantBeBlockedByPower
                evasion.minPower shouldBe 2
            }
        }

        describe("Foot Soldiers") {
            val card = FootSoldiers

            it("should be a 2/4 vanilla Human Soldier") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 4
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.SOLDIER
                card.keywords shouldHaveSize 0
            }
        }

        describe("Gift of Estates") {
            val card = GiftOfEstates

            it("should have conditional library search") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<SearchLibraryEffect>()
            }

            it("should search for up to 3 Plains") {
                val effect = card.spellEffect as SearchLibraryEffect
                effect.count shouldBe 3
                effect.destination shouldBe SearchDestination.HAND
            }
        }

        describe("Harsh Justice") {
            val card = HarshJustice

            it("should be an instant with cast restrictions") {
                card.typeLine.isInstant shouldBe true
                card.script.hasCastRestrictions shouldBe true
            }

            it("should reflect combat damage") {
                card.spellEffect.shouldBeInstanceOf<ReflectCombatDamageEffect>()
            }
        }

        describe("Keen-Eyed Archers") {
            val card = KeenEyedArchers

            it("should be a 2/2 Elf Archer with Reach") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.ELF
                card.typeLine.subtypes shouldContain Subtype.ARCHER
                card.keywords shouldContain Keyword.REACH
            }
        }

        describe("Knight Errant") {
            val card = KnightErrant

            it("should be a 2/2 vanilla Human Knight") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.KNIGHT
                card.keywords shouldHaveSize 0
            }
        }
    }

    describe("Portal Set - Cards 21-30") {

        describe("Path of Peace") {
            val card = PathOfPeace

            it("should be a sorcery") {
                card.name shouldBe "Path of Peace"
                card.manaCost.toString() shouldBe "{3}{W}"
                card.typeLine.isSorcery shouldBe true
            }

            it("should target a creature") {
                card.targetRequirements shouldHaveSize 1
            }

            it("should destroy and grant life to owner") {
                card.spellEffect.shouldBeInstanceOf<CompositeEffect>()
                val composite = card.spellEffect as CompositeEffect
                composite.effects shouldHaveSize 2
            }
        }

        describe("Regal Unicorn") {
            val card = RegalUnicorn

            it("should be a 2/3 vanilla Unicorn") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 3
                card.typeLine.subtypes shouldContain Subtype.UNICORN
                card.keywords shouldHaveSize 0
            }
        }

        describe("Renewing Dawn") {
            val card = RenewingDawn

            it("should gain life per Mountain") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = card.spellEffect as GainLifeEffect
                effect.amount.shouldBeInstanceOf<DynamicAmount.LandsOfTypeTargetOpponentControls>()
                val dynamic = effect.amount as DynamicAmount.LandsOfTypeTargetOpponentControls
                dynamic.landType shouldBe "Mountain"
                dynamic.multiplier shouldBe 2
            }

            it("should target an opponent") {
                card.targetRequirements shouldHaveSize 1
            }
        }

        describe("Sacred Knight") {
            val card = SacredKnight

            it("should be a 3/2 Human Knight") {
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.KNIGHT
            }

            it("should have evasion from black and red creatures") {
                card.staticAbilities shouldHaveSize 1
                val ability = card.staticAbilities.first()
                ability.shouldBeInstanceOf<CantBeBlockedByColors>()
                val evasion = ability as CantBeBlockedByColors
                evasion.colors shouldContain Color.BLACK
                evasion.colors shouldContain Color.RED
            }
        }

        describe("Sacred Nectar") {
            val card = SacredNectar

            it("should gain 4 life") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = card.spellEffect as GainLifeEffect
                effect.amount shouldBe DynamicAmount.Fixed(4)
            }
        }

        describe("Seasoned Marshal") {
            val card = SeasonedMarshal

            it("should be a 2/2 Human Soldier") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.SOLDIER
            }

            it("should have attack trigger with optional tap") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnAttack>()
                trigger.optional shouldBe true
                trigger.effect.shouldBeInstanceOf<TapUntapEffect>()
            }
        }

        describe("Spiritual Guardian") {
            val card = SpiritualGuardian

            it("should be a 3/4 Spirit") {
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 4
                card.typeLine.subtypes shouldContain Subtype.SPIRIT
            }

            it("should have ETB trigger to gain life") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
                trigger.effect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = trigger.effect as GainLifeEffect
                effect.amount shouldBe DynamicAmount.Fixed(4)
            }
        }

        describe("Spotted Griffin") {
            val card = SpottedGriffin

            it("should be a 2/3 Griffin with flying") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 3
                card.typeLine.subtypes shouldContain Subtype.GRIFFIN
                card.keywords shouldContain Keyword.FLYING
            }
        }

        describe("Starlight") {
            val card = Starlight

            it("should gain life per black creature") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = card.spellEffect as GainLifeEffect
                effect.amount.shouldBeInstanceOf<DynamicAmount.CreaturesOfColorTargetOpponentControls>()
                val dynamic = effect.amount as DynamicAmount.CreaturesOfColorTargetOpponentControls
                dynamic.color shouldBe Color.BLACK
                dynamic.multiplier shouldBe 3
            }

            it("should target an opponent") {
                card.targetRequirements shouldHaveSize 1
            }
        }

        describe("Starlit Angel") {
            val card = StarlitAngel

            it("should be a 3/4 Angel with flying") {
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 4
                card.typeLine.subtypes shouldContain Subtype.ANGEL
                card.keywords shouldContain Keyword.FLYING
            }
        }
    }

    describe("Portal Set - Cards 31-40") {

        describe("Steadfastness") {
            val card = Steadfastness

            it("should give creatures +0/+3") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<ModifyStatsForGroupEffect>()
                val effect = card.spellEffect as ModifyStatsForGroupEffect
                effect.powerModifier shouldBe 0
                effect.toughnessModifier shouldBe 3
                effect.filter shouldBe CreatureGroupFilter.AllYouControl
            }
        }

        describe("Stern Marshal") {
            val card = SternMarshal

            it("should be a 2/2 Human Soldier") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.SOLDIER
            }

            it("should have tap ability to pump creature") {
                card.activatedAbilities shouldHaveSize 1
                val ability = card.activatedAbilities.first()
                ability.cost shouldBe AbilityCost.Tap
                ability.effect.shouldBeInstanceOf<ModifyStatsEffect>()
                val effect = ability.effect as ModifyStatsEffect
                effect.powerModifier shouldBe 2
                effect.toughnessModifier shouldBe 2
            }
        }

        describe("Temporary Truce") {
            val card = TemporaryTruce

            it("should let each player draw up to 2 cards") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<EachPlayerMayDrawEffect>()
                val effect = card.spellEffect as EachPlayerMayDrawEffect
                effect.maxCards shouldBe 2
            }
        }

        describe("Valorous Charge") {
            val card = ValorousCharge

            it("should give white creatures +2/+0") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<ModifyStatsForGroupEffect>()
                val effect = card.spellEffect as ModifyStatsForGroupEffect
                effect.powerModifier shouldBe 2
                effect.toughnessModifier shouldBe 0
            }
        }

        describe("Venerable Monk") {
            val card = VenerableMonk

            it("should be a 2/2 Human Monk Cleric") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.MONK
                card.typeLine.subtypes shouldContain Subtype.CLERIC
            }

            it("should have ETB to gain 2 life") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
                trigger.effect.shouldBeInstanceOf<GainLifeEffect>()
                val effect = trigger.effect as GainLifeEffect
                effect.amount shouldBe DynamicAmount.Fixed(2)
            }
        }

        describe("Vengeance") {
            val card = Vengeance

            it("should destroy target tapped creature") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<DestroyEffect>()
                card.targetRequirements shouldHaveSize 1
            }
        }

        describe("Wall of Swords") {
            val card = WallOfSwords

            it("should be a 3/5 Wall with Defender and Flying") {
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 5
                card.typeLine.subtypes shouldContain Subtype.WALL
                card.keywords shouldContain Keyword.DEFENDER
                card.keywords shouldContain Keyword.FLYING
            }
        }

        describe("Warrior's Charge") {
            val card = WarriorsCharge

            it("should give creatures +1/+1") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<ModifyStatsForGroupEffect>()
                val effect = card.spellEffect as ModifyStatsForGroupEffect
                effect.powerModifier shouldBe 1
                effect.toughnessModifier shouldBe 1
                effect.filter shouldBe CreatureGroupFilter.AllYouControl
            }
        }

        describe("Wrath of God") {
            val card = WrathOfGod

            it("should destroy all creatures") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect shouldBe DestroyAllCreaturesEffect
            }

            it("should cost 2WW") {
                card.manaCost.toString() shouldBe "{2}{W}{W}"
                card.cmc shouldBe 4
            }
        }

        describe("Ancestral Memories") {
            val card = AncestralMemories

            it("should look at top 7 and keep 2") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<LookAtTopCardsEffect>()
                val effect = card.spellEffect as LookAtTopCardsEffect
                effect.count shouldBe 7
                effect.keepCount shouldBe 2
                effect.restToGraveyard shouldBe true
            }

            it("should cost 2UUU") {
                card.manaCost.toString() shouldBe "{2}{U}{U}{U}"
                card.cmc shouldBe 5
            }
        }
    }

    describe("Portal Set - Cards 41-50") {

        describe("Balance of Power") {
            val card = BalanceOfPower

            it("should draw cards equal to hand difference") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<DrawCardsEffect>()
                val effect = card.spellEffect as DrawCardsEffect
                effect.count shouldBe DynamicAmount.HandSizeDifferenceFromTargetOpponent
            }

            it("should target an opponent") {
                card.targetRequirements shouldHaveSize 1
            }

            it("should cost 3UU") {
                card.manaCost.toString() shouldBe "{3}{U}{U}"
                card.cmc shouldBe 5
            }
        }

        describe("Baleful Stare") {
            val card = BalefulStare

            it("should reveal hand and draw per Mountain/red card") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<RevealHandDrawPerMatchEffect>()
                val effect = card.spellEffect as RevealHandDrawPerMatchEffect
                effect.landType shouldBe "Mountain"
                effect.color shouldBe Color.RED
            }
        }

        describe("Capricious Sorcerer") {
            val card = CapriciousSorcerer

            it("should be a 1/1 Human Wizard") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.WIZARD
            }

            it("should have tap ability to deal damage") {
                card.activatedAbilities shouldHaveSize 1
                val ability = card.activatedAbilities.first()
                ability.cost shouldBe AbilityCost.Tap
                ability.effect.shouldBeInstanceOf<DealDamageEffect>()
            }
        }

        describe("Cloak of Feathers") {
            val card = CloakOfFeathers

            it("should grant flying and draw") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<CompositeEffect>()
                val composite = card.spellEffect as CompositeEffect
                composite.effects shouldHaveSize 2
            }

            it("should cost U") {
                card.manaCost.toString() shouldBe "{U}"
                card.cmc shouldBe 1
            }
        }

        describe("Cloud Dragon") {
            val card = CloudDragon

            it("should be a 5/4 Illusion Dragon with flying") {
                card.creatureStats?.basePower shouldBe 5
                card.creatureStats?.baseToughness shouldBe 4
                card.typeLine.subtypes shouldContain Subtype.ILLUSION
                card.typeLine.subtypes shouldContain Subtype.DRAGON
                card.keywords shouldContain Keyword.FLYING
            }

            it("should only block creatures with flying") {
                card.staticAbilities shouldHaveSize 1
                card.staticAbilities.first().shouldBeInstanceOf<CanOnlyBlockCreaturesWithKeyword>()
            }
        }

        describe("Cloud Pirates") {
            val card = CloudPirates

            it("should be a 1/1 Human Pirate with flying") {
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.PIRATE
                card.keywords shouldContain Keyword.FLYING
            }

            it("should only block creatures with flying") {
                card.staticAbilities shouldHaveSize 1
                card.staticAbilities.first().shouldBeInstanceOf<CanOnlyBlockCreaturesWithKeyword>()
            }
        }

        describe("Cloud Spirit") {
            val card = CloudSpirit

            it("should be a 3/1 Spirit with flying") {
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.SPIRIT
                card.keywords shouldContain Keyword.FLYING
            }

            it("should only block creatures with flying") {
                card.staticAbilities shouldHaveSize 1
                card.staticAbilities.first().shouldBeInstanceOf<CanOnlyBlockCreaturesWithKeyword>()
            }
        }

        describe("Command of Unsummoning") {
            val card = CommandOfUnsummoning

            it("should be an instant with cast restrictions") {
                card.typeLine.isInstant shouldBe true
                card.script.hasCastRestrictions shouldBe true
            }

            it("should return 1-2 attacking creatures") {
                card.spellEffect.shouldBeInstanceOf<ReturnToHandEffect>()
                card.targetRequirements shouldHaveSize 1
            }
        }

        describe("Coral Eel") {
            val card = CoralEel

            it("should be a 2/1 vanilla Fish") {
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.FISH
                card.keywords shouldHaveSize 0
            }
        }

        describe("Cruel Fate") {
            val card = CruelFate

            it("should look at top 5 and put 1 in graveyard") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<LookAtOpponentLibraryEffect>()
                val effect = card.spellEffect as LookAtOpponentLibraryEffect
                effect.count shouldBe 5
                effect.toGraveyard shouldBe 1
            }
        }
    }

    describe("Portal Set - Cards 51-60") {

        describe("Deep-Sea Serpent") {
            val card = DeepSeaSerpent

            it("should be a 5/5 Serpent") {
                card.manaCost.toString() shouldBe "{4}{U}{U}"
                card.creatureStats?.basePower shouldBe 5
                card.creatureStats?.baseToughness shouldBe 5
                card.typeLine.subtypes shouldContain Subtype.SERPENT
            }

            it("should have Island attack restriction") {
                card.staticAbilities shouldHaveSize 1
                card.staticAbilities.first().shouldBeInstanceOf<CantAttackUnlessDefenderControlsLandType>()
                val ability = card.staticAbilities.first() as CantAttackUnlessDefenderControlsLandType
                ability.landType shouldBe "Island"
            }
        }

        describe("Djinn of the Lamp") {
            val card = DjinnOfTheLamp

            it("should be a 5/6 Djinn with flying") {
                card.manaCost.toString() shouldBe "{5}{U}{U}"
                card.creatureStats?.basePower shouldBe 5
                card.creatureStats?.baseToughness shouldBe 6
                card.typeLine.subtypes shouldContain Subtype.DJINN
                card.keywords shouldContain Keyword.FLYING
            }
        }

        describe("Déjà Vu") {
            val card = DejaVu

            it("should return sorcery from graveyard") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<ReturnFromGraveyardEffect>()
                val effect = card.spellEffect as ReturnFromGraveyardEffect
                effect.filter shouldBe CardFilter.SorceryCard
            }
        }

        describe("Exhaustion") {
            val card = Exhaustion

            it("should prevent untapping of creatures and lands") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<SkipUntapEffect>()
                val effect = card.spellEffect as SkipUntapEffect
                effect.affectsCreatures shouldBe true
                effect.affectsLands shouldBe true
            }
        }

        describe("Flux") {
            val card = Flux

            it("should have flux effect with extra draw") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<FluxEffect>()
                val effect = card.spellEffect as FluxEffect
                effect.drawExtra shouldBe 1
            }
        }

        describe("Giant Octopus") {
            val card = GiantOctopus

            it("should be a 3/3 vanilla Octopus") {
                card.manaCost.toString() shouldBe "{3}{U}"
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 3
                card.typeLine.subtypes shouldContain Subtype.OCTOPUS
                card.keywords shouldHaveSize 0
            }
        }

        describe("Horned Turtle") {
            val card = HornedTurtle

            it("should be a 1/4 vanilla Turtle") {
                card.manaCost.toString() shouldBe "{2}{U}"
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 4
                card.typeLine.subtypes shouldContain Subtype.TURTLE
                card.keywords shouldHaveSize 0
            }
        }

        describe("Ingenious Thief") {
            val card = IngeniousThief

            it("should be a 1/1 Human Rogue with flying") {
                card.manaCost.toString() shouldBe "{1}{U}"
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.HUMAN
                card.typeLine.subtypes shouldContain Subtype.ROGUE
                card.keywords shouldContain Keyword.FLYING
            }

            it("should have ETB look at hand trigger") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
                trigger.effect.shouldBeInstanceOf<LookAtTargetHandEffect>()
            }
        }

        describe("Man-o'-War") {
            val card = ManOWar

            it("should be a 2/2 Jellyfish") {
                card.manaCost.toString() shouldBe "{2}{U}"
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.JELLYFISH
            }

            it("should have ETB bounce trigger") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
                trigger.effect.shouldBeInstanceOf<ReturnToHandEffect>()
            }
        }

        describe("Merfolk of the Pearl Trident") {
            val card = MerfolkOfThePearlTrident

            it("should be a 1/1 vanilla Merfolk") {
                card.manaCost.toString() shouldBe "{U}"
                card.cmc shouldBe 1
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.MERFOLK
                card.keywords shouldHaveSize 0
            }
        }
    }

    describe("Portal Set - Cards 61-70") {

        describe("Mystic Denial") {
            val card = MysticDenial

            it("should be an instant that counters creature or sorcery spells") {
                card.manaCost.toString() shouldBe "{1}{U}{U}"
                card.typeLine.isInstant shouldBe true
                card.spellEffect.shouldBeInstanceOf<CounterSpellWithFilterEffect>()
                val effect = card.spellEffect as CounterSpellWithFilterEffect
                effect.filter.shouldBeInstanceOf<SpellFilter.CreatureOrSorcery>()
            }
        }

        describe("Omen") {
            val card = Omen

            it("should look at top 3 with optional shuffle and draw") {
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<OmenEffect>()
                val effect = card.spellEffect as OmenEffect
                effect.lookAtCount shouldBe 3
                effect.mayShuffle shouldBe true
                effect.drawAfter shouldBe 1
            }
        }

        describe("Owl Familiar") {
            val card = OwlFamiliar

            it("should be a 1/1 Bird with flying") {
                card.manaCost.toString() shouldBe "{1}{U}"
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 1
                card.typeLine.subtypes shouldContain Subtype.BIRD
                card.keywords shouldContain Keyword.FLYING
            }

            it("should have ETB loot trigger") {
                card.triggeredAbilities shouldHaveSize 1
                val trigger = card.triggeredAbilities.first()
                trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
                trigger.effect.shouldBeInstanceOf<LootEffect>()
            }
        }

        describe("Personal Tutor") {
            val card = PersonalTutor

            it("should search for sorcery and put on top") {
                card.manaCost.toString() shouldBe "{U}"
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<SearchLibraryToTopEffect>()
                val effect = card.spellEffect as SearchLibraryToTopEffect
                effect.filter shouldBe CardFilter.SorceryCard
            }
        }

        describe("Phantom Warrior") {
            val card = PhantomWarrior

            it("should be a 2/2 unblockable Illusion Warrior") {
                card.manaCost.toString() shouldBe "{1}{U}{U}"
                card.creatureStats?.basePower shouldBe 2
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.ILLUSION
                card.typeLine.subtypes shouldContain Subtype.WARRIOR
                card.keywords shouldContain Keyword.UNBLOCKABLE
            }
        }

        describe("Prosperity") {
            val card = Prosperity

            it("should be an X spell that draws for all players") {
                card.manaCost.toString() shouldBe "{X}{U}"
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<EachPlayerDrawsXEffect>()
            }
        }

        describe("Snapping Drake") {
            val card = SnappingDrake

            it("should be a 3/2 Drake with flying") {
                card.manaCost.toString() shouldBe "{3}{U}"
                card.creatureStats?.basePower shouldBe 3
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.DRAKE
                card.keywords shouldContain Keyword.FLYING
            }
        }

        describe("Sorcerous Sight") {
            val card = SorcerousSight

            it("should look at opponent's hand and draw") {
                card.manaCost.toString() shouldBe "{U}"
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<SorcerousSightEffect>()
            }
        }

        describe("Storm Crow") {
            val card = StormCrow

            it("should be a 1/2 Bird with flying") {
                card.manaCost.toString() shouldBe "{1}{U}"
                card.creatureStats?.basePower shouldBe 1
                card.creatureStats?.baseToughness shouldBe 2
                card.typeLine.subtypes shouldContain Subtype.BIRD
                card.keywords shouldContain Keyword.FLYING
            }
        }

        describe("Symbol of Unsummoning") {
            val card = SymbolOfUnsummoning

            it("should bounce creature and draw a card") {
                card.manaCost.toString() shouldBe "{2}{U}"
                card.typeLine.isSorcery shouldBe true
                card.spellEffect.shouldBeInstanceOf<CompositeEffect>()
                val composite = card.spellEffect as CompositeEffect
                composite.effects shouldHaveSize 2
            }
        }
    }

    describe("PortalSet object") {
        it("should have 70 cards in the set") {
            PortalSet.allCards shouldHaveSize 70
        }

        it("should have correct set code") {
            PortalSet.SET_CODE shouldBe "POR"
            PortalSet.SET_NAME shouldBe "Portal"
        }

        it("should find cards by name") {
            PortalSet.getCard("Armageddon") shouldBe Armageddon
            PortalSet.getCard("Fleet-Footed Monk") shouldBe FleetFootedMonk
            PortalSet.getCard("Starlit Angel") shouldBe StarlitAngel
            PortalSet.getCard("Nonexistent") shouldBe null
        }

        it("should find cards by collector number") {
            PortalSet.getCardByNumber("5") shouldBe Armageddon
            PortalSet.getCardByNumber("15") shouldBe FleetFootedMonk
            PortalSet.getCardByNumber("30") shouldBe StarlitAngel
            PortalSet.getCardByNumber("999") shouldBe null
        }

        it("should have image URIs for all cards") {
            PortalSet.allCards.forEach { card ->
                card.metadata.imageUri shouldNotBe null
                card.metadata.imageUri shouldStartWith "https://cards.scryfall.io/"
            }
        }
    }
})
