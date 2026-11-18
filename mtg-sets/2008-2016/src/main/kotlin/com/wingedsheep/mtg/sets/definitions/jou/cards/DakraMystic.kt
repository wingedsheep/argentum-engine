package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dakra Mystic
 * {U}
 * Creature — Merfolk Wizard
 * 1/1
 *
 * {U}, {T}: Each player reveals the top card of their library. You may put the revealed cards
 * into their owners' graveyards. If you don't, each player draws a card.
 *
 * Every player reveals first ([CardSource.TopOfLibrary] over [Player.Each]); then the ability's
 * controller alone makes one all-or-nothing choice. Yes puts every revealed card into its owner's
 * graveyard; no makes each player draw (the revealed card, barring replacement effects).
 */
val DakraMystic = card("Dakra Mystic") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 1
    oracleText = "{U}, {T}: Each player reveals the top card of their library. You may put the " +
        "revealed cards into their owners' graveyards. If you don't, each player draws a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(count = 1, player = Player.Each),
                revealed = true
            )
            run(
                Effects.May(
                    effect = Effects.Pipeline { toGraveyard(revealed) },
                    otherwise = Effects.ForEachPlayer(Player.Each, Effects.DrawCards(1)),
                    prompt = "Put the revealed cards into their owners' graveyards? " +
                        "If you don't, each player draws a card."
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Wesley Burt"
        flavorText = "Choices are rarely as simple as they seem."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e5f71ca-e7d9-4f9a-adf1-40353693deea.jpg?1783939449"
        ruling(
            "2014-04-26",
            "You make the decision after all the cards are revealed. If you decide not to put the " +
                "cards into their owners' graveyards, the card each player draws will be the revealed card."
        )
    }
}
