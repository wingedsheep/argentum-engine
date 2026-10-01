package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Prosthetic Injector
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +0/+2 and has toxic 1.
 * Equip {1}
 *
 * "Has toxic 1" is a static grant of the projected `TOXIC_1` keyword — the form printed toxic
 * projects as, so it sums with any toxic the creature already has (CR 702.164b).
 */
val ProstheticInjector = card("Prosthetic Injector") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+2 and has toxic 1. (Players dealt combat damage by equipped creature also get a poison counter.)\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(0, 2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword("${Keyword.TOXIC.name}_1", Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Zezhou Chen"
        flavorText = "Why carry a subject to a surgical lab when you can bring the surgical lab to them?"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a1beec-bafe-4243-b91e-040a88fb0e95.jpg?1783917987"
    }
}
