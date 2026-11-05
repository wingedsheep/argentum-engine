package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kumena's Speaker
 * {G}
 * Creature — Merfolk Shaman
 * 1/1
 * This creature gets +1/+1 as long as you control another Merfolk or an Island.
 *
 * "Another" binds only to the Merfolk half (the source is itself a Merfolk), so that half
 * excludes the source while the Island half doesn't need to. Both nouns name any *permanent*
 * with the subtype, not just creatures/lands.
 */
val KumenasSpeaker = card("Kumena's Speaker") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Shaman"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 as long as you control another Merfolk or an Island."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.Any(
                Conditions.YouControl(
                    GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK),
                    excludeSelf = true
                ),
                Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.ISLAND))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Johann Bodin"
        flavorText = "\"The same power that drives the river ever onward flows through the roots and branches—to me.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52e96ddf-ac9e-4cc5-8c9a-afee421d99a6.jpg?1783935723"
    }
}
