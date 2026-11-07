package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cathar's Shield — Eldritch Moon #192
 * {0} · Artifact — Equipment
 *
 * Equipped creature gets +0/+3 and has vigilance.
 * Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)
 *
 * Accorder's Shield's functional twin: two statics over [GroupFilter.attachedCreature] plus the
 * printed equip ability, which `equipAbility` lowers into the sorcery-speed attach (CR 702.6).
 */
val CatharsShield = card("Cathar's Shield") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+3 and has vigilance.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(0, 3, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature())
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Mark Zug"
        flavorText = "\"A leader can inspire courage, but it is the smiths and healers who truly keep hope alive.\"\n—Thalia, heretic cathar"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61c18ffa-ffff-4dc4-9db0-d5f183c35e17.jpg"
    }
}
