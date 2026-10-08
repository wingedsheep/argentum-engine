package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Audacity
 * {G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+0 and has trample.
 * When this Aura is put into a graveyard from the battlefield, draw a card.
 *
 * Reach for the Sky's shape: static [ModifyStats] + static [GrantKeyword] on the enchanted
 * creature, and a self `dies()` draw trigger (CR 700.4: "dies" is "put into a graveyard from the
 * battlefield", which reads the same for a noncreature permanent).
 */
val Audacity = card("Audacity") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+0 and has trample. (It can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "When this Aura is put into a graveyard from the battlefield, draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Rudy Siswanto"
        flavorText = "Bravery outlives the brave."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40b0813a-38cf-4a07-81d6-91d24af8b549.jpg"
    }
}
