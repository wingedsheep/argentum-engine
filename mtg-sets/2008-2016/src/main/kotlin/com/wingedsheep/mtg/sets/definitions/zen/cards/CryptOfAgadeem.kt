package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Crypt of Agadeem
 * Land
 * This land enters tapped.
 * {T}: Add {B}.
 * {2}, {T}: Add {B} for each black creature card in your graveyard.
 */
val CryptOfAgadeem = card("Crypt of Agadeem") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "{2}, {T}: Add {B} for each black creature card in your graveyard."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddMana(
            Color.BLACK,
            DynamicAmounts.zone(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.Creature.withColor(Color.BLACK)
            ).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/672869d6-5314-422e-92f6-6a328fe0c6f6.jpg?1783942125"
    }
}
