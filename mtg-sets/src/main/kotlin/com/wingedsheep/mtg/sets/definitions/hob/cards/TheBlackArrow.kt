package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Black Arrow — The Hobbit #171
 * {3} · Legendary Artifact — Equipment · Uncommon
 *
 * Flash
 * When The Black Arrow enters, it deals 1 damage to any target. If a Dragon is dealt damage this
 * way, destroy it.
 * Equipped creature gets +1/+1 and has reach.
 * Equip {1}
 *
 * Flash plus the enters trigger is the whole point: held up during the opponent's turn, it answers
 * an attacking Dragon for {3} regardless of the Dragon's toughness. The conditional destroy follows
 * the Sonic Shrieker convention — "is dealt damage this way" is modeled as a check on what the
 * chosen target *was*, since the engine deals the damage unconditionally to a legal target.
 * [Conditions.TargetMatchesFilter] returns false for a player target, so a player hit by the arrow
 * never trips the destroy.
 *
 * The Dragon check is `GameObjectFilter.Any.withSubtype(DRAGON)` rather than `Creature.withSubtype`
 * so a noncreature permanent that is a Dragon (a Dragon planeswalker, a Dragon Vehicle) is still
 * caught by the destroy — the card says "a Dragon", not "a Dragon creature".
 */
val TheBlackArrow = card("The Black Arrow") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Flash\n" +
        "When The Black Arrow enters, it deals 1 damage to any target. If a Dragon is dealt " +
        "damage this way, destroy it.\n" +
        "Equipped creature gets +1/+1 and has reach.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.Composite(
            Effects.DealDamage(1, anyTarget),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                    targetIndex = 0
                ),
                effect = Effects.Destroy(EffectTarget.ContextTarget(0))
            )
        )
        description = "When The Black Arrow enters, it deals 1 damage to any target. " +
            "If a Dragon is dealt damage this way, destroy it."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab181190-d53d-4972-8cd5-8e54b45f2276.jpg?1785496386"
    }
}
