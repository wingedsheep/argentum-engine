package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.CardNameSource
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Alhammarret, High Arbiter — Magic Origins #43.
 *
 * The as-enters clause is an [EntersWithChoice] card-name choice drawing its names from the hands
 * each opponent reveals ([CardNameSource.OPPONENTS_REVEALED_HANDS]) and restricted to nonland names
 * ([CardNamePool.NONLAND]). With no nonland card revealed nothing is named, so the cast ban matches
 * nothing. The ban is the reused [PlayersCantCastSpells] scoped to each opponent, reading the name
 * through [GameObjectFilter.namedFromChosenComponent], as Gideon's Intervention does.
 */
val AlhammarretHighArbiter = card("Alhammarret, High Arbiter") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "As Alhammarret enters, each opponent reveals their hand. You choose the name of a nonland card revealed this way.\n" +
        "Your opponents can't cast spells with the chosen name (as long as this creature is on the battlefield)."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
            cardNameSource = CardNameSource.OPPONENTS_REVEALED_HANDS,
        )
    )

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.namedFromChosenComponent(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4200b101-19d3-4442-bddd-3eb77d5f99a9.jpg?1783938355"
    }
}
