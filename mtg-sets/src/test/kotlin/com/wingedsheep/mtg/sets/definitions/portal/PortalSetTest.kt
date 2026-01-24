package com.wingedsheep.mtg.sets.definitions.portal

import com.wingedsheep.mtg.sets.definitions.portal.cards.*
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
                card.spellEffect.shouldBeInstanceOf<GainLifePerAttackerEffect>()
                val effect = card.spellEffect as GainLifePerAttackerEffect
                effect.lifePerAttacker shouldBe 3
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

    describe("PortalSet object") {
        it("should have 20 cards in the set") {
            PortalSet.allCards shouldHaveSize 20
        }

        it("should have correct set code") {
            PortalSet.SET_CODE shouldBe "POR"
            PortalSet.SET_NAME shouldBe "Portal"
        }

        it("should find cards by name") {
            PortalSet.getCard("Armageddon") shouldBe Armageddon
            PortalSet.getCard("Fleet-Footed Monk") shouldBe FleetFootedMonk
            PortalSet.getCard("Nonexistent") shouldBe null
        }

        it("should find cards by collector number") {
            PortalSet.getCardByNumber("5") shouldBe Armageddon
            PortalSet.getCardByNumber("15") shouldBe FleetFootedMonk
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
