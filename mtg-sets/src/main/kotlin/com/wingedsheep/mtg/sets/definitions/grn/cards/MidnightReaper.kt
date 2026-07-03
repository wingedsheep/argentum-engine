package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Midnight Reaper
 * {2}{B}
 * Creature — Zombie Knight
 * 3/2
 *
 * Whenever a nontoken creature you control dies, this creature deals 1 damage to you and
 * you draw a card.
 *
 * The trigger uses ANY binding (not OTHER): "a nontoken creature you control" includes
 * Midnight Reaper itself, so it fires on its own death too (resolving via last-known
 * information). Not optional — the damage and draw are mandatory.
 */
val MidnightReaper = card("Midnight Reaper") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Knight"
    power = 3
    toughness = 2
    oracleText = "Whenever a nontoken creature you control dies, this creature deals 1 damage " +
        "to you and you draw a card."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().nontoken(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.DealDamage(1, EffectTarget.Controller, damageSource = EffectTarget.Self),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Sidharth Chaturvedi"
        flavorText = "No one welcomes his visit, yet all must grant him tribute."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e122fe0-51c5-404d-a7b9-3d161a426c35.jpg?1782709131"
    }
}
