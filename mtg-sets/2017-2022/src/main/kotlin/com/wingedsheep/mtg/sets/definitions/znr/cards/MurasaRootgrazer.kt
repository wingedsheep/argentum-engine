package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Murasa Rootgrazer
 * {G}{W}
 * Creature — Beast
 * 2/3
 *
 * Vigilance
 * {T}: You may put a basic land card from your hand onto the battlefield.
 * {T}: Return target basic land you control to its owner's hand.
 *
 * The put ability is not a land play (same shape as Skyshroud Ranger, without the sorcery-speed
 * restriction), so it works even after the turn's land drop.
 */
val MurasaRootgrazer = card("Murasa Rootgrazer") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "{T}: You may put a basic land card from your hand onto the battlefield.\n" +
        "{T}: Return target basic land you control to its owner's hand."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.May(Patterns.Hand.putFromHand(filter = GameObjectFilter.BasicLand))
    }

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter(GameObjectFilter.BasicLand.youControl()))
        effect = Effects.ReturnToHand(land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Mathias Kollros"
        flavorText = "Few other creatures so closely embody Murasa's rapid cycles of growth."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e70b3b78-9bdc-449b-82a9-c2fc3dd7f120.jpg?1783929322"
        ruling(
            "2020-09-25",
            "Murasa Rootgrazer's middle ability doesn't count as playing a land. It can put a land card onto the battlefield even if you've already played your land for the turn."
        )
        ruling(
            "2020-09-25",
            "Once Murasa Rootgrazer has attacked, you can activate either of its activated abilities. Doing so doesn't remove it from combat. It remains an attacking creature and will deal and receive combat damage as normal."
        )
    }
}
