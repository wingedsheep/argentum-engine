package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Triton Cavalry — Journey into Nyx #55 (canonical printing)
 * {3}{U}
 * Creature — Merfolk Soldier
 * 2/4
 * Heroic — Whenever you cast a spell that targets this creature, you may return target
 * enchantment to its owner's hand.
 */
val TritonCavalry = card("Triton Cavalry") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Soldier"
    power = 2
    toughness = 4
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, you may return target enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        optional = true
        val enchantment = target(TargetFilter.Enchantment)
        effect = Effects.ReturnToHand(enchantment)
        description = "Heroic — Whenever you cast a spell that targets this creature, you may return target enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Kev Walker"
        flavorText = "The tritons broke the surface, and the Nyxborn broke ranks."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b9b6c6b-35f8-4c8a-aa29-ae71d5166121.jpg?1783939442"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
