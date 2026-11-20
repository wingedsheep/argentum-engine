package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sealock Monster — Theros #62
 * {3}{U}{U} · Creature — Octopus · 5/5
 *
 * This creature can't attack unless defending player controls an Island.
 * {5}{U}{U}: Monstrosity 3.
 * When this creature becomes monstrous, target land becomes an Island in addition to its other types.
 *
 * "In addition to its other types" is additive, so [Effects.AddSubtype] (not `SetLandType`, which
 * replaces land subtypes per CR 305.7), with no duration — the land stays an Island indefinitely.
 * Making an opponent's land an Island is how the monster frees itself to attack.
 */
val SealockMonster = card("Sealock Monster") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack unless defending player controls an Island.\n" +
        "{5}{U}{U}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, target land becomes an Island in addition to its other types."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}{U}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        val land = target(TargetFilter.Land)
        effect = Effects.AddSubtype("Island", land, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f942075-b285-495c-941e-6f1da48d7948.jpg?1783939792"
        ruling("2013-09-15", "The effect making the land an Island doesn't have a duration and will last indefinitely. The land retains any land types and abilities it already had. An Island has the ability \"{T}: Add {U}.\"")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
