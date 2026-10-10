package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Selvala, Explorer Returned
 * {1}{G}{W}
 * Legendary Creature — Elf Scout
 * 2/4
 * Parley — {T}: Each player reveals the top card of their library. For each nonland card revealed
 * this way, add {G} and you gain 1 life. Then each player draws a card. (Activate only as an
 * instant.)
 *
 * Parley is an ability word: each player reveals their top card ([CardSource.TopOfLibrary] over
 * [Player.Each], revealed), the nonland ones are counted, and that count feeds both the {G} and
 * the life gain. Since the August 2026 update to CR 605.1a (an ability that moves cards to or from
 * a library isn't a mana ability), this ability uses the stack — so it is *not* flagged as a mana
 * ability.
 */
val SelvalaExplorerReturned = card("Selvala, Explorer Returned") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Scout"
    power = 2
    toughness = 4
    oracleText = "Parley — {T}: Each player reveals the top card of their library. For each nonland " +
        "card revealed this way, add {G} and you gain 1 life. Then each player draws a card. " +
        "(Activate only as an instant.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(count = 1, player = Player.Each),
                revealed = true
            )
            val nonland = filter(revealed, GameObjectFilter.Nonland)
            run(Effects.AddMana(Color.GREEN, nonland.count))
            run(Effects.GainLife(nonland.count))
            run(Effects.ForEachPlayer(Player.Each, Effects.DrawCards(1)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Tyler Jacobson"
        flavorText = "\"The Lowlands refuse to suffer at the whims of the High City.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89d4786c-e022-4ae5-9ef3-75886db51f49.jpg?1783939370"
        ruling(
            "2026-08-05",
            "Selvala's ability is not a mana ability. Players can respond to it, and it can't be " +
                "activated in the middle of paying for a cost. A recent update to rule 605.1a has made " +
                "any abilities that move cards to or from libraries no longer mana abilities."
        )
        ruling(
            "2014-05-29",
            "Except in some very rare cases, the card each player draws will be the card revealed " +
                "from the top of their library."
        )
    }
}
