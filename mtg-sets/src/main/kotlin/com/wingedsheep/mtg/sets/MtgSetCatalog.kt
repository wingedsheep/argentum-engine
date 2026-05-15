package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.definitions.avr.AvacynRestoredSet
import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.blc.BloomburrowCommanderSet
import com.wingedsheep.mtg.sets.definitions.bro.BrothersWarSet
import com.wingedsheep.mtg.sets.definitions.dft.AetherdriftSet
import com.wingedsheep.mtg.sets.definitions.dom.DominariaSet
import com.wingedsheep.mtg.sets.definitions.dmu.DominariaUnitedSet
import com.wingedsheep.mtg.sets.definitions.dsk.DuskmournSet
import com.wingedsheep.mtg.sets.definitions.eoe.EdgeOfEternitiesSet
import com.wingedsheep.mtg.sets.definitions.fdn.FoundationsSet
import com.wingedsheep.mtg.sets.definitions.fin.FinalFantasySet
import com.wingedsheep.mtg.sets.definitions.inr.InnistradRemasteredSet
import com.wingedsheep.mtg.sets.definitions.inv.InvasionSet
import com.wingedsheep.mtg.sets.definitions.mid.InnistradMidnightHuntSet
import com.wingedsheep.mtg.sets.definitions.vow.InnistradCrimsonVowSet
import com.wingedsheep.mtg.sets.definitions.khm.KaldheimSet
import com.wingedsheep.mtg.sets.definitions.ktk.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lgn.LegionsSet
import com.wingedsheep.mtg.sets.definitions.lrw.LorwynSet
import com.wingedsheep.mtg.sets.definitions.ecl.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.lci.LostCavernsOfIxalanSet
import com.wingedsheep.mtg.sets.definitions.mbs.MirrodinBesiegedSet
import com.wingedsheep.mtg.sets.definitions.mrd.MirrodinSet
import com.wingedsheep.mtg.sets.definitions.mkm.MurdersAtKarlovManorSet
import com.wingedsheep.mtg.sets.definitions.mom.MarchOfTheMachineSet
import com.wingedsheep.mtg.sets.definitions.om1.OmenpathsSet
import com.wingedsheep.mtg.sets.definitions.one.PhyrexiaAllWillBeOneSet
import com.wingedsheep.mtg.sets.definitions.ons.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.definitions.roe.RiseOfTheEldraziSet
import com.wingedsheep.mtg.sets.definitions.scg.ScourgeSet
import com.wingedsheep.mtg.sets.definitions.tmp.TempestSet
import com.wingedsheep.mtg.sets.definitions.usg.UrzasSagaSet
import com.wingedsheep.mtg.sets.definitions.otj.OutlawsOfThunderJunctionSet
import com.wingedsheep.mtg.sets.definitions.spm.SpiderManSet
import com.wingedsheep.mtg.sets.definitions.tdm.TarkirDragonstormSet
import com.wingedsheep.mtg.sets.definitions.tla.AvatarTheLastAirbenderSet
import com.wingedsheep.mtg.sets.definitions.war.WarOfTheSparkSet
import com.wingedsheep.mtg.sets.definitions.woe.WildsOfEldrainSet
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
        PortalSet,
        TempestSet,
        UrzasSagaSet,
        InvasionSet,
        LorwynSet,
        KaldheimSet,
        OnslaughtSet,
        ScourgeSet,
        LegionsSet,
        KhansOfTarkirSet,
        MirrodinSet,
        MirrodinBesiegedSet,
        RiseOfTheEldraziSet,
        AvacynRestoredSet,
        DominariaSet,
        DominariaUnitedSet,
        PhyrexiaAllWillBeOneSet,
        BrothersWarSet,
        InnistradRemasteredSet,
        InnistradMidnightHuntSet,
        InnistradCrimsonVowSet,
        MarchOfTheMachineSet,
        MurdersAtKarlovManorSet,
        WildsOfEldrainSet,
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
        OmenpathsSet,
        SpiderManSet,
        TarkirDragonstormSet,
        AvatarTheLastAirbenderSet,
        WarOfTheSparkSet,
    )

    private val byCode: Map<String, MtgSet> = all.associateBy { it.code }

    fun byCode(code: String): MtgSet? = byCode[code]

    fun requireByCode(code: String): MtgSet =
        byCode(code) ?: throw IllegalArgumentException("Unknown set code: $code")
}
