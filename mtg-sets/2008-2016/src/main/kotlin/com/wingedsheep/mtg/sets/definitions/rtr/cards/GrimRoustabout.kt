package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Roustabout — Return to Ravnica #68 (canonical printing)
 * {1}{B} · Creature — Skeleton Warrior · 1/1
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 * {1}{B}: Regenerate this creature.
 */
val GrimRoustabout = card("Grim Roustabout") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    power = 1
    toughness = 1
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)\n" +
        "{1}{B}: Regenerate this creature."

    unleash()

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Steven Belledin"
        flavorText = "He'll point you to your death row seats."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a5ae3f5-5466-4058-a2cd-1a036cb38a8e.jpg?1783940362"
    }
}
