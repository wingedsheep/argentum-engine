package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zephyr Winder — March of the Machine #328
 * {1}{U} · Creature — Elemental · 2/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, untap up to one target creature.
 */
val ZephyrWinder = card("Zephyr Winder") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, untap up to one target creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "328"
        artist = "Jana Schirmer"
        flavorText = "It twines through the roiling cloudscape feeding on the energies of nascent storms."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14456a8e-016c-4407-8410-c490db3f5ea9.jpg?1783916904"
    }
}
