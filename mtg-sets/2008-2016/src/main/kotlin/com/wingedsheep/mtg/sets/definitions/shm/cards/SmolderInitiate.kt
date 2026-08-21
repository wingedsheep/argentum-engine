package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Smolder Initiate
 * {B}
 * Creature — Elemental Shaman
 * 1 / 1
 *
 * Whenever a player casts a black spell, you may pay {1}. If you do, target player loses 1 life.
 *
 * - "A player" is *every* player, the Initiate's controller included, so this is
 *   [Triggers.anyPlayerCasts] (ANY binding) rather than a "whenever you cast" trigger.
 * - "Target player" is unrestricted ([Targets.Player]) — you may point it at yourself. It is chosen
 *   when the ability goes on the stack, before the optional {1} is paid.
 * - "You may pay {1}. If you do, …" is the [MayPayManaEffect] gate; the life loss is the gate's
 *   `then`, so declining does nothing.
 */
val SmolderInitiate = card("Smolder Initiate") {
    manaCost = "{B}"
    typeLine = "Creature — Elemental Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever a player casts a black spell, you may pay {1}. If you do, target player loses 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Any.withColor(Color.BLACK))
        val player = target("target", Targets.Player)
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.LoseLife(1, player)
        )
        description = "Whenever a player casts a black spell, you may pay {1}. If you do, " +
            "target player loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Chippy"
        flavorText = "\"Life is a circle. Death is a vicious circle.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16ecbed3-3a41-4823-8cd2-498daaaa0d4f.jpg?1783942752"
    }
}
