package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Transmogrant's Crown — The Brothers' War #125
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +2/+0.
 * Whenever equipped creature dies, draw a card.
 * Equip {2} or {B}
 *
 * "Equip {2} or {B}" is two equip abilities with the same effect, so it lowers to two
 * [equipAbility] calls (the same shape Blackblade Reforged uses for its two equip costs).
 */
val TransmograntsCrown = card("Transmogrant's Crown") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\nWhenever equipped creature dies, draw a card.\nEquip {2} or {B}"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DrawCards(1)
    }

    equipAbility("{2}")
    equipAbility("{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Nino Vecia"
        flavorText = "Fallaji soldiers hated seeing loved ones' faces among the transmogrants. Ashnod devised a solution."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc913ed6-3032-4b45-a6c1-6a4208a7e00a.jpg?1783920074"
    }
}
