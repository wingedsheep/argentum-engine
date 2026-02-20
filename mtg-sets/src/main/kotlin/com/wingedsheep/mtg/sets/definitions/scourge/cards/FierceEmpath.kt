package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Fierce Empath
 * {2}{G}
 * Creature — Elf
 * 1/1
 * When Fierce Empath enters the battlefield, you may search your library for a creature card
 * with mana value 6 or greater, reveal it, put it into your hand, then shuffle.
 */
val FierceEmpath = card("Fierce Empath") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1
    oracleText = "When Fierce Empath enters the battlefield, you may search your library for a creature card with mana value 6 or greater, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.Creature.manaValueAtLeast(6),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d237e169-f152-4ddf-a5a1-32ca46cfa16d.jpg?1562535173"
    }
}
