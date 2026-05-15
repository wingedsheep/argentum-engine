package com.wingedsheep.mtg.sets

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
import com.wingedsheep.mtg.sets.definitions.ktk.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.lgn.LegionsSet
import com.wingedsheep.mtg.sets.definitions.ecl.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.lci.LostCavernsOfIxalanSet
import com.wingedsheep.mtg.sets.definitions.mkm.MurdersAtKarlovManorSet
import com.wingedsheep.mtg.sets.definitions.mom.MarchOfTheMachineSet
import com.wingedsheep.mtg.sets.definitions.one.PhyrexiaAllWillBeOneSet
import com.wingedsheep.mtg.sets.definitions.ons.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.definitions.scg.ScourgeSet
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
        PortalSet,
        InvasionSet,
        OnslaughtSet,
        ScourgeSet,
        LegionsSet,
        KhansOfTarkirSet,
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
