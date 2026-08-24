package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Corpseberry Cultivator
 * {1}{B/G}{B/G}
 * Creature — Squirrel Warlock
 * 2/3
 *
 * At the beginning of combat on your turn, you may forage.
 * (Exile three cards from your graveyard or sacrifice a Food.)
 *
 * Whenever you forage, put a +1/+1 counter on this creature.
 *
 * Two printed abilities, and the second one is a real trigger on `Triggers.WheneverYouForage`
 * (CR 701.59a) rather than a counter folded into this card's own forage — so a forage from *any*
 * source grows it, which is what the card says and what a Food-heavy board actually does.
 */
val CorpseberryCultivator = card("Corpseberry Cultivator") {
    manaCost = "{1}{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Squirrel Warlock"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, you may forage. (Exile three cards from your graveyard or sacrifice a Food.)\nWhenever you forage, put a +1/+1 counter on this creature."

    // At the beginning of combat on your turn, you may forage.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = MayEffect(
            effect = Patterns.Mechanic.forage(),
            descriptionOverride = "You may forage",
            hint = "Exile three cards from your graveyard or sacrifice a Food"
        )
    }

    // Whenever you forage, put a +1/+1 counter on this creature. Fed by the ability above and by
    // every other forage on the board — a forage paid as an ability cost (Camellia, the Seedmiser)
    // or a cast-time additional cost (Feed the Cycle) counts.
    triggeredAbility {
        trigger = Triggers.WheneverYouForage
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Izzy"
        flavorText = "A rare fruit grows from the corpses of Calamity Beasts, with nectar the flavor of strength and skin as fragile as life."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c911a759-ed7b-452b-88a3-663478357610.jpg?1721427036"
    }
}
