package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gornog, the Red Reaper — Foundations Jumpstart #49 (canonical printing)
 * {2}{R} · Legendary Creature — Minotaur Warrior · 2/3
 *
 * Haste
 * Cowards can't block Warriors.
 * Whenever one or more Warriors you control attack a player, target creature that player controls
 * becomes a Coward.
 * Attacking Warriors you control get +X/+0, where X is the number of Cowards your opponents control.
 *
 * - "Cowards can't block Warriors" is read from the attacker's side, the same shape as Boldwyr
 *   Intimidator: every Warrior (any controller) can't be blocked by Cowards.
 * - The attack trigger is `Triggers.you.attacksAPlayer(Warriors you control)`: it fires once per
 *   player attacked by at least one of your Warriors and binds that player as the triggering player,
 *   which `controlledByTriggeringPlayer()` reads for "target creature that player controls".
 * - "Becomes a Coward" overwrites the creature's other creature types with no duration (the
 *   rulings), so it is [Effects.SetCreatureSubtypes] with [Duration.Permanent].
 * - The pump is a static [GrantDynamicStats] over attacking Warriors you control, recounting the
 *   Cowards your opponents control continuously.
 */
val GornogTheRedReaper = card("Gornog, the Red Reaper") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    oracleText = "Haste\n" +
        "Cowards can't block Warriors.\n" +
        "Whenever one or more Warriors you control attack a player, target creature that player " +
        "controls becomes a Coward.\n" +
        "Attacking Warriors you control get +X/+0, where X is the number of Cowards your opponents control."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withSubtype(Subtype("Coward")),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR))
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer(
            GameObjectFilter.Creature.youControl().withSubtype(Subtype.WARRIOR)
        )
        val creature = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.SetCreatureSubtypes(setOf("Coward"), creature, Duration.Permanent)
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter(
                GameObjectFilter.Creature.attacking().youControl().withSubtype(Subtype.WARRIOR)
            ),
            powerBonus = DynamicAmounts.battlefield(
                Player.EachOpponent,
                GameObjectFilter.Any.withSubtype(Subtype("Coward"))
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Ishikawa Kenta"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c80a88ae-f2f2-426a-88f3-76b5d598c25f.jpg?1783908855"

        ruling(
            "2024-11-08",
            "Once a creature has blocked a Warrior, turning that creature into a Coward won't remove it " +
                "from combat or cause the Warrior to become unblocked."
        )
        ruling(
            "2024-11-08",
            "Gornog's third ability overwrites the target's existing creature types. Once the ability " +
                "resolves, that creature will be a Coward and won't have its previous creature types. It " +
                "retains any other types and supertypes it has as well as any subtypes that aren't creature types."
        )
        ruling(
            "2024-11-08",
            "Gornog's third ability doesn't have a duration. It will remain in effect until the game ends, " +
                "the target creature leaves the battlefield, or some subsequent effect changes the creature's " +
                "characteristics, whichever comes first."
        )
        ruling(
            "2024-11-08",
            "The bonus granted by Gornog's last ability changes as the number of Cowards your opponents control changes."
        )
    }
}
