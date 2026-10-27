package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rune of Sustenance {1}{W}
 * Enchantment — Aura Rune
 *
 * Enchant permanent
 * When this Aura enters, draw a card.
 * As long as enchanted permanent is a creature, it has lifelink.
 * As long as enchanted permanent is an Equipment, it has "Equipped creature has lifelink."
 *
 * On an Equipment the Aura grants the Equipment a static ability, so the Equipment is what gives
 * lifelink to whichever creature it's attached to — re-equipping moves it without moving the Rune.
 */
val RuneOfSustenance = card("Rune of Sustenance") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura Rune"
    oracleText = "Enchant permanent\n" +
        "When this Aura enters, draw a card.\n" +
        "As long as enchanted permanent is a creature, it has lifelink.\n" +
        "As long as enchanted permanent is an Equipment, it has \"Equipped creature has lifelink.\""

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantStaticAbility(GrantKeyword(Keyword.LIFELINK)),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e31b716-f325-445a-9098-9ca75d7b35a4.jpg?1783928279"
    }
}
