package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lithobraking
 * {2}{R}
 * Instant
 * Create a Lander token. Then you may sacrifice an artifact. When you do,
 * Lithobraking deals 2 damage to each creature.
 */
val Lithobraking = card("Lithobraking") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Create a Lander token. Then you may sacrifice an artifact. When you do, " +
        "Lithobraking deals 2 damage to each creature. " +
        "(A Lander token is an artifact with \"{2}, {T}, Sacrifice this token: Search your library " +
        "for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    spell {
        effect = Effects.CreateLander().then(
            ReflexiveTriggerEffect(
                action = SacrificeEffect(GameObjectFilter.Artifact),
                optional = true,
                reflexiveEffect = Effects.ForEachInGroup(
                    GroupFilter.AllCreatures,
                    DealDamageEffect(2, EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Andrew Mar"
        flavorText = "A Kav landing\n—Sotheran expression meaning \"a bad time\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a22024c-2c0f-4487-98d1-ee89cf3dba89.jpg?1752947127"
    }
}
