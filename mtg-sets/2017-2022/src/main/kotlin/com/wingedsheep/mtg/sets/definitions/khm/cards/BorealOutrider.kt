package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boreal Outrider
 * {2}{G}
 * Snow Creature — Elf Warrior
 * 3/2
 * Whenever you cast a creature spell, if {S} of any of that spell's colors was spent to cast it,
 * that creature enters with an additional +1/+1 counter on it.
 *
 * - The intervening "if" is a match-time [SpellCastPredicate.PaidWithSnowManaOfItsColors]: the
 *   spell's payment never changes, so it reads the same when the trigger resolves. Snow colorless
 *   mana, or snow mana of a color the spell doesn't have, doesn't count — on any part of the cost.
 * - The counter goes on the creature spell while it is still on the stack (Runadi's shape) and
 *   travels with it as it resolves, so it enters with it even if Boreal Outrider has left by then.
 */
val BorealOutrider = card("Boreal Outrider") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Snow Creature — Elf Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever you cast a creature spell, if {S} of any of that spell's colors was spent " +
        "to cast it, that creature enters with an additional +1/+1 counter on it. " +
        "({S} is mana from a snow source.)"

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Creature,
            requires = setOf(SpellCastPredicate.PaidWithSnowManaOfItsColors)
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/455ae615-20d7-4251-828d-72a3345d06f1.jpg?1783928219"
    }
}
