package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.OptionalEffectDiscardDestination
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nephalia Academy
 * Land
 * If a spell or ability an opponent controls causes you to discard a card, you may reveal that
 * card and put it on top of your library instead of putting it anywhere else.
 * {T}: Add {C}.
 */
val NephaliaAcademy = card("Nephalia Academy") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "If a spell or ability an opponent controls causes you to discard a card, you may reveal that " +
        "card and put it on top of your library instead of putting it anywhere else.\n{T}: Add {C}."

    replacementEffect(
        OptionalEffectDiscardDestination(
            destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
            appliesTo = EventPattern.DiscardEvent(Player.You, cause = ZoneChangeCause.DiscardedByOpponentEffect),
            reveal = true,
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9243eb5b-905e-446f-9ee0-7557e3b31db3.jpg?1783937421"
        ruling("2016-07-13", "If you discard a card with madness, you'll choose whether to exile it or put it on top of your library. If you put it on top of your library, you can't cast it using madness.")
        ruling("2016-07-13", "In a Magic game, cards are discarded only from a player's hand. Effects that put cards from a player's library into that player's graveyard do not cause those cards to be discarded.")
        ruling("2016-07-13", "When you apply Nephalia Academy's effect, the card was still discarded. Abilities that trigger when a card is discarded will still trigger.")
        ruling("2016-07-13", "If a spell or ability an opponent controls causes you to make a choice and you choose to discard a card, Nephalia Academy's ability will apply.")
    }
}
