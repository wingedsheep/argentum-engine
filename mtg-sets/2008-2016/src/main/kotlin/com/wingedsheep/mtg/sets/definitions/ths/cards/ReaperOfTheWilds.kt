package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reaper of the Wilds
 * {2}{B}{G}
 * Creature — Gorgon
 * 4/5
 * Whenever another creature dies, scry 1.
 * {B}: This creature gains deathtouch until end of turn.
 * {1}{G}: This creature gains hexproof until end of turn.
 */
val ReaperOfTheWilds = card("Reaper of the Wilds") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Gorgon"
    power = 4
    toughness = 5
    oracleText = "Whenever another creature dies, scry 1.\n" +
        "{B}: This creature gains deathtouch until end of turn.\n" +
        "{1}{G}: This creature gains hexproof until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34d6c78f-b868-4171-b231-4dee45cfa6e0.jpg?1783939727"
    }
}
