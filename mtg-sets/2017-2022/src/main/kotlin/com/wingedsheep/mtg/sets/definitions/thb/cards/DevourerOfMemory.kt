package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devourer of Memory
 * {U}{B}
 * Creature — Nightmare
 * 2/1
 * Whenever one or more cards are put into your graveyard from your library, this creature gets
 * +1/+1 until end of turn and can't be blocked this turn.
 * {1}{U}{B}: Mill a card.
 *
 * The library-to-graveyard batching trigger (Sidisi / Hedge Shredder) over any card — one trigger
 * per event however many cards moved, per the ruling — then a +1/+1 pump and a
 * `CANT_BE_BLOCKED` grant on itself, both until end of turn.
 */
val DevourerOfMemory = card("Devourer of Memory") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Nightmare"
    power = 2
    toughness = 1
    oracleText = "Whenever one or more cards are put into your graveyard from your library, this " +
        "creature gets +1/+1 until end of turn and can't be blocked this turn.\n" +
        "{1}{U}{B}: Mill a card."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).putIntoYourGraveyard(fromLibrary = true)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}{B}")
        effect = Patterns.Library.mill(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Daarken"
        flavorText = "It slips silently through the still waters of the Labyrinth of Memories to feed."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/514a6206-c2e5-4a0f-8a20-7efd6628dd0a.jpg?1783931524"
        ruling(
            "2020-01-24",
            "Devourer of Memory's triggered ability grants it just +1/+1 each time cards are put into " +
                "your graveyard from your library, no matter how many cards are put there at once."
        )
        ruling(
            "2020-01-24",
            "Once Devourer of Memory has been blocked, resolving its triggered ability (perhaps by " +
                "activating its activated ability) won't change or undo that block."
        )
    }
}
