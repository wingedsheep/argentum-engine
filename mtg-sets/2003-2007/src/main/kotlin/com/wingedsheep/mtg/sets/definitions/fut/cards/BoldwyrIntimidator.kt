package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boldwyr Intimidator — Future Sight #111 (canonical printing)
 * {5}{R}{R} · Creature — Giant Warrior · 5/5
 *
 * Cowards can't block Warriors.
 * {R}: Target creature becomes a Coward until end of turn.
 * {2}{R}: Target creature becomes a Warrior until end of turn.
 *
 * "Cowards can't block Warriors" is read from the attacker's side: every Warrior (any controller)
 * can't be blocked by Cowards, the same shape as Kargan Intimidator. Both "becomes a ..." abilities
 * replace the creature's other creature types (per the 2007-05-01 ruling), so they are
 * [Effects.SetCreatureSubtypes], not an added type.
 */
val BoldwyrIntimidator = card("Boldwyr Intimidator") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Warrior"
    power = 5
    toughness = 5
    oracleText = "Cowards can't block Warriors.\n" +
        "{R}: Target creature becomes a Coward until end of turn.\n" +
        "{2}{R}: Target creature becomes a Warrior until end of turn."

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withSubtype(Subtype("Coward")),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR))
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.SetCreatureSubtypes(setOf("Coward"), creature, Duration.EndOfTurn)
        description = "{R}: Target creature becomes a Coward until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.SetCreatureSubtypes(setOf("Warrior"), creature, Duration.EndOfTurn)
        description = "{2}{R}: Target creature becomes a Warrior until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Esad Ribic"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/179bec14-2e22-43eb-8854-b70a97dd023b.jpg?1783943103"

        ruling("2007-05-01", "Each of the activated abilities replaces all creature types the affected creature may have had.")
    }
}
