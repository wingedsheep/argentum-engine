package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Resolute Rider
 * {W/B}{W/B}{W/B}{W/B}
 * Creature — Human Knight
 * 4/2
 *
 * {W/B}{W/B}: This creature gains lifelink until end of turn.
 * {W/B}{W/B}{W/B}: This creature gains indestructible until end of turn.
 */
val ResoluteRider = card("Resolute Rider") {
    manaCost = "{W/B}{W/B}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Knight"
    oracleText = "{W/B}{W/B}: This creature gains lifelink until end of turn.\n" +
        "{W/B}{W/B}{W/B}: This creature gains indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy it.)"
    power = 4
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{W/B}{W/B}")
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{W/B}{W/B}{W/B}")
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Carmen Sinek"
        flavorText = "\"Respect the wilds? Certainly. Fear them? No.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/853dbf04-7165-4728-b96c-c9e4ff4d3491.jpg"
    }
}
