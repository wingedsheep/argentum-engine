package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanentOrPlayer

/**
 * Coalborn Entity
 * {4}{R}
 * Creature — Elemental
 * 4/4
 * {2}{R}: This creature deals 1 damage to target creature token, player, or planeswalker.
 *
 * The target is the "permanent or player" union with its permanent half narrowed to
 * "creature token or planeswalker" — nontoken creatures and battles are not legal targets.
 */
val CoalbornEntity = card("Coalborn Entity") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "{2}{R}: This creature deals 1 damage to target creature token, player, or planeswalker."
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val t = target(
            TargetPermanentOrPlayer(
                permanentFilter = TargetFilter(
                    GameObjectFilter.Creature.token() or GameObjectFilter.Planeswalker
                ),
                descriptionOverride = "target creature token, player, or planeswalker"
            )
        )
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Liiga Smilshkalne"
        flavorText = "The blacksmith forgot to feed the forge, so the forge fed itself."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d581e4f-77a7-4ad1-a263-882babffded8.jpg?1783919184"
    }
}
