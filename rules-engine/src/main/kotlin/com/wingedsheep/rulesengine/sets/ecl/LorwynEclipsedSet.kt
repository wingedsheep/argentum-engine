package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.ecl.cards.AdeptWatershaper
import com.wingedsheep.rulesengine.sets.ecl.cards.AjaniOutlandChaperone
import com.wingedsheep.rulesengine.sets.ecl.cards.AquitectsDefenses
import com.wingedsheep.rulesengine.sets.ecl.cards.AppealToEirdu
import com.wingedsheep.rulesengine.sets.ecl.cards.BarkOfDoran
import com.wingedsheep.rulesengine.sets.ecl.cards.Blossombind
import com.wingedsheep.rulesengine.sets.ecl.cards.BrigidClachansHeart
import com.wingedsheep.rulesengine.sets.ecl.cards.BurdenedStoneback
import com.wingedsheep.rulesengine.sets.ecl.cards.ChangelingWayfinder
import com.wingedsheep.rulesengine.sets.ecl.cards.ClachanFestival
import com.wingedsheep.rulesengine.sets.ecl.cards.CribSwap
import com.wingedsheep.rulesengine.sets.ecl.cards.CuriousColossus
import com.wingedsheep.rulesengine.sets.ecl.cards.DisruptorOfCurrents
import com.wingedsheep.rulesengine.sets.ecl.cards.EncumberedReejerey
import com.wingedsheep.rulesengine.sets.ecl.cards.FlockImpostor
import com.wingedsheep.rulesengine.sets.ecl.cards.GallantFowlknight
import com.wingedsheep.rulesengine.sets.ecl.cards.Glamermite
import com.wingedsheep.rulesengine.sets.ecl.cards.GoldmeadowNomad
import com.wingedsheep.rulesengine.sets.ecl.cards.GravelgillScoundrel
import com.wingedsheep.rulesengine.sets.ecl.cards.KeepOut
import com.wingedsheep.rulesengine.sets.ecl.cards.Kinbinding
import com.wingedsheep.rulesengine.sets.ecl.cards.KinscaerSentry
import com.wingedsheep.rulesengine.sets.ecl.cards.KulrathMystic
import com.wingedsheep.rulesengine.sets.ecl.cards.LiminalHold
import com.wingedsheep.rulesengine.sets.ecl.cards.RiverguardsReflexes
import com.wingedsheep.rulesengine.sets.ecl.cards.RooftopPercher
import com.wingedsheep.rulesengine.sets.ecl.cards.ShoreLurker
import com.wingedsheep.rulesengine.sets.ecl.cards.SunDappledCelebrant
import com.wingedsheep.rulesengine.sets.ecl.cards.TimidShieldbearer
import com.wingedsheep.rulesengine.sets.ecl.cards.TributaryVaulter
import com.wingedsheep.rulesengine.sets.ecl.cards.WanderbrinePreacher
import com.wingedsheep.rulesengine.sets.ecl.cards.WanderbrineTrapper

object LorwynEclipsedSet : BaseCardRegistry() {
    override val setCode: String = "ECL"
    override val setName: String = "Lorwyn Eclipsed"

    init {
        register(AdeptWatershaper.definition, AdeptWatershaper.script)
        register(AjaniOutlandChaperone.definition, AjaniOutlandChaperone.script)
        register(AquitectsDefenses.definition, AquitectsDefenses.script)
        register(AppealToEirdu.definition, AppealToEirdu.script)
        register(BarkOfDoran.definition, BarkOfDoran.script)
        register(Blossombind.definition, Blossombind.script)
        register(BrigidClachansHeart.definition, BrigidClachansHeart.script)
        // Also register back face script for Brigid
        registerScript(BrigidClachansHeart.backScript)
        register(BurdenedStoneback.definition, BurdenedStoneback.script)
        register(ChangelingWayfinder.definition, ChangelingWayfinder.script)
        register(ClachanFestival.definition, ClachanFestival.script)
        register(CribSwap.definition, CribSwap.script)
        register(CuriousColossus.definition, CuriousColossus.script)
        register(DisruptorOfCurrents.definition, DisruptorOfCurrents.script)
        register(EncumberedReejerey.definition, EncumberedReejerey.script)
        register(FlockImpostor.definition, FlockImpostor.script)
        register(GallantFowlknight.definition, GallantFowlknight.script)
        register(Glamermite.definition, Glamermite.script)
        register(GoldmeadowNomad.definition, GoldmeadowNomad.script)
        register(GravelgillScoundrel.definition, GravelgillScoundrel.script)
        register(KeepOut.definition, KeepOut.script)
        register(Kinbinding.definition, Kinbinding.script)
        register(KinscaerSentry.definition, KinscaerSentry.script)
        register(KulrathMystic.definition, KulrathMystic.script)
        register(LiminalHold.definition, LiminalHold.script)
        register(RiverguardsReflexes.definition, RiverguardsReflexes.script)
        register(RooftopPercher.definition, RooftopPercher.script)
        register(ShoreLurker.definition, ShoreLurker.script)
        register(SunDappledCelebrant.definition, SunDappledCelebrant.script)
        register(TimidShieldbearer.definition, TimidShieldbearer.script)
        register(TributaryVaulter.definition, TributaryVaulter.script)
        register(WanderbrinePreacher.definition, WanderbrinePreacher.script)
        register(WanderbrineTrapper.definition, WanderbrineTrapper.script)
    }
}
