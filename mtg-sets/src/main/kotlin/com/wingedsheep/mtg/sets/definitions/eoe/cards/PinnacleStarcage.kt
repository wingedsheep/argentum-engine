package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pinnacle Starcage
 * {1}{W}{W}
 * Artifact
 * When this artifact enters, exile all artifacts and creatures with mana value 2 or less
 * until this artifact leaves the battlefield.
 * {6}{W}{W}: Put each card exiled with this artifact into its owner's graveyard, then create
 * a 2/2 colorless Robot artifact creature token for each card put into a graveyard this way.
 * Sacrifice this artifact.
 */
val PinnacleStarcage = card("Pinnacle Starcage") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile all artifacts and creatures with mana value 2 or less until this artifact leaves the battlefield.\n" +
        "{6}{W}{W}: Put each card exiled with this artifact into its owner's graveyard, then create a 2/2 colorless Robot artifact creature token for each card put into a graveyard this way. Sacrifice this artifact."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // (artifact|creature) with mana value 2 or less. Built from an explicit
        // CardPredicate.Or plus an AND-ed ManaValueAtMost -- equivalent to
        // `GameObjectFilter.Artifact or GameObjectFilter.Creature` then `.manaValueAtMost(2)`,
        // since the `or` infix produces a single CardPredicate.Or that a trailing card
        // predicate AND-conjoins.
        effect = Effects.ExileGroupAndLink(
            GroupFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)
                        ),
                        CardPredicate.ManaValueAtMost(2)
                    )
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    activatedAbility {
        cost = Costs.Mana("{6}{W}{W}")
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromLinkedExile(),
                storeAs = "exiled"
            ),
            MoveCollectionEffect(
                from = "exiled",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                storeMovedAs = "moved"
            ),
            CreateTokenEffect(
                count = DynamicAmount.VariableReference("moved_count"),
                power = 2,
                toughness = 2,
                colors = emptySet(),
                creatureTypes = setOf("Robot"),
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
            ),
            Effects.SacrificeTarget(EffectTarget.Self)
        )
        description = "Put each card exiled with this artifact into its owner's graveyard, then create a 2/2 colorless Robot artifact creature token for each card put into a graveyard this way. Sacrifice this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1f40c4c-a955-4d9c-8225-251fa4159124.jpg?1752946658"
        ruling("2025-07-25", "Auras attached to an exiled permanent will be put into their owners' graveyards. Equipment attached to an exiled permanent (if they're not also being exiled) will become unattached and remain on the battlefield. Any counters on the exiled permanents will cease to exist.")
        ruling("2025-07-25", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2025-07-25", "If an artifact or creature has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-07-25", "If Pinnacle Starcage leaves the battlefield before its first ability resolves, no artifacts or creatures will be exiled.")
        ruling("2025-07-25", "Every token has mana value 0 unless it is copying something or was created with a specific mana cost.")
        ruling("2025-07-25", "In a multiplayer game, if Pinnacle Starcage's owner leaves the game, the exiled cards will return to the battlefield. Because the one-shot effect that returns the cards isn't an ability that goes on the stack, it won't cease to exist along with the leaving player's spells and abilities on the stack.")
    }
}
