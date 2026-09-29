package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

val Copycrook = card("Copycrook") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Shapeshifter Rogue"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except it has \"Whenever this creature attacks, it connives.\" (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"

    replacementEffect(EntersAsCopy(
        exceptions = CopyExceptions(addedTriggeredAbilities = listOf(
            grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.Connive()
            }
        ))
    ))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Peter Polach"
        flavorText = "The best hiding places are other people."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52ea5e71-d0a3-4065-918b-9b2af98589ba.jpg?1783911293"
        ruling("2024-06-07", "You can choose not to copy anything. In that case, Copycrook simply enters the battlefield as a 0/0 creature and is probably put into your graveyard immediately, unless something else is increasing its toughness to keep it alive. It won't have \"Whenever this creature attacks, it connives.\"")
    }
}
