package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.AddManaEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.portal.cards.*

/**
 * The Portal set - a simplified introductory set for Magic: The Gathering.
 * Portal was released in 1997 and contains 222 cards.
 */
object PortalSet : BaseCardRegistry() {
    override val setCode: String = "POR"
    override val setName: String = "Portal"

    init {
        // Register basic lands
        registerBasicLands()

        // Register all Portal cards using the new modular card file pattern
        register(AlabasterDragon.definition, AlabasterDragon.script)
        register(AlluringScent.definition, AlluringScent.script)
        register(Anaconda.definition, Anaconda.script)
        register(AncestralMemories.definition, AncestralMemories.script)
        register(AngelicBlessing.definition, AngelicBlessing.script)
        register(Archangel.definition, Archangel.script)
        register(ArdentMilitia.definition, ArdentMilitia.script)
        register(Armageddon.definition, Armageddon.script)
        register(ArmoredPegasus.definition, ArmoredPegasus.script)
        register(ArrogantVampire.definition, ArrogantVampire.script)
        register(BeeSting.definition, BeeSting.script)
        register(Blaze.definition, Blaze.script)
        register(BogImp.definition, BogImp.script)
        register(BoilingSeas.definition, BoilingSeas.script)
        register(BorderGuard.definition, BorderGuard.script)
        register(BreathOfLife.definition, BreathOfLife.script)
        register(BurningCloak.definition, BurningCloak.script)
        register(ChargingBandits.definition, ChargingBandits.script)
        register(ChargingPaladin.definition, ChargingPaladin.script)
        register(ChargingRhino.definition, ChargingRhino.script)
        register(CloakOfFeathers.definition, CloakOfFeathers.script)
        register(CloudSpirit.definition, CloudSpirit.script)
        register(CoralEel.definition, CoralEel.script)
        register(CravenGiant.definition, CravenGiant.script)
        register(CravenKnight.definition, CravenKnight.script)
        register(CruelBargain.definition, CruelBargain.script)
        register(CruelTutor.definition, CruelTutor.script)
        register(DefiantStand.definition, DefiantStand.script)
        register(DesertDrake.definition, DesertDrake.script)
        register(DevotedHero.definition, DevotedHero.script)
        register(DjinnOfTheLamp.definition, DjinnOfTheLamp.script)
        register(DreadReaper.definition, DreadReaper.script)
        register(DrySpell.definition, DrySpell.script)
        register(EbonDragon.definition, EbonDragon.script)
        register(EliteCatWarrior.definition, EliteCatWarrior.script)
        register(ElvishRanger.definition, ElvishRanger.script)
        register(EndlessCockroaches.definition, EndlessCockroaches.script)
        register(Exhaustion.definition, Exhaustion.script)
        register(FalsePeace.definition, FalsePeace.script)
        register(FeralShadow.definition, FeralShadow.script)
        register(FireImp.definition, FireImp.script)
        register(FireSnake.definition, FireSnake.script)
        register(FireTempest.definition, FireTempest.script)
        register(Flashfires.definition, Flashfires.script)
        register(FootSoldiers.definition, FootSoldiers.script)
        register(GiantOctopus.definition, GiantOctopus.script)
        register(GiantSpider.definition, GiantSpider.script)
        register(GiftOfEstates.definition, GiftOfEstates.script)
        register(GoblinBully.definition, GoblinBully.script)
        register(GorillaWarrior.definition, GorillaWarrior.script)
        register(Gravedigger.definition, Gravedigger.script)
        register(GrizzlyBears.definition, GrizzlyBears.script)
        register(HandOfDeath.definition, HandOfDeath.script)
        register(HighlandGiant.definition, HighlandGiant.script)
        register(HillGiant.definition, HillGiant.script)
        register(HornedTurtle.definition, HornedTurtle.script)
        register(HowlingFury.definition, HowlingFury.script)
        register(HulkingCyclops.definition, HulkingCyclops.script)
        register(HulkingGoblin.definition, HulkingGoblin.script)
        register(JungleLion.definition, JungleLion.script)
        register(KnightErrant.definition, KnightErrant.script)
        register(LavaAxe.definition, LavaAxe.script)
        register(LizardWarrior.definition, LizardWarrior.script)
        register(ManOWar.definition, ManOWar.script)
        register(MerfolkOfThePearlTrident.definition, MerfolkOfThePearlTrident.script)
        register(MindRot.definition, MindRot.script)
        register(MinotaurWarrior.definition, MinotaurWarrior.script)
        register(MonstrousGrowth.definition, MonstrousGrowth.script)
        register(MoonSprite.definition, MoonSprite.script)
        register(MountainGoat.definition, MountainGoat.script)
        register(MuckRats.definition, MuckRats.script)
        register(NaturalOrder.definition, NaturalOrder.script)
        register(NaturalSpring.definition, NaturalSpring.script)
        register(NaturesCloak.definition, NaturesCloak.script)
        register(NaturesLore.definition, NaturesLore.script)
        register(NeedleStorm.definition, NeedleStorm.script)
        register(NoxiousToad.definition, NoxiousToad.script)
        register(OwlFamiliar.definition, OwlFamiliar.script)
        register(PantherWarriors.definition, PantherWarriors.script)
        register(PathOfPeace.definition, PathOfPeace.script)
        register(PersonalTutor.definition, PersonalTutor.script)
        register(PhantomWarrior.definition, PhantomWarrior.script)
        register(PrimevalForce.definition, PrimevalForce.script)
        register(Prosperity.definition, Prosperity.script)
        register(Pyroclasm.definition, Pyroclasm.script)
        register(Python.definition, Python.script)
        register(RagingCougar.definition, RagingCougar.script)
        register(RagingGoblin.definition, RagingGoblin.script)
        register(RagingMinotaur.definition, RagingMinotaur.script)
        register(RaiseDead.definition, RaiseDead.script)
        register(RedwoodTreefolk.definition, RedwoodTreefolk.script)
        register(RegalUnicorn.definition, RegalUnicorn.script)
        register(RowanTreefolk.definition, RowanTreefolk.script)
        register(SacredKnight.definition, SacredKnight.script)
        register(SacredNectar.definition, SacredNectar.script)
        register(ScorchingSpear.definition, ScorchingSpear.script)
        register(SeasonedMarshal.definition, SeasonedMarshal.script)
        register(SerpentAssassin.definition, SerpentAssassin.script)
        register(SerpentWarrior.definition, SerpentWarrior.script)
        register(SkeletalCrocodile.definition, SkeletalCrocodile.script)
        register(SkeletalSnake.definition, SkeletalSnake.script)
        register(SoulShred.definition, SoulShred.script)
        register(SpinedWurm.definition, SpinedWurm.script)
        register(SpiritualGuardian.definition, SpiritualGuardian.script)
        register(SpottedGriffin.definition, SpottedGriffin.script)
        register(StalkingTiger.definition, StalkingTiger.script)
        register(StarlitAngel.definition, StarlitAngel.script)
        register(StoneRain.definition, StoneRain.script)
        register(StormCrow.definition, StormCrow.script)
        register(SylvanTutor.definition, SylvanTutor.script)
        register(SymbolOfUnsummoning.definition, SymbolOfUnsummoning.script)
        register(ThunderingWurm.definition, ThunderingWurm.script)
        register(TouchOfBrilliance.definition, TouchOfBrilliance.script)
        register(UndyingBeast.definition, UndyingBeast.script)
        register(VampiricFeast.definition, VampiricFeast.script)
        register(VampiricTouch.definition, VampiricTouch.script)
        register(VenerableMonk.definition, VenerableMonk.script)
        register(Vengeance.definition, Vengeance.script)
        register(VolcanicDragon.definition, VolcanicDragon.script)
        register(VolcanicHammer.definition, VolcanicHammer.script)
        register(WallOfGranite.definition, WallOfGranite.script)
        register(WallOfSwords.definition, WallOfSwords.script)
        register(WarriorsCharge.definition, WarriorsCharge.script)
        register(WhiptailWurm.definition, WhiptailWurm.script)
        register(WillowDryad.definition, WillowDryad.script)
        register(WindDrake.definition, WindDrake.script)
        register(WindsOfChange.definition, WindsOfChange.script)
        register(WoodElves.definition, WoodElves.script)
        register(WrathOfGod.definition, WrathOfGod.script)
    }

    // =========================================================================
    // Basic Lands
    // =========================================================================

    private fun registerBasicLands() {
        registerForest()
        registerIsland()
        registerMountain()
        registerPlains()
        registerSwamp()
    }

    private fun registerForest() {
        val definition = CardDefinition.basicLand("Forest", Subtype.FOREST)
        val script = cardScript("Forest") {
            manaAbility(AddManaEffect(Color.GREEN))
        }
        register(definition, script)
    }

    private fun registerIsland() {
        val definition = CardDefinition.basicLand("Island", Subtype.ISLAND)
        val script = cardScript("Island") {
            manaAbility(AddManaEffect(Color.BLUE))
        }
        register(definition, script)
    }

    private fun registerMountain() {
        val definition = CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN)
        val script = cardScript("Mountain") {
            manaAbility(AddManaEffect(Color.RED))
        }
        register(definition, script)
    }

    private fun registerPlains() {
        val definition = CardDefinition.basicLand("Plains", Subtype.PLAINS)
        val script = cardScript("Plains") {
            manaAbility(AddManaEffect(Color.WHITE))
        }
        register(definition, script)
    }

    private fun registerSwamp() {
        val definition = CardDefinition.basicLand("Swamp", Subtype.SWAMP)
        val script = cardScript("Swamp") {
            manaAbility(AddManaEffect(Color.BLACK))
        }
        register(definition, script)
    }
}
