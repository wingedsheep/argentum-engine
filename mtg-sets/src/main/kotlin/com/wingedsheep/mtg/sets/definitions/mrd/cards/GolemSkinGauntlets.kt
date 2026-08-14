package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.values.AttachmentKind
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Golem-Skin Gauntlets — Mirrodin #181
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0 for each Equipment attached to it.
 * Equip {2}
 *
 * The bonus counts Equipment on the *equipped creature*, not on the Gauntlets — so it reads through
 * the source's attachment link with
 * [DynamicAmounts.attachmentsOnEnchantedCreature]`(AttachmentKind.EQUIPMENT)`. Two consequences
 * worth spelling out, both of them correct per the card's rulings:
 *
 *  - The Gauntlets count themselves, so a lone pair is +1/+0, not +0/+0.
 *  - Two pairs of Gauntlets on one creature each see *both*, so the total is +4/+0 rather than
 *    +2/+0 — each is a separate static ability contributing its own recomputed bonus.
 *
 * The count is read off *projected* subtypes, so a permanent animated into or out of being an
 * Equipment is counted correctly, and Auras attached to the same creature never are.
 */
val GolemSkinGauntlets = card("Golem-Skin Gauntlets") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 for each Equipment attached to it.\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.attachmentsOnEnchantedCreature(AttachmentKind.EQUIPMENT),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9519f2a-e98d-4f84-885c-24a9849a996d.jpg?1783944519"
    }
}
