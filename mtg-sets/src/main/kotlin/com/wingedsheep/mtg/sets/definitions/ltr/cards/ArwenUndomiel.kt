package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Arwen Undómiel
 * {G}{U}
 * Legendary Creature — Elf Noble
 * 2/2
 *
 * Whenever you scry, put a +1/+1 counter on target creature.
 * {4}{G}{U}: Scry 2.
 */
val ArwenUndomiel = card("Arwen Undómiel") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Noble"
    power = 2
    toughness = 2
    oracleText = "Whenever you scry, put a +1/+1 counter on target creature.\n" +
        "{4}{G}{U}: Scry 2."

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{U}")
        effect = LibraryPatterns.scry(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Yongjae Choi"
        flavorText = "She was called Undómiel, for she was the Evenstar of her people."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c544e301-13b4-4dec-b65c-d54809bb7736.jpg?1695498672"
    }
}
