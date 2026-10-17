package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ceremonial Groundbreaker — Streets of New Capenna #175
 * {1}{G}{W} · Artifact — Equipment
 *
 * Equipped creature gets +2/+1 and has trample.
 * Equip Citizen {1}
 * Equip {3}
 *
 * "Equip Citizen {1}" is an "Equip [quality] creature" variant (CR 702.6c): attach to target
 * Citizen creature you control, sorcery speed — the same shape as Blackblade Reforged.
 */
val CeremonialGroundbreaker = card("Ceremonial Groundbreaker") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1 and has trample.\n" +
        "Equip Citizen {1}\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 1)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    equipAbility(
        "{1}",
        quality = "Citizen",
        targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.CITIZEN),
    )

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Drew Baker"
        flavorText = "Construction in New Capenna is imbued with tradition and ritual."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a542616-aa4d-40d9-acad-47bc98bd6281.jpg?1783923091"
    }
}
