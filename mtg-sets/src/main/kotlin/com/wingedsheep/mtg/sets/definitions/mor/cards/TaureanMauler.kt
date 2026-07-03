package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taurean Mauler
 * {2}{R}
 * Creature — Shapeshifter
 * 2/2
 *
 * Changeling (This card is every creature type.)
 * Whenever an opponent casts a spell, you may put a +1/+1 counter on this creature.
 */
val TaureanMauler = card("Taurean Mauler") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Whenever an opponent casts a spell, you may put a +1/+1 counter on this creature."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.OpponentCastsSpell
        optional = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Dominick Domingo"
        flavorText = "The power of a waterfall. The fury of an avalanche. The intellect of a gale-force wind."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d50b5df1-b658-4df0-900e-79c44599b93e.jpg?1782716218"
    }
}
