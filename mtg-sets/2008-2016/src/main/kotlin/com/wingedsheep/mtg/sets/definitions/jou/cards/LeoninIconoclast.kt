package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leonin Iconoclast — Journey into Nyx #16 (canonical printing)
 * {3}{W}
 * Creature — Cat Monk
 * 3/2
 * Heroic — Whenever you cast a spell that targets this creature, destroy target enchantment creature
 * an opponent controls.
 *
 * Heroic is the `TargetsSource` cast trigger (Elite Skirmisher); the target is Bearer of Memory's
 * "enchantment creature" filter scoped to an opponent's permanents.
 */
val LeoninIconoclast = card("Leonin Iconoclast") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Monk"
    power = 3
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, destroy target " +
        "enchantment creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(
            TargetFilter((GameObjectFilter.Creature and GameObjectFilter.Enchantment).opponentControls())
        )
        effect = Effects.Destroy(creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, destroy target " +
            "enchantment creature an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Steve Prescott"
        flavorText = "\"The stars belong in the night sky. This is our world, and we are our own masters.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dfaa12e-f1d9-4a39-804f-14cdaf382257.jpg?1783939457"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
