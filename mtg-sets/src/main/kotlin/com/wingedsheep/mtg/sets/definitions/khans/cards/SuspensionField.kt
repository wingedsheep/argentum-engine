package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Suspension Field
 * {1}{W}
 * Enchantment
 * When this enchantment enters, you may exile target creature with toughness 3 or
 * greater until this enchantment leaves the battlefield.
 *
 * Modeled with ETB + LTB triggers using LinkedExileComponent for the link.
 */
val SuspensionField = card("Suspension Field") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may exile target creature with toughness 3 or greater until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "creature with toughness 3 or greater",
            TargetCreature(filter = TargetFilter.Creature.toughnessAtLeast(3))
        )
        effect = Effects.ExileUntilLeaves(creature)
        optional = true
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba5c9628-1801-43d9-8bb4-4cca168510b2.jpg?1562792611"
    }
}
