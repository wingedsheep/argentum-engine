package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shivan Gorge
 * Legendary Land
 * {T}: Add {C}.
 * {2}{R}, {T}: Shivan Gorge deals 1 damage to each opponent.
 */
val ShivanGorge = card("Shivan Gorge") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n{2}{R}, {T}: Shivan Gorge deals 1 damage to each opponent."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "326"
        artist = "John Matson"
        flavorText = "Both forge and pyre."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1531eb8c-af8a-45c6-9058-3337c44e609f.jpg?1783946297"
        ruling("2004-10-04", "The damage has no color. It is not red.")
    }
}
