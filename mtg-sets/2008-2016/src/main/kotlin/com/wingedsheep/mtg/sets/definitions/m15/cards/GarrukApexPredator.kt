package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val GARRUK_APEX_PREDATOR_EMBLEM =
    "Whenever a creature attacks you, it gets +5/+5 and gains trample until end of turn."

/**
 * Garruk, Apex Predator — Magic 2015 #210 (canonical printing; the earlier PS14 copy is a promo)
 * {5}{B}{G} · Legendary Planeswalker — Garruk · Loyalty 5
 *
 * +1: Destroy another target planeswalker.
 * +1: Create a 3/3 black Beast creature token with deathtouch.
 * −3: Destroy target creature. You gain life equal to its toughness.
 * −8: Target opponent gets an emblem with "Whenever a creature attacks you, it gets +5/+5 and
 *     gains trample until end of turn."
 *
 * - The −3 is Engulfing Slagwurm's shape: the toughness is frozen with `storeNumber` before the
 *   destroy, so the life gain reads the creature as it last existed on the battlefield (pumps and
 *   counters included). The gain isn't conditioned on the destroy — an indestructible creature
 *   still gives life — but an illegal target fizzles the whole ability (ruling).
 * - The −8 is Ob Nixilis Reignited's shape: `ForEachPlayer(Player.TargetOpponent, …)` rebinds "you"
 *   to the targeted opponent, who owns and controls the emblem (ruling). Its trigger is a
 *   per-attacker `attacks()` scoped by `attackingPlayer(Controller)` — the emblem owner themself,
 *   never a planeswalker they control (ruling).
 */
val GarrukApexPredator = card("Garruk, Apex Predator") {
    manaCost = "{5}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 5
    oracleText = "+1: Destroy another target planeswalker.\n" +
        "+1: Create a 3/3 black Beast creature token with deathtouch.\n" +
        "−3: Destroy target creature. You gain life equal to its toughness.\n" +
        "−8: Target opponent gets an emblem with \"$GARRUK_APEX_PREDATOR_EMBLEM\""

    // +1: Destroy another target planeswalker.
    loyaltyAbility(+1) {
        val planeswalker = target(TargetFilter(GameObjectFilter.Planeswalker, excludeSelf = true))
        effect = Effects.Destroy(planeswalker)
        description = "Destroy another target planeswalker."
    }

    // +1: Create a 3/3 black Beast creature token with deathtouch.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Beast"),
            keywords = setOf(Keyword.DEATHTOUCH),
            imageUri = "https://cards.scryfall.io/normal/front/f/c/fc48bf5f-5fe1-41ef-a63d-85daacd941f7.jpg?1783939142"
        )
        description = "Create a 3/3 black Beast creature token with deathtouch."
    }

    // −3: Destroy target creature. You gain life equal to its toughness.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val toughness = storeNumber(DynamicAmounts.toughnessOf(creature))
            run(Effects.Destroy(creature))
            run(Effects.GainLife(toughness.amount))
        }
        description = "Destroy target creature. You gain life equal to its toughness."
    }

    // −8: Target opponent gets an emblem with "Whenever a creature attacks you, ..."
    loyaltyAbility(-8) {
        target(Targets.Opponent)
        effect = Effects.ForEachPlayer(
            Player.TargetOpponent,
            Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.a(
                        GameObjectFilter.Creature.attackingPlayer(EffectTarget.Controller)
                    ).attacks()
                    effect = Effects.ModifyStats(5, 5, EffectTarget.TriggeringEntity) then
                        Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.TriggeringEntity)
                    description = GARRUK_APEX_PREDATOR_EMBLEM
                },
                descriptionOverride = GARRUK_APEX_PREDATOR_EMBLEM,
                emblem = true
            )
        )
        description = "Target opponent gets an emblem with \"$GARRUK_APEX_PREDATOR_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "210"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3957b43-4d7e-41f9-b7a1-6f57d6603ab8.jpg?1783939159"

        ruling("2014-07-18", "The first ability can target and destroy a Garruk planeswalker controlled by another player.")
        ruling("2014-07-18", "If you activate the third ability, and the target creature is an illegal target when the ability tries to resolve, it won't resolve and none of its effects will happen. You won't gain any life.")
        ruling("2014-07-18", "The emblem's ability won't trigger if a creature attacks a planeswalker.")
        ruling("2014-07-18", "The emblem is owned and controlled by the target of the ability that created it. In a multiplayer game, if Garruk's owner leaves the game, the emblem does not.")
    }
}
