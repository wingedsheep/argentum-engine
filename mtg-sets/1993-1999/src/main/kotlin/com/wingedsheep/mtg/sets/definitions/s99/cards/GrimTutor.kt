package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GrimTutor = card("Grim Tutor") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card, put that card into your hand, then shuffle. You lose 3 life."

    spell {
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY), search = true)
            // An unrestricted search must find a card when the library is nonempty.
            val found = chooseExactly(1, from = library, prompt = "Choose a card to put into your hand")
            toHand(found)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        } then Effects.LoseLife(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Mark Tedin"
        flavorText = "One who goes unpunished never learns.\n—Greek proverb"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff00e877-3588-4ba9-a1f2-86f726157017.jpg?1783946034"
        ruling("2020-06-23", "You don't reveal the card you search for.")
    }
}
