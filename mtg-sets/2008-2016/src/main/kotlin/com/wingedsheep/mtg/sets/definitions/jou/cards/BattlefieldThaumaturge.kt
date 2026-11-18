package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Battlefield Thaumaturge — Journey into Nyx #31 (canonical printing)
 * {1}{U}
 * Creature — Human Wizard
 * 2/1
 * Each instant and sorcery spell you cast costs {1} less to cast for each creature it targets.
 * Heroic — Whenever you cast a spell that targets this creature, this creature gains hexproof
 * until end of turn.
 *
 * The discount counts the spell's *different* targets that are creatures
 * ([CostReductionSource.ChosenTargets] with a filter) — one creature targeted twice is {1}
 * (ruling), and `onBattlefield()` keeps a creature card in a graveyard from counting.
 * Generic-only, as the reduction is.
 */
val BattlefieldThaumaturge = card("Battlefield Thaumaturge") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "Each instant and sorcery spell you cast costs {1} less to cast for each creature it targets.\n" +
        "Heroic — Whenever you cast a spell that targets this creature, this creature gains hexproof until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.ChosenTargets(filter = GameObjectFilter.Creature.onBattlefield()),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, this creature gains hexproof until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Mike Sass"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/467ffc93-b107-457c-af04-a1b682a4b886.jpg?1783939450"
        ruling("2014-04-26", "Battlefield Thaumaturge's first ability doesn't change that spell's mana cost or mana value. It changes only the amount you actually pay (the \"total cost\" of the spell).")
        ruling("2014-04-26", "To calculate the total cost of an instant or sorcery spell you're casting, start with the mana cost (or alternative cost, if applicable), add all cost increases (including additional costs such as those imposed by strive abilities), then subtract all cost reductions (such as the one created by Battlefield Thaumaturge's first ability).")
        ruling("2014-04-26", "The cost reduction created by Battlefield Thaumaturge's first ability can reduce only the generic portion of the spell's total cost. Colored mana requirements aren't affected.")
        ruling("2014-04-26", "The cost reduction is {1} per target creature, even if the spell targets a creature more than once.")
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
