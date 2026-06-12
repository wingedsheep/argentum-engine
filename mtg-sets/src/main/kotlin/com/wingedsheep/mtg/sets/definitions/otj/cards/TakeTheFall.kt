package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Take the Fall
 * {U}
 * Instant
 *
 * Target creature gets -1/-0 until end of turn. It gets -4/-0 until end of turn instead if
 * you control an outlaw. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)
 * Draw a card.
 *
 * The outlaw check is made at resolution (a state test, no choice/pause): if you control an
 * outlaw the target gets -4/-0 *instead* of -1/-0 — never both. The card is drawn
 * unconditionally afterward.
 */
val TakeTheFall = card("Take the Fall") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-0 until end of turn. It gets -4/-0 until end of turn instead if you control an outlaw. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)\nDraw a card."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Composite(
            listOf(
                ConditionalEffect(
                    condition = Conditions.YouControl(Filters.OutlawCreature),
                    effect = Effects.ModifyStats(-4, 0, creature),
                    elseEffect = Effects.ModifyStats(-1, 0, creature)
                ),
                DrawCardsEffect(1, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Eduardo Francisco"
        flavorText = "Dewey had been briefed on every aspect of the plan... except the bit where he was left holding the bag."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fea80c4-923c-40a1-9363-bfa4c267a024.jpg?1712355525"
    }
}
