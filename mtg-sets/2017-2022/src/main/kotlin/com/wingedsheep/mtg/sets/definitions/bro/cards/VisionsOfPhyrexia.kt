package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val VisionsOfPhyrexia = card("Visions of Phyrexia") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, exile the top card of your library. You may play that card this turn.\nAt the beginning of your end step, if you didn't play a card from exile this turn, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Exile.impulse(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.Any(
            Conditions.YouPlayedLandThisTurn(fromZone = Zone.EXILE),
            Conditions.YouCastSpellsThisTurn(atLeast = 1, fromZone = Zone.EXILE)
        ))
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e922ef35-b62a-4cf8-9282-319f6de150b0.jpg?1783920057"
        ruling("2022-10-14", "You must follow all normal timing rules for a card you play using Visions of Phyrexia's first ability and, if it's a spell, you must pay its costs to cast it.")
        ruling("2022-10-14", "If you played a card from exile for any reason, the ability won't trigger, even if it wasn't the card you exiled with the first ability.")
        ruling("2022-10-14", "If the card you exiled with the first ability is an instant, you may play it during your end step after Visions of Phyrexia's last ability has already resolved and created a Powerstone token. We call this having your cake and compleating it too.")
    }
}
