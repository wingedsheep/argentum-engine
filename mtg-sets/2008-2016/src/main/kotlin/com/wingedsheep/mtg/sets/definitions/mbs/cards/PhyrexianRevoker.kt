package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityScope
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Phyrexian Revoker
 * {2}
 * Artifact Creature — Phyrexian Horror
 * 2/1
 * As this creature enters, choose a nonland card name.
 * Activated abilities of sources with the chosen name can't be activated.
 *
 * Pithing Needle without the mana-ability exemption: [PreventActivatedAbilities] with
 * `abilities = ALL` stops mana abilities too (a named Llanowar Elves can't tap for mana, by hand or
 * by auto-pay), and `anyZone = true` because "sources" reaches cards outside the battlefield
 * (cycling, channel). Loyalty abilities are activated abilities as well, so a named planeswalker is
 * shut off.
 */
val PhyrexianRevoker = card("Phyrexian Revoker") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Horror"
    power = 2
    toughness = 1
    oracleText = "As this creature enters, choose a nonland card name.\n" +
        "Activated abilities of sources with the chosen name can't be activated."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            abilities = ActivatedAbilityScope.ALL,
            anyZone = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Kev Walker"
        flavorText = "Basic senses like sight and taste are reserved for those in power."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c7bec21-61b0-4e72-848b-82f38e1910e0.jpg?1783941366"
    }
}
