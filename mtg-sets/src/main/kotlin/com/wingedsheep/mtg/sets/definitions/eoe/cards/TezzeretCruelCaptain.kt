package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Tezzeret, Cruel Captain
 * {3}
 * Legendary Planeswalker — Tezzeret
 * Starting Loyalty: 4
 *
 * Whenever an artifact you control enters, put a loyalty counter on Tezzeret.
 * 0: Untap target artifact or creature. If it's an artifact creature, put a +1/+1 counter on it.
 * −3: Search your library for an artifact card with mana value 1 or less, reveal it, put it into
 *     your hand, then shuffle.
 * −7: You get an emblem with "At the beginning of combat on your turn, put three +1/+1 counters
 *     on target artifact you control. If it's not a creature, it becomes a 0/0 Robot artifact
 *     creature."
 */
val TezzeretCruelCaptain = card("Tezzeret, Cruel Captain") {
    manaCost = "{3}"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 4
    oracleText = "Whenever an artifact you control enters, put a loyalty counter on Tezzeret.\n" +
        "0: Untap target artifact or creature. If it's an artifact creature, put a +1/+1 counter on it.\n" +
        "−3: Search your library for an artifact card with mana value 1 or less, reveal it, put it into your hand, then shuffle.\n" +
        "−7: You get an emblem with \"At the beginning of combat on your turn, put three +1/+1 counters on target artifact you control. If it's not a creature, it becomes a 0/0 Robot artifact creature.\""

    // Whenever an artifact you control enters, put a loyalty counter on Tezzeret.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.LOYALTY, 1, EffectTarget.Self)
    }

    // 0: Untap target artifact or creature. If it's an artifact creature, put a +1/+1 counter on it.
    loyaltyAbility(0) {
        val target = target(
            "artifact or creature",
            TargetPermanent(filter = TargetFilter.CreatureOrArtifact)
        )
        effect = Effects.Untap(target)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.ArtifactCreature),
                    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target)
                )
            )
    }

    // −3: Search your library for an artifact card with mana value 1 or less, reveal it, put it
    //     into your hand, then shuffle.
    loyaltyAbility(-3) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValueAtMost(1),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    // −7: You get an emblem with "At the beginning of combat on your turn, put three +1/+1
    //     counters on target artifact you control. If it's not a creature, it becomes a 0/0
    //     Robot artifact creature."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.BeginCombat.event,
                binding = Triggers.BeginCombat.binding,
                effect = Effects.AddCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    3,
                    EffectTarget.ContextTarget(0)
                ).then(
                    ConditionalEffect(
                        condition = Conditions.TargetMatchesFilter(GameObjectFilter.Noncreature),
                        effect = Effects.BecomeCreature(
                            target = EffectTarget.ContextTarget(0),
                            power = 0,
                            toughness = 0,
                            creatureTypes = setOf("Robot"),
                            duration = Duration.Permanent
                        )
                    )
                ),
                targetRequirement = TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Artifact.youControl())
                )
            ),
            descriptionOverride = "At the beginning of combat on your turn, put three +1/+1 " +
                "counters on target artifact you control. If it's not a creature, it becomes " +
                "a 0/0 Robot artifact creature."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02e8e540-8aa3-4e6a-9a11-c3949cab5f0f.jpg?1754473212"
        ruling("2025-07-25", "If the emblem's ability causes a Vehicle to become an artifact creature, its base power and toughness will be set to 0/0. Crewing that Vehicle will not restore its power and toughness. Similarly, if the ability causes a permanent with station to become an artifact creature, putting an amount of charge counters on it greater than or equal to the amount required for it to become an artifact creature won't overwrite the base power and toughness set by the emblem's ability.")
        ruling("2025-07-25", "If a card in your library has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-07-25", "The resulting artifact creature will be able to attack if it's been under your control continuously since the turn began. That is, it doesn't matter how long it's been a creature, just how long it's been on the battlefield.")
        ruling("2025-07-25", "The emblem's triggered ability doesn't remove any abilities, types, subtypes, or supertypes the artifact has.")
        ruling("2025-07-25", "If the emblem's ability causes a Vehicle to become an artifact creature, it doesn't count as \"crewing\" that Vehicle for any ability that would trigger due to a Vehicle becoming crewed.")
    }
}
