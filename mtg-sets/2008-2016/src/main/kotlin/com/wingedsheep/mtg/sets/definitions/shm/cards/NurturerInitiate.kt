package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Nurturer Initiate
 * {G}
 * Creature — Elf Shaman
 * 1 / 1
 *
 * Whenever a player casts a green spell, you may pay {1}. If you do, target creature gets +1/+1
 * until end of turn.
 *
 * - "A player" is *every* player, the Initiate's controller included, so this is
 *   [Triggers.anyPlayerCasts] (ANY binding) rather than a "whenever you cast" trigger.
 * - The target is chosen when the ability goes on the stack (a `targetRequirement` on the trigger),
 *   *before* the optional {1} is paid — declining the payment still consumed the target choice.
 * - "You may pay {1}. If you do, …" is the [MayPayManaEffect] gate; the pump is the gate's `then`,
 *   so nothing happens if the controller declines.
 */
val NurturerInitiate = card("Nurturer Initiate") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever a player casts a green spell, you may pay {1}. If you do, target creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Any.withColor(Color.GREEN))
        val creature = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.ModifyStats(1, 1, creature)
        )
        description = "Whenever a player casts a green spell, you may pay {1}. If you do, " +
            "target creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Jim Pavelec"
        flavorText = "The elves alone preserve the few shreds of beauty left. There is no one else who cares."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2330c78d-1655-4074-a650-27740be4cee1.jpg?1783942741"
    }
}
