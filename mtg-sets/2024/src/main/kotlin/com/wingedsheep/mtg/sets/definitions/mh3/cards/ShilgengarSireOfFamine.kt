package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shilgengar, Sire of Famine
 * {3}{B}{B}
 * Legendary Creature — Elder Demon
 * 6/6
 *
 * Flying
 * Sacrifice another creature: Create a Blood token. If you sacrificed an Angel this way, create a
 * number of Blood tokens equal to its toughness instead.
 * {W/B}{W/B}{W/B}, Sacrifice six Blood tokens: Return each creature card from your graveyard to the
 * battlefield with a finality counter on it. Those creatures are Vampires in addition to their
 * other types.
 *
 * The Angel branch reads the sacrificed creature's subtypes and toughness from the cost's
 * last-known snapshot ([Conditions.SacrificedHadSubtype], [DynamicAmounts.sacrificedToughness]).
 * The reanimation tracks exactly the cards that moved and makes each of those a Vampire
 * permanently.
 */
val ShilgengarSireOfFamine = card("Shilgengar, Sire of Famine") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Elder Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Sacrifice another creature: Create a Blood token. If you sacrificed an Angel this way, " +
        "create a number of Blood tokens equal to its toughness instead.\n" +
        "{W/B}{W/B}{W/B}, Sacrifice six Blood tokens: Return each creature card from your " +
        "graveyard to the battlefield with a finality counter on it. Those creatures are Vampires " +
        "in addition to their other types."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.If(
            condition = Conditions.SacrificedHadSubtype("Angel"),
            then = Effects.CreateBlood(DynamicAmounts.sacrificedToughness()),
            otherwise = Effects.CreateBlood(1)
        )
        description = "Sacrifice another creature: Create a Blood token. If you sacrificed an " +
            "Angel this way, create a number of Blood tokens equal to its toughness instead."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W/B}{W/B}{W/B}"),
            Costs.SacrificeMultiple(6, GameObjectFilter.Artifact.withSubtype("Blood").token())
        )
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                )
            )
            val returned = moveTracked(
                creatures,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = CounterType.FINALITY
            )
            run(
                Effects.ForEachInCollection(
                    returned,
                    Effects.AddCreatureType("Vampire", EffectTarget.IterationEntity, Duration.Permanent)
                )
            )
        }
        description = "{W/B}{W/B}{W/B}, Sacrifice six Blood tokens: Return each creature card " +
            "from your graveyard to the battlefield with a finality counter on it. Those " +
            "creatures are Vampires in addition to their other types."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e6aba35-bee5-4058-a2df-d73506d225c2.jpg?1783911275"
        ruling(
            "2024-06-07",
            "If you sacrificed an Angel to pay the cost of Shilgengar's second ability, use the " +
                "toughness of that Angel as it last existed on the battlefield to determine how " +
                "many Blood tokens to create."
        )
        ruling(
            "2024-06-07",
            "Finality counters work on any permanent, not only creatures. If a permanent with a " +
                "finality counter on it would go to a graveyard from the battlefield, exile it instead."
        )
        ruling("2025-01-24", "You can't sacrifice a Blood token to pay multiple costs.")
    }
}
