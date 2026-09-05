package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val OonasProwler = card("Oona's Prowler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie Rogue"
    power = 3
    toughness = 1
    oracleText = "Flying\nDiscard a card: This creature gets -2/-0 until end of turn. Any player may activate this ability."
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.ModifyStats(-2, 0, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
        description = "This creature gets -2/-0 until end of turn. Any player may activate this ability."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Wayne Reynolds"
        flavorText = "Deep in Glen Elendra blossoms Oona, queen of the faeries, nourished by secrets and pollinated by stolen dreams."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9675b8ea-47f9-440e-9535-de879da53f76.jpg?1783942886"
    }
}
