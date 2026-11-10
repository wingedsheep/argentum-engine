package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Murderer's Axe
 * {4}
 * Artifact — Equipment
 * Equipped creature gets +2/+2.
 * Equip—Discard a card.
 *
 * The equip cost is a non-mana cost, modeled as an equip-flagged, sorcery-speed activated
 * ability whose cost discards a card (cf. the discard half of Bloodthorn Flail).
 */
val MurderersAxe = card("Murderer's Axe") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\n" +
        "Equip—Discard a card."

    // Equipped creature gets +2/+2.
    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }

    // Equip—Discard a card.
    activatedAbility {
        isEquipAbility = true
        cost = Costs.DiscardCard
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        description = "Equip—Discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "259"
        artist = "Winona Nelson"
        flavorText = "Though the first who raised it in a fit of passion is long dead, the axe still carries " +
            "memories of bitter scorn."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/696dcb79-4915-424f-ba8f-5363ae3395a6.jpg?1783937707"
    }
}
