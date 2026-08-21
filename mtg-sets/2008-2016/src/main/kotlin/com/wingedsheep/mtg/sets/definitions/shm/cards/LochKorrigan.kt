package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loch Korrigan
 * {3}{B}
 * Creature — Spirit
 * 1 / 1
 *
 * {U/B}: This creature gets +1/+1 until end of turn.
 *
 * - The hybrid `{U/B}` stays in the activation cost as written; it is payable with either {U} or
 *   {B} and the parser derives that from the symbol.
 * - No target: "this creature" is the source, so the pump uses [EffectTarget.Self].
 */
val LochKorrigan = card("Loch Korrigan") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "{U/B}: This creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{U/B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Daarken"
        flavorText = "\"Don't look upon still waters without first breaking the surface. The korrigan will catch you with her gaze and drag you to your death.\"\n" +
            "—Kithkin superstition"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2964b501-5b7f-4225-9dd3-e7519bf34048.jpg?1783942753"
    }
}
