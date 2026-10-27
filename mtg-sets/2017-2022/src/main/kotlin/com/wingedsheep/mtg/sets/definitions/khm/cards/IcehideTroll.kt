package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Icehide Troll
 * {2}{G}
 * Snow Creature — Troll Warrior
 * 2/3
 * {S}{S}: This creature gets +2/+0 and gains indestructible until end of turn. Tap it.
 *
 * The ability has no {T} in its cost, so it can be activated while the Troll is already tapped
 * (e.g. after it attacked) — "Tap it" then simply does nothing.
 */
val IcehideTroll = card("Icehide Troll") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Snow Creature — Troll Warrior"
    oracleText = "{S}{S}: This creature gets +2/+0 and gains indestructible until end of turn. Tap it. " +
        "(Damage and effects that say \"destroy\" don't destroy it. {S} can be paid with one mana from a snow source.)"
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{S}{S}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
        description = "{S}{S}: This creature gets +2/+0 and gains indestructible until end of turn. Tap it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Andrey Kuzinskiy"
        flavorText = "A troll's greatest weapon is its ability to emerge unexpectedly from the landscape."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f3aaed-a01f-4c6a-b9bb-cd70b0f6ceb1.jpg?1783928212"
        ruling("2021-02-05", "You can activate Icehide Troll's last ability even if it's already tapped.")
    }
}
