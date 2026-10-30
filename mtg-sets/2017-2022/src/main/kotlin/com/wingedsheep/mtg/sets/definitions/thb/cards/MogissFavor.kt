package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mogis's Favor
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/-1.
 * Escape—{2}{B}, Exile two other cards from your graveyard.
 *
 * Escape Velocity's shape: an Aura with a static stat change and an escape keyword ability. Escaped
 * from the graveyard it is an ordinary Aura spell that targets a creature as it is cast; once it
 * hits the graveyard again it can escape again.
 */
val MogissFavor = card("Mogis's Favor") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/-1.\n" +
        "Escape—{2}{B}, Exile two other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(+2, -1)
    }

    keywordAbility(KeywordAbility.escape("{2}{B}", Costs.additional.ExileOtherCards(2)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Uriah Voth"
        flavorText = "Rage-crowned Mogis swiftly rewards the virtue of brutality."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de6bf7fb-11ca-4878-8072-cebaa763a549.jpg?1783931562"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
