package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ominous Seas
 * {1}{U}
 * Enchantment
 * Whenever you draw a card, put a foreshadow counter on this enchantment.
 * Remove eight foreshadow counters from this enchantment: Create an 8/8 blue Kraken creature token.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 */
val OminousSeas = card("Ominous Seas") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you draw a card, put a foreshadow counter on this enchantment.\n" +
        "Remove eight foreshadow counters from this enchantment: Create an 8/8 blue Kraken creature token.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.FORESHADOW, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.FORESHADOW, 8)
        effect = Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Kraken"),
            imageUri = "https://cards.scryfall.io/normal/front/c/a/ca17c7b2-180a-4bd1-9ab2-152f8f656dba.jpg?1783930947",
        )
        description = "Remove eight foreshadow counters from this enchantment: Create an 8/8 blue Kraken creature token."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce8965f2-756a-4461-a643-db024a11c2de.jpg?1783931072"
    }
}
