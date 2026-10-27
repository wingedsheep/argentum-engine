package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rune of Speed {1}{R}
 * Enchantment — Aura Rune
 *
 * Enchant permanent
 * When this Aura enters, draw a card.
 * As long as enchanted permanent is a creature, it gets +1/+0 and has haste.
 * As long as enchanted permanent is an Equipment, it has "Equipped creature gets +1/+0 and has haste."
 *
 * On an Equipment the Aura grants the Equipment a static ability, so the Equipment is what pumps
 * whichever creature it's attached to — re-equipping moves the bonus without moving the Rune.
 */
val RuneOfSpeed = card("Rune of Speed") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Rune"
    oracleText = "Enchant permanent\n" +
        "When this Aura enters, draw a card.\n" +
        "As long as enchanted permanent is a creature, it gets +1/+0 and has haste.\n" +
        "As long as enchanted permanent is an Equipment, it has \"Equipped creature gets +1/+0 and has haste.\""

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(listOf(ModifyStats(1, 0), GrantKeyword(Keyword.HASTE))),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantStaticAbility(
                CompositeStaticAbility(listOf(ModifyStats(1, 0), GrantKeyword(Keyword.HASTE)))
            ),
            condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47faee66-b274-466a-890e-bb396dda943d.jpg?1783928225"
    }
}
