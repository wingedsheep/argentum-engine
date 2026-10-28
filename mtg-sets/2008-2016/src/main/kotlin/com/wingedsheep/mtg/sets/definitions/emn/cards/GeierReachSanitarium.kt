package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Geier Reach Sanitarium
 * Legendary Land
 * {T}: Add {C}.
 * {2}, {T}: Each player draws a card, then discards a card.
 *
 * Lore Broker's body: every player draws (APNAP) before anyone discards, and the discards are
 * chosen in turn order and then made simultaneously by [Patterns.Hand.eachPlayerDiscards].
 */
val GeierReachSanitarium = card("Geier Reach Sanitarium") {
    manaCost = ""
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Each player draws a card, then discards a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.DrawCards(1)) then
            Patterns.Hand.eachPlayerDiscards(1)
        description = "{2}, {T}: Each player draws a card, then discards a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Cliff Childs"
        flavorText = "All are welcome."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96093739-fedc-4d8f-a29d-0e57f571e5a9.jpg?1783937421"
        ruling(
            "2016-07-13",
            "When you activate Geier Reach Sanitarium's last ability, first each player draws a card. Then the player whose turn it is selects a card from their hand and sets it aside without revealing it; proceeding in turn order, each other player does the same. Then the cards that were set aside are discarded at once.",
        )
    }
}
