package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
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
 * Note: The "whenever you forage" trigger is merged into the combat trigger
 * for simplicity. The +1/+1 counter is applied as part of the forage action.
 * This means foraging from other sources won't trigger the counter — a future
 * ForageEvent/trigger system would be needed for full support.
 */
val CorpseberryCultivator = card("Corpseberry Cultivator") {
    manaCost = "{1}{B/G}{B/G}"
    typeLine = "Creature — Squirrel Warlock"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, you may forage. (Exile three cards from your graveyard or sacrifice a Food.)\nWhenever you forage, put a +1/+1 counter on this creature."

    // At the beginning of combat on your turn, you may forage.
    // If you do, put a +1/+1 counter on this creature.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = MayEffect(
            effect = EffectPatterns.forage(
                afterEffect = Effects.AddCounters("PLUS_ONE_PLUS_ONE", 1, EffectTarget.Self)
            ),
            descriptionOverride = "You may forage",
            hint = "Exile three cards from your graveyard or sacrifice a Food"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Izzy"
        flavorText = "A rare fruit grows from the corpses of Calamity Beasts, with nectar the flavor of strength and skin as fragile as life."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c911a759-ed7b-452b-88a3-663478357610.jpg?1721427036"
    }
}
