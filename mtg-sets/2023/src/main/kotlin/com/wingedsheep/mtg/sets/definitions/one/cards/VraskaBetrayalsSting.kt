package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, Betrayal's Sting — Phyrexia: All Will Be One #115
 * {4}{B}{B/P} · Legendary Planeswalker — Vraska · Starting loyalty 6
 *
 * Compleated
 * 0: You draw a card and lose 1 life. Proliferate.
 * −2: Target creature becomes a Treasure artifact with "{T}, Sacrifice this artifact: Add one
 *     mana of any color" and loses all other card types and abilities.
 * −9: If target player has fewer than nine poison counters, they get a number of poison
 *     counters equal to the difference.
 *
 * The −2 keeps the creature's colors and supertypes (only card types, subtypes and abilities are
 * replaced), so `colors = null`. The −9 adds `9 − current` poison counters; a non-positive
 * difference adds nothing, which is the "fewer than nine" gate.
 */
val VraskaBetrayalsSting = card("Vraska, Betrayal's Sting") {
    manaCost = "{4}{B}{B/P}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 6
    oracleText = "Compleated ({B/P} can be paid with {B} or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "0: You draw a card and lose 1 life. Proliferate.\n" +
        "−2: Target creature becomes a Treasure artifact with \"{T}, Sacrifice this artifact: Add one mana of any color\" and loses all other card types and abilities.\n" +
        "−9: If target player has fewer than nine poison counters, they get a number of poison counters equal to the difference."

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(0) {
        effect = Effects.DrawCards(1) then
            Effects.LoseLife(1, EffectTarget.Controller) then
            Effects.Proliferate()
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.BecomeArtifact(
            target = creature,
            cardTypes = setOf("ARTIFACT"),
            subtypes = setOf("Treasure"),
            colors = null,
            loseAllAbilities = true,
            grantedAbility = ActivatedAbility(
                cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                descriptionOverride = "{T}, Sacrifice this artifact: Add one mana of any color."
            ),
            duration = Duration.Permanent
        )
    }

    loyaltyAbility(-9) {
        val player = target(Targets.Player)
        effect = Effects.AddDynamicCounters(
            CounterType.POISON,
            9 - DynamicAmounts.playerCounterCount(CounterType.POISON, player.asPlayer),
            player
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f59f2b07-47ad-4efd-ae8c-1c04b9265024.jpg?1783918037"
        ruling("2023-02-04", "In most cases, the target of Vraska's last loyalty ability will end up with nine poison counters. However, once you calculate what the difference is, replacement effects can change how many poison counters the player actually gets. For example, if the player controls Melira, the Living Cure, they will end up getting one poison counter if they had eight or fewer to begin with.")
        ruling("2023-02-04", "The target of Vraska's second loyalty ability will lose any other subtypes and card types it previously had and will be only a Treasure artifact. It will retain any supertypes it had.")
        ruling("2023-02-04", "A Phyrexian mana symbol contributes 1 toward the mana value of a card, even if life is paid for it. Specifically, Vraska's mana value is always 6.")
        ruling("2023-02-04", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
    }
}
