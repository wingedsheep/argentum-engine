package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.definitions.akh.AmonkhetSet
import com.wingedsheep.mtg.sets.definitions.ala.ShardsOfAlaraSet
import com.wingedsheep.mtg.sets.definitions.arn.ArabianNightsSet
import com.wingedsheep.mtg.sets.definitions.avr.AvacynRestoredSet
import com.wingedsheep.mtg.sets.definitions.bfz.BattleForZendikarSet
import com.wingedsheep.mtg.sets.definitions.big.TheBigScoreSet
import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.blc.BloomburrowCommanderSet
import com.wingedsheep.mtg.sets.definitions.bro.BrothersWarSet
import com.wingedsheep.mtg.sets.definitions.c15.Commander2015Set
import com.wingedsheep.mtg.sets.definitions.c17.Commander2017Set
import com.wingedsheep.mtg.sets.definitions.cmd.Commander2011Set
import com.wingedsheep.mtg.sets.definitions.con.ConfluxSet
import com.wingedsheep.mtg.sets.definitions.dft.AetherdriftSet
import com.wingedsheep.mtg.sets.definitions.dtk.DragonsOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.dom.DominariaSet
import com.wingedsheep.mtg.sets.definitions.dmu.DominariaUnitedSet
import com.wingedsheep.mtg.sets.definitions.dsk.DuskmournSet
import com.wingedsheep.mtg.sets.definitions.gpt.GuildpactSet
import com.wingedsheep.mtg.sets.definitions.m10.Magic2010Set
import com.wingedsheep.mtg.sets.definitions.m12.Magic2012Set
import com.wingedsheep.mtg.sets.definitions.m14.Magic2014Set
import com.wingedsheep.mtg.sets.definitions.m21.CoreSet2021Set
import com.wingedsheep.mtg.sets.definitions.ody.OdysseySet
import com.wingedsheep.mtg.sets.definitions.tsp.TimeSpiralSet
import com.wingedsheep.mtg.sets.definitions.eoe.EdgeOfEternitiesSet
import com.wingedsheep.mtg.sets.definitions.exo.ExodusSet
import com.wingedsheep.mtg.sets.definitions.fdn.FoundationsSet
import com.wingedsheep.mtg.sets.definitions.frf.FateReforgedSet
import com.wingedsheep.mtg.sets.definitions.fin.FinalFantasySet
import com.wingedsheep.mtg.sets.definitions.inr.InnistradRemasteredSet
import com.wingedsheep.mtg.sets.definitions.inv.InvasionSet
import com.wingedsheep.mtg.sets.definitions.jud.JudgmentSet
import com.wingedsheep.mtg.sets.definitions.mid.InnistradMidnightHuntSet
import com.wingedsheep.mtg.sets.definitions.vow.InnistradCrimsonVowSet
import com.wingedsheep.mtg.sets.definitions.khm.KaldheimSet
import com.wingedsheep.mtg.sets.definitions.ktk.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.leg.LegendsSet
import com.wingedsheep.mtg.sets.definitions.lgn.LegionsSet
import com.wingedsheep.mtg.sets.definitions.lrw.LorwynSet
import com.wingedsheep.mtg.sets.definitions.ecl.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.lci.LostCavernsOfIxalanSet
import com.wingedsheep.mtg.sets.definitions.ltr.LordOfTheRingsSet
import com.wingedsheep.mtg.sets.definitions.mbs.MirrodinBesiegedSet
import com.wingedsheep.mtg.sets.definitions.mir.MirageSet
import com.wingedsheep.mtg.sets.definitions.mrd.MirrodinSet
import com.wingedsheep.mtg.sets.definitions.mkm.MurdersAtKarlovManorSet
import com.wingedsheep.mtg.sets.definitions.mom.MarchOfTheMachineSet
import com.wingedsheep.mtg.sets.definitions.ncc.NewCapennaCommanderSet
import com.wingedsheep.mtg.sets.definitions.one.PhyrexiaAllWillBeOneSet
import com.wingedsheep.mtg.sets.definitions.ons.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.p02.PortalSecondAgeSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.definitions.pz2.TreasureChestSet
import com.wingedsheep.mtg.sets.definitions.rix.RivalsOfIxalanSet
import com.wingedsheep.mtg.sets.definitions.xln.IxalanSet
import com.wingedsheep.mtg.sets.definitions.roe.RiseOfTheEldraziSet
import com.wingedsheep.mtg.sets.definitions.scg.ScourgeSet
import com.wingedsheep.mtg.sets.definitions.soi.ShadowsOverInnistradSet
import com.wingedsheep.mtg.sets.definitions.stx.StrixhavenSchoolOfMagesSet
import com.wingedsheep.mtg.sets.definitions.tmp.TempestSet
import com.wingedsheep.mtg.sets.definitions.usg.UrzasSagaSet
import com.wingedsheep.mtg.sets.definitions.vis.VisionsSet
import com.wingedsheep.mtg.sets.definitions.wth.WeatherlightSet
import com.wingedsheep.mtg.sets.definitions.otj.OutlawsOfThunderJunctionSet
import com.wingedsheep.mtg.sets.definitions.spm.SpiderManSet
import com.wingedsheep.mtg.sets.definitions.tdm.TarkirDragonstormSet
import com.wingedsheep.mtg.sets.definitions.tla.AvatarTheLastAirbenderSet
import com.wingedsheep.mtg.sets.definitions.tmt.TeenageMutantNinjaTurtlesSet
import com.wingedsheep.mtg.sets.definitions.war.WarOfTheSparkSet
import com.wingedsheep.mtg.sets.definitions.woe.WildsOfEldrainSet
import com.wingedsheep.mtg.sets.definitions.wwk.WorldwakeSet
import com.wingedsheep.sdk.model.MtgSet

/**
 * Single source of truth for all known MTG sets.
 *
 * Adding a new set: implement [MtgSet] and append the object to [all].
 * The game-server, gym, and tests discover sets through this catalog —
 * no other registration is required.
 */
object MtgSetCatalog {

    val all: List<MtgSet> = listOf(
        AlphaSet,
        ArabianNightsSet,
        LegendsSet,
        PortalSet,
        PortalSecondAgeSet,
        MirageSet,
        VisionsSet,
        WeatherlightSet,
        TempestSet,
        ExodusSet,
        UrzasSagaSet,
        InvasionSet,
        LorwynSet,
        KaldheimSet,
        OnslaughtSet,
        ScourgeSet,
        LegionsSet,
        KhansOfTarkirSet,
        FateReforgedSet,
        DragonsOfTarkirSet,
        MirrodinSet,
        MirrodinBesiegedSet,
        RiseOfTheEldraziSet,
        AvacynRestoredSet,
        DominariaSet,
        DominariaUnitedSet,
        PhyrexiaAllWillBeOneSet,
        BrothersWarSet,
        Commander2011Set,
        Commander2015Set,
        TreasureChestSet,
        Commander2017Set,
        InnistradRemasteredSet,
        InnistradMidnightHuntSet,
        InnistradCrimsonVowSet,
        MarchOfTheMachineSet,
        NewCapennaCommanderSet,
        MurdersAtKarlovManorSet,
        WildsOfEldrainSet,
        IxalanSet,
        RivalsOfIxalanSet,
        LostCavernsOfIxalanSet,
        BloomburrowSet,
        BloomburrowCommanderSet,
        FoundationsSet,
        FinalFantasySet,
        DuskmournSet,
        AetherdriftSet,
        EdgeOfEternitiesSet,
        LorwynEclipsedSet,
        OutlawsOfThunderJunctionSet,
        TheBigScoreSet,
        SpiderManSet,
        TarkirDragonstormSet,
        AvatarTheLastAirbenderSet,
        TeenageMutantNinjaTurtlesSet,
        WarOfTheSparkSet,
        OdysseySet,
        JudgmentSet,
        GuildpactSet,
        TimeSpiralSet,
        ConfluxSet,
        ShardsOfAlaraSet,
        Magic2010Set,
        Magic2012Set,
        Magic2014Set,
        CoreSet2021Set,
        BattleForZendikarSet,
        AmonkhetSet,
        ShadowsOverInnistradSet,
        WorldwakeSet,
        LordOfTheRingsSet,
        StrixhavenSchoolOfMagesSet,
    )

    private val byCode: Map<String, MtgSet> = all.associateBy { it.code }

    fun byCode(code: String): MtgSet? = byCode[code]

    fun requireByCode(code: String): MtgSet =
        byCode(code) ?: throw IllegalArgumentException("Unknown set code: $code")
}
