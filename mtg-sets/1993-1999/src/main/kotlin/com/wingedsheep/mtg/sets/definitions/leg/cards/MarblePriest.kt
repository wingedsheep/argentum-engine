package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Marble Priest — Legends #286
 * {5} · Artifact Creature — Cleric · 3/3
 *
 * All Walls able to block this creature do so.
 * Prevent all combat damage that would be dealt to this creature by Walls.
 *
 * A Lure scoped to Walls: [MustBeBlocked] with a `blockers` filter binds only the defending Walls
 * able to block it, leaving every other creature free. The prevention is the Uncle Istvan
 * [PreventDamage] replacement narrowed to combat damage from Wall sources.
 */
val MarblePriest = card("Marble Priest") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Cleric"
    power = 3
    toughness = 3
    oracleText = "All Walls able to block this creature do so.\n" +
        "Prevent all combat damage that would be dealt to this creature by Walls."

    staticAbility {
        ability = MustBeBlocked(allCreatures = true, blockers = GameObjectFilter.Creature.withSubtype("Wall"))
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.withSubtype("Wall"),
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "286"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/459b71d7-34c1-43b9-93ff-364f95aa4789.jpg?1783948026"
    }
}
