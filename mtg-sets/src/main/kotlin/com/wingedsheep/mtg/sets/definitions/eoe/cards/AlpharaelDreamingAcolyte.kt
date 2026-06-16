package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Alpharael, Dreaming Acolyte
 * {1}{U}{B}
 * Legendary Creature — Human Cleric
 * 2/3
 *
 * When Alpharael enters, draw two cards. Then discard two cards unless you discard an artifact card.
 * During your turn, Alpharael has deathtouch.
 */
val AlpharaelDreamingAcolyte = card("Alpharael, Dreaming Acolyte") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Cleric"
    oracleText = "When Alpharael enters, draw two cards. Then discard two cards unless you discard an artifact card.\nDuring your turn, Alpharael has deathtouch."
    power = 2
    toughness = 3

    // ETB: draw two cards, then discard two cards unless you discard an artifact card
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(2)
            .then(Effects.DiscardUnlessMatching(2, GameObjectFilter.Artifact))
    }

    // Conditional deathtouch during your turn
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.IsYourTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Cristi Balanescu"
        flavorText = "\"I am worthy of the Faller's blessing. I can prove it.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/349a2211-2b23-418d-a1ef-1c72ad2e171d.jpg?1752947420"
    }
}
