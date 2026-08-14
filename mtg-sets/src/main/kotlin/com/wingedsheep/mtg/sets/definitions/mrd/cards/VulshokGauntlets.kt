package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Vulshok Gauntlets — Mirrodin #273
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +4/+2 and doesn't untap during its controller's untap step.
 * Equip {3}
 *
 * Modelling notes:
 * - The two halves land in different layers — the +4/+2 in layer 7c, the untap restriction as an
 *   [AbilityFlag.DOESNT_UNTAP] grant — so they are two static abilities over the same
 *   [Filters.EquippedCreature] set. Attachment is re-read continuously either way, so unattaching
 *   the Gauntlets frees the creature to untap on its controller's very next untap step.
 * - The restriction is permanent while attached, not a one-shot "next untap step" (contrast
 *   Stitcher's Graft, whose attack rider is duration-bounded).
 */
val VulshokGauntlets = card("Vulshok Gauntlets") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+2 and doesn't untap during its controller's untap step.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(+4, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "273"
        artist = "Richard Sardinha"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14a04bbd-1d07-4b11-aa54-01790b9dd3a0.jpg?1783944496"
    }
}
