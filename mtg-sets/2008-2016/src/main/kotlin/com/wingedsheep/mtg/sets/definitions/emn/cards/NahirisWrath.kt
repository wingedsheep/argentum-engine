package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nahiri's Wrath — Eldritch Moon #137
 * {2}{R} · Sorcery · Mythic
 *
 * As an additional cost to cast this spell, discard X cards.
 * Nahiri's Wrath deals damage equal to the total mana value of the discarded cards to each of up
 * to X target creatures and/or planeswalkers.
 *
 * "Discard X cards" announces X with the spell (CR 601.2b), before targets, so the up-to-X target
 * requirement is sized by it (`dynamicMaxCount = XValue`) and the payment discards exactly X. The
 * damage reads every discarded card's mana value wherever the discard put it
 * (`discardedAsCostTotalManaValue()`); each target takes the full amount — it isn't divided.
 */
val NahirisWrath = card("Nahiri's Wrath") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard X cards.\n" +
        "Nahiri's Wrath deals damage equal to the total mana value of the discarded cards to each of " +
        "up to X target creatures and/or planeswalkers."

    additionalCost(Costs.additional.DiscardVariableCards())

    spell {
        target = TargetObject(
            optional = true,
            filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.ForEachTarget(
            Effects.DealDamage(DynamicAmounts.discardedAsCostTotalManaValue(), EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "137"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e6cb8b0-e3a5-4a94-845d-3b286aa392ac.jpg?1783937457"
        ruling(
            "2016-07-13",
            "Nahiri's Wrath adds the mana values of the discarded cards in whatever zone they were put " +
                "into as a result of being discarded, even if they weren't put into a graveyard (due to " +
                "madness, for example) or are no longer in that zone."
        )
        ruling("2016-07-13", "The damage is not divided. Nahiri's Wrath will deal that much damage to each target.")
        ruling(
            "2016-07-13",
            "You can't target the same creature or planeswalker multiple times to deal more damage to it."
        )
        ruling("2016-07-13", "If you discard a card with {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2016-07-13",
            "If you cast Nahiri's Wrath without paying its mana cost, you'll still choose a value for X and " +
                "discard X cards."
        )
        ruling(
            "2016-07-13",
            "If you copy Nahiri's Wrath, it has the same number of targets and deals the same amount of " +
                "damage as the original spell."
        )
    }
}
