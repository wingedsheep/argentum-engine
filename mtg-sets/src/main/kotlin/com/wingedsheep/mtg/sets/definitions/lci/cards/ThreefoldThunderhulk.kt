package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Threefold Thunderhulk — The Lost Caverns of Ixalan #265
 * {7} · Artifact Creature — Gnome · Rare · 0/0
 * Artist: Xavier Ribeiro
 *
 * This creature enters with three +1/+1 counters on it.
 * Whenever this creature enters or attacks, create a number of 1/1 colorless Gnome artifact
 *   creature tokens equal to its power.
 * {2}, Sacrifice another artifact: Put a +1/+1 counter on this creature.
 *
 * Ability 1 — [EntersWithCounters] replacement effect (count = 3, selfOnly = true) applies the
 *   three +1/+1 counters as the Thunderhulk enters; base P/T is 0/0, so it is a 3/3 on entry.
 *   Default counter type is PlusOnePlusOne, so no counterType parameter is needed.
 *
 * Ability 2 — The "enters or attacks" idiom is modeled as two triggered abilities sharing the
 *   same effect (the Queen's Bay Paladin / Anim Pakal split). Each creates
 *   [DynamicAmounts.sourcePower] (= power of the source creature, evaluated at resolution) many
 *   1/1 colorless Gnome artifact creature tokens: no color set, `artifactToken = true`,
 *   `creatureTypes = setOf("Gnome")`. Reading the *source's* power at resolution means later
 *   +1/+1 counters (from ability 3, or any other pump) increase the token count on future
 *   triggers, matching the printed "equal to its power". No imageUri: the local LCI Scryfall
 *   dump has no Gnome token entry (matching Anim Pakal).
 *
 * Ability 3 — [Costs.Composite] of [Costs.Mana] "{2}" and
 *   [Costs.SacrificeAnother] over [GameObjectFilter.Artifact] (excludeSelf, so the Thunderhulk
 *   cannot sacrifice itself), with [Effects.AddCounters] putting one +1/+1 counter on the
 *   Thunderhulk ([EffectTarget.Self]).
 */
val ThreefoldThunderhulk = card("Threefold Thunderhulk") {
    manaCost = "{7}"
    typeLine = "Artifact Creature — Gnome"
    power = 0
    toughness = 0
    oracleText = "This creature enters with three +1/+1 counters on it.\n" +
        "Whenever this creature enters or attacks, create a number of 1/1 colorless Gnome " +
        "artifact creature tokens equal to its power.\n" +
        "{2}, Sacrifice another artifact: Put a +1/+1 counter on this creature."

    // This creature enters with three +1/+1 counters on it.
    replacementEffect(EntersWithCounters(count = 3, selfOnly = true))

    // Whenever this creature enters, create tokens equal to its power.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmounts.sourcePower(),
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Gnome"),
            artifactToken = true
        )
    }

    // Whenever this creature attacks, create tokens equal to its power.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateTokenEffect(
            count = DynamicAmounts.sourcePower(),
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Gnome"),
            artifactToken = true
        )
    }

    // {2}, Sacrifice another artifact: Put a +1/+1 counter on this creature.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeAnother(GameObjectFilter.Artifact)
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1917c62f-d463-43eb-87ad-89ffbc88b6fe.jpg?1782694400"
    }
}
