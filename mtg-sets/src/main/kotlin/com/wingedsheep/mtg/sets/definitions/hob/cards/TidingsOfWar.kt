package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Tidings of War
 * {R}
 * Sorcery
 *
 * Amass Goblins 1. If this spell was cast from a graveyard, amass Goblins 3 instead.
 * Flashback {3}{R}
 */
val TidingsOfWar = card("Tidings of War") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Amass Goblins 1. If this spell was cast from a graveyard, amass Goblins 3 instead. " +
        "(To amass Goblins X, put X +1/+1 counters on an Army you control. It's also a Goblin. " +
        "If you don't control an Army, create a 0/0 black Goblin Army creature token first.)\n" +
        "Flashback {3}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = ConditionalEffect(
            condition = Conditions.WasCastFromGraveyard,
            effect = Effects.Amass(3, "Goblin"),
            elseEffect = Effects.Amass(1, "Goblin"),
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c16a0a-375e-48cb-9720-dbbc08c603ae.jpg?1785497148"
    }
}
