package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merseine
 * {2}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * This Aura enters with three net counters on it.
 * Enchanted creature doesn't untap during its controller's untap step if this Aura has a net
 * counter on it.
 * Pay enchanted creature's mana cost: Remove a net counter from this Aura. Only the controller of
 * the enchanted creature may activate this ability.
 *
 * The escape is bought three times over, at the trapped creature's own price — which is why the
 * cost has to read off the *enchanted permanent* rather than the Aura
 * ([AbilityCost.AttachedPermanentManaCost]).
 *
 * "Only the controller of the enchanted creature may activate" is two restrictions working
 * together: `AnyPlayerMay` opens the ability to players other than the Aura's controller, and the
 * condition then narrows it back to exactly the one who controls the trapped creature — which is
 * the Aura's controller when they enchanted their own creature, and the victim when they didn't.
 */
val Merseine = card("Merseine") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "This Aura enters with three net counters on it.\n" +
        "Enchanted creature doesn't untap during its controller's untap step if this Aura has a net counter on it.\n" +
        "Pay enchanted creature's mana cost: Remove a net counter from this Aura. Only the " +
        "controller of the enchanted creature may activate this ability."
    auraTarget = Targets.Creature

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.NET),
            count = 3,
            selfOnly = true
        )
    )

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.NET, 1)
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Any.attachedToBySource())
        )
    }

    activatedAbility {
        cost = AbilityCost.AttachedPermanentManaCost
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.AnyPlayerMay,
                ActivationRestriction.OnlyIfCondition(
                    Conditions.EnchantedPermanentMatches(GameObjectFilter.Any.youControl())
                )
            )
        )
        effect = Effects.RemoveCounters(Counters.NET, 1, EffectTarget.Self)
        description = "Pay enchanted creature's mana cost: Remove a net counter from this Aura. Only the controller of the enchanted creature may activate this ability."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23a"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1e96895-ef1d-44fa-b263-bce833fc3109.jpg?1783947910"
    }
}
