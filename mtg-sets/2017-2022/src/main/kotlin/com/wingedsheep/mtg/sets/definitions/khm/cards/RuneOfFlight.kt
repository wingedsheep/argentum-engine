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
 * Rune of Flight {1}{U}
 * Enchantment — Aura Rune
 *
 * Enchant permanent
 * When this Aura enters, draw a card.
 * As long as enchanted permanent is a creature, it has flying.
 * As long as enchanted permanent is an Equipment, it has "Equipped creature has flying."
 *
 * On an Equipment the Aura grants the Equipment a static ability, so the Equipment is what gives
 * flying to whichever creature it's attached to — re-equipping moves it without moving the Rune.
 */
val RuneOfFlight = card("Rune of Flight") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura Rune"
    oracleText = "Enchant permanent\n" +
        "When this Aura enters, draw a card.\n" +
        "As long as enchanted permanent is a creature, it has flying.\n" +
        "As long as enchanted permanent is an Equipment, it has \"Equipped creature has flying.\""

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantStaticAbility(GrantKeyword(Keyword.FLYING)),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5c1a271-2107-462a-9d4a-9c7fbcfa8d77.jpg?1783928257"
    }
}
