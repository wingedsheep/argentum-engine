package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Summary Dismissal — exile all other spells and counter all abilities. */
val SummaryDismissal = card("Summary Dismissal") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile all other spells and counter all abilities."

    spell {
        effect = Effects.ExileSpellsOnStack()
            .then(Effects.CounterAllStackObjects(spells = false, abilities = true))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Igor Kieryluk"
        flavorText = "\"Let's start fresh, shall we?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b75794d-3334-4b4d-9446-0a251dd3bd15.jpg?1783937493"
        ruling("2016-07-13", "Spells that can't be countered are exiled by Summary Dismissal. They won't resolve.")
        ruling("2016-07-13", "Only activated and triggered abilities on the stack are countered. Static abilities of objects remain unaffected, and activated and triggered abilities of objects may be activated or may trigger again later in the turn. Spells and abilities that have already resolved aren't affected.")
    }
}
