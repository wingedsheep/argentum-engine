package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enshrouding Mist
 * {W}
 * Instant
 * Target creature gets +1/+1 until end of turn. Prevent all damage that would be dealt to it this
 * turn. If it's renowned, untap it.
 *
 * One named target; every clause binds to it. "If it's renowned" is checked as the spell resolves
 * against the target's renowned designation (`StatePredicate.IsRenowned`).
 */
val EnshroudingMist = card("Enshrouding Mist") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 until end of turn. Prevent all damage that would be dealt to it " +
        "this turn. If it's renowned, untap it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then
            Effects.PreventDamage(target = creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.renowned(), creature),
                then = Effects.Untap(creature)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Howard Lyon"
        flavorText = "True faith is letting the blade fall without parry. True cunning is using that moment to strike a deadly blow."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b059bd3e-451c-445a-b299-544c82fc0eb9.jpg?1783938362"
    }
}
