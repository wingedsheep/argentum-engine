package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Undertaker
 * {1}{B}
 * Creature — Human Spellshaper
 * 1 / 1
 */
val Undertaker = card("Undertaker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{B}, {T}, Discard a card: Return target creature card from your graveyard to your hand."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.CreatureCardInYourGraveyard)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Jeff Easley"
        flavorText = "The weight of death is heavy but not immovable."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f615f531-e8af-4f7b-a4ea-fb962149093f.jpg"
    }
}
