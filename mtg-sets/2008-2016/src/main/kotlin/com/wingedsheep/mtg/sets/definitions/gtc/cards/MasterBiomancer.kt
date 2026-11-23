package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAs
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master Biomancer — Gatecrash #176
 * {2}{G}{U} · Creature — Elf Wizard · 2/4
 *
 * Each other creature you control enters with a number of additional +1/+1 counters on it equal
 * to this creature's power and as a Mutant in addition to its other types.
 *
 * Two "each other" entry replacements on the same group: the counters (Arwen, Weaver of Hope's
 * shape, the count read off the Biomancer as the creature enters) and the Mutant subtype
 * ([EntersAs], a permanent layer-4 addition stamped as the creature arrives).
 */
val MasterBiomancer = card("Master Biomancer") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Wizard"
    power = 2
    toughness = 4
    oracleText = "Each other creature you control enters with a number of additional +1/+1 counters " +
        "on it equal to this creature's power and as a Mutant in addition to its other types."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.powerOf(EffectTarget.GrantingSource),
            otherOnly = true,
        )
    )
    replacementEffect(
        EntersAs(
            characteristics = EntersWithCharacteristics(addedSubtypes = setOf(Subtype("Mutant"))),
            otherOnly = true,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "176"
        artist = "Willian Murai"
        flavorText = "\"Nature is a wonderful engineer but works far too slowly for my tastes.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a47da7c-80f3-4b98-aaac-778c34a35cb6.jpg?1783940105"
        ruling("2013-01-24", "To determine how many additional +1/+1 counters a creature enters with, use Master Biomancer's power as that creature is entering.")
    }
}
