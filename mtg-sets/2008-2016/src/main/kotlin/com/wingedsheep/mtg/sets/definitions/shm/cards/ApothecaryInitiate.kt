package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Apothecary Initiate
 * {W}
 * Creature — Kithkin Cleric
 * 1 / 1
 *
 * Whenever a player casts a white spell, you may pay {1}. If you do, you gain 1 life.
 *
 * - "A player" is *every* player, the Initiate's controller included, so this is
 *   [Triggers.anyPlayerCasts] (ANY binding) rather than a "whenever you cast" trigger.
 * - The spell filter is colour-based, not type-based: any white spell qualifies, including a white
 *   land-less permanent spell or a multicoloured spell that is partly white.
 * - "You may pay {1}. If you do, …" is the [MayPayManaEffect] gate — both the yes/no and the mana
 *   payment happen on resolution, and declining simply does nothing. The Initiate's controller is
 *   always the one who pays and gains the life, whoever cast the spell.
 */
val ApothecaryInitiate = card("Apothecary Initiate") {
    manaCost = "{W}"
    typeLine = "Creature — Kithkin Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever a player casts a white spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Any.withColor(Color.WHITE))
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.GainLife(1)
        )
        description = "Whenever a player casts a white spell, you may pay {1}. If you do, you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Kev Walker"
        flavorText = "Kithkin jealously hoard their knowledge of poultices and remedies so that no outside threat can benefit from their wisdom."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b6ae637-bdb7-4117-8539-e424159bad6f.jpg?1783942770"
    }
}
