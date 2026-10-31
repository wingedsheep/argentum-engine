package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * One with the Stars
 * {3}{U}
 * Enchantment — Aura
 *
 * Enchant creature or enchantment
 * Enchanted permanent is an enchantment and loses all other card types. (It still has its
 * abilities, but it's no longer a creature.)
 *
 * A layer-4 [TransformPermanent] setting the card types to just enchantment. Subtypes are left to
 * the engine: per CR 205.1a the subtypes of the lost types (creature types, Equipment, land
 * types) go with them, while enchantment types such as Aura or Saga stay.
 */
val OneWithTheStars = card("One with the Stars") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or enchantment\nEnchanted permanent is an enchantment and loses " +
        "all other card types. (It still has its abilities, but it's no longer a creature.)"

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrEnchantment)

    staticAbility {
        ability = TransformPermanent(setCardTypes = setOf("ENCHANTMENT"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Caio Monteiro"
        flavorText = "\"Take your place among the stars.\"\n—Kruphix, god of horizons"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98b7070d-4b09-4390-aa21-1bc0aa2b629c.jpg?1783931580"
        ruling("2020-01-24", "If One with the Stars enchants an enchantment that has no other card " +
            "types, the enchanted permanent won't be affected.")
        ruling("2020-01-24", "The enchanted permanent keeps its abilities but has no power or " +
            "toughness. If one of its abilities refers to them, it uses 0.")
        ruling("2020-01-24", "The enchanted permanent loses any subtypes associated with the card " +
            "types it lost. It keeps any supertypes, such as legendary.")
        ruling("2020-01-24", "One with the Stars may enchant a permanent that is only temporarily " +
            "a creature, such as a Vehicle. If this happens, One with the Stars's effect causes the " +
            "enchanted permanent to remain an enchantment even after the temporary effect expires.")
    }
}
