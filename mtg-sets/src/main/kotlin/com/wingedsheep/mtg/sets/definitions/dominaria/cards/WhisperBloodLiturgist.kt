package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Whisper, Blood Liturgist
 * {3}{B}
 * Legendary Creature — Human Cleric
 * 2/2
 * {T}, Sacrifice two creatures: Return target creature card from your graveyard to the battlefield.
 */
val WhisperBloodLiturgist = card("Whisper, Blood Liturgist") {
    manaCost = "{3}{B}"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{T}, Sacrifice two creatures: Return target creature card from your graveyard to the battlefield."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature)
        )
        val creature = target("creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Kieran Yanner"
        flavorText = "\"Look how they beg for the knife! The Demonlord has trained them well.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0647feeb-ad7a-40c7-830f-f307ba8339ad.jpg?1562730877"
    }
}
