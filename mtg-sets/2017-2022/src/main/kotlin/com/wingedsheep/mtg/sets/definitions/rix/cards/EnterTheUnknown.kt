package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enter the Unknown — Rivals of Ixalan #128 (canonical printing)
 * {G} · Sorcery
 *
 * Target creature you control explores. (Reveal the top card of your library. Put that card into
 * your hand if it's a land. Otherwise, put a +1/+1 counter on the creature, then put the card back
 * or put it into your graveyard.)
 * You may play an additional land this turn.
 *
 * [Effects.Explore] on the targeted creature, then [Effects.PlayAdditionalLands] for the extra land
 * drop. Both live on one spell, so an illegal target fizzles the whole spell — matching the ruling.
 */
val EnterTheUnknown = card("Enter the Unknown") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on the creature, then put the card back or put it into your graveyard.)\n" +
        "You may play an additional land this turn."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Explore(creature) then Effects.PlayAdditionalLands(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96acf94b-75bb-4f0e-92be-978ce5920710.jpg?1783935289"
        ruling("2018-01-19", "Enter the Unknown's effect allows you to play an additional land during your main phase. Doing so follows the normal timing rules for playing lands.")
        ruling("2018-01-19", "The effects of multiples of Enter the Unknown in the same turn are cumulative. They're also cumulative with other effects that let you play additional lands, such as the one from Wayward Swordtooth.")
        ruling("2018-01-19", "If you somehow manage to cast Enter the Unknown when it's not your turn, the target creature explores when it resolves, but you won't be able to play a land that turn.")
        ruling("2018-01-19", "If the target creature is an illegal target by the time Enter the Unknown tries to resolve, the spell doesn't resolve. It won't explore, and you won't be able to play an additional land.")
    }
}
