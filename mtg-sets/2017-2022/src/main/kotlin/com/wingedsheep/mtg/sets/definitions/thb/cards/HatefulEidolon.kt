package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hateful Eidolon
 * {B}
 * Enchantment Creature — Spirit
 * 1/2
 * Lifelink
 * Whenever an enchanted creature dies, draw a card for each Aura you controlled that was attached
 * to it.
 *
 * "Enchanted" and the Aura count both read the dead creature's last-known information: an
 * opponent's Aura makes it trigger, but only the Auras you controlled draw cards.
 */
val HatefulEidolon = card("Hateful Eidolon") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Spirit"
    power = 1
    toughness = 2
    oracleText = "Lifelink\nWhenever an enchanted creature dies, draw a card for each Aura you controlled " +
        "that was attached to it."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.enchanted()).dies()
        effect = Effects.DrawCards(DynamicAmounts.aurasAttachedToTriggering(Player.You))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Daniel Ljunggren"
        flavorText = "From the echoes of a violent life, a terrible foe is born."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/812d0f15-f738-4644-9d29-2589d7e66915.jpg?1783931565"
        ruling("2020-01-24", "If an Aura you control is destroyed at the same time as the creature it enchants, Hateful Eidolon's triggered ability counts that Aura to determine whether it triggers and how many cards you'll draw.")
        ruling("2020-01-24", "If Hateful Eidolon dies at the same time as one or more enchanted creatures, its last ability triggers for each of those creatures. This includes Hateful Eidolon itself if it's enchanted.")
    }
}
