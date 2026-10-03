package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ajani Fells the Godsire
 * {3}{W}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Exile target creature an opponent controls with power 3 or greater.
 * II — Create a 2/1 white Cat Warrior creature token, then put a vigilance counter on a creature
 *      you control.
 * III — Target creature you control gains double strike until end of turn.
 *
 * Chapter II's "a creature you control" is not targeted: it's chosen as the chapter resolves,
 * after the Cat Warrior exists, so the fresh token is a legal choice. That is a resolution-time
 * `selectTarget(nonTargeting = true)` sequenced after the token creation.
 */
val AjaniFellsTheGodsire = card("Ajani Fells the Godsire") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile target creature an opponent controls with power 3 or greater.\n" +
        "II — Create a 2/1 white Cat Warrior creature token, then put a vigilance counter on a creature you control.\n" +
        "III — Target creature you control gains double strike until end of turn."

    sagaChapter(1) {
        val victim = target(TargetFilter.CreatureOpponentControls.powerAtLeast(3))
        effect = Effects.Exile(victim)
    }

    sagaChapter(2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/c/e/ce5c5bcf-1fdd-4d73-a92b-223292da00ca.jpg?1783911117"
        ) then Effects.Pipeline {
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter.CreatureYouControl),
                nonTargeting = true,
                prompt = "Choose a creature you control to get a vigilance counter"
            )
            run(Effects.AddCounters(CounterType.VIGILANCE, 1, chosen.asTarget))
        }
    }

    sagaChapter(3) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d918133-d4e2-4674-a3cb-58edef1c6758.jpg?1783911304"
    }
}
