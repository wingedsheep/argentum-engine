package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dire Wolf Prowler
 * {2}{G}
 * Creature — Wolf
 * 2/2
 * {1}{G}: This creature gets +2/+2 and gains haste until end of turn. Activate only once each turn.
 */
val DireWolfProwler = card("Dire Wolf Prowler") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "{1}{G}: This creature gets +2/+2 and gains haste until end of turn. Activate only once each turn."
    power = 2
    toughness = 2
    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self) then Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Alessandra Pisano"
        flavorText = "Large as horses, dire wolves must hunt often to satisfy their ravenous hunger. A mere human does little more than blunt their appetite."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab4769d-31ba-40d3-9375-494b93f01183.jpg?1783926465"
    }
}
