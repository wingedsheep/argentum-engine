package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Zada, Hedron Grinder (BFZ #162)
 * {3}{R}
 * Legendary Creature — Goblin Ally
 * 3/3
 *
 * Whenever you cast an instant or sorcery spell that targets only Zada, copy that spell for each
 * other creature you control that the spell could target. Each copy targets a different one of
 * those creatures.
 *
 * Implementation notes:
 * - Mirrorwing Dragon's shape scoped to its controller: `Triggers.you.casts` narrowed by
 *   [SpellCastPredicate.TargetsOnlySource] (every instance of "target" on the spell must be Zada and
 *   nothing else), paid off by [Effects.CopyForEachOtherPossibleTarget] — the CR 707.10d "copy for
 *   each object it could target" effect, which fills every target slot of each copy with its own
 *   candidate and asks no player to choose.
 */
val ZadaHedronGrinder = card("Zada, Hedron Grinder") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Ally"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast an instant or sorcery spell that targets only Zada, copy that " +
        "spell for each other creature you control that the spell could target. Each copy targets a " +
        "different one of those creatures."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsOnlySource)
        )
        effect = Effects.CopyForEachOtherPossibleTarget(
            candidates = GameObjectFilter.Creature.youControl()
        )
        description = "Copy that spell for each other creature you control that the spell could " +
            "target. Each copy targets a different one of those creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Chris Rallis"
        flavorText = "\"A hedron holds magic for a thousand years—or less, if need be.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc0969f1-92a0-47c3-8eff-d106fbef2f7e.jpg?1783938190"

        ruling("2018-03-16", "Zada's ability triggers whenever you cast an instant or sorcery spell that targets only Zada and no other object or player.")
        ruling("2018-03-16", "If you cast an instant or sorcery spell that has multiple targets, but it's targeting only Zada with all of them, Zada's ability will trigger. The copies will similarly each be targeting only one of your other creatures. You can't change any of the copy's targets to other creatures.")
        ruling("2018-03-16", "Any creature you control that couldn't be targeted by the original spell (due to shroud, protection abilities, targeting restrictions, or any other reason) is just ignored by Zada's ability.")
        ruling("2018-03-16", "You control all the copies. You choose the order the copies are put onto the stack. The original spell will be on the stack beneath those copies and will resolve last.")
        ruling("2018-03-16", "The copies that the ability creates are created on the stack, so they're not cast. Abilities that trigger when a player casts a spell (like Zada's ability itself) won't trigger.")
        ruling("2018-03-16", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copies will have the same mode. You can't choose a different one.")
        ruling("2018-03-16", "If the spell that's copied has an X whose value was determined as it was cast (like Rolling Thunder does), the copies have the same value of X.")
        ruling("2018-03-16", "The controller of a copy can't choose to pay any alternative or additional costs for the copy. However, effects based on any alternative or additional costs that were paid for the original spell are copied as though those same costs were paid for the copy.")
    }
}
