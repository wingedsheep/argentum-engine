package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goro-Goro, Disciple of Ryusei — Kamigawa: Neon Dynasty #145 (canonical printing)
 * {1}{R} · Legendary Creature — Goblin Samurai · 2/2
 *
 * {R}: Creatures you control gain haste until end of turn.
 * {3}{R}{R}: Create a 5/5 red Dragon Spirit creature token with flying. Activate only if you
 * control an attacking modified creature.
 *
 * The haste grant locks in the creatures you control as the ability resolves (Bitter Reunion's
 * shape). "Modified" is [StatePredicate.IsModified] (CR 700.9), combined with "attacking" in a
 * single filter so the same creature must be both.
 */
val GoroGoroDiscipleOfRyusei = card("Goro-Goro, Disciple of Ryusei") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Samurai"
    power = 2
    toughness = 2
    oracleText = "{R}: Creatures you control gain haste until end of turn.\n" +
        "{3}{R}{R}: Create a 5/5 red Dragon Spirit creature token with flying. Activate only if you " +
        "control an attacking modified creature. (Equipment, Auras you control, and counters are " +
        "modifications.)"

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
        description = "Creatures you control gain haste until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(
                    GameObjectFilter.Creature.attacking().withStatePredicate(StatePredicate.IsModified)
                )
            )
        )
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon", "Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c06e08-2026-471d-a6d0-bbb0f040420a.jpg?1783923715"
        )
        description = "Create a 5/5 red Dragon Spirit creature token with flying. Activate only if you " +
            "control an attacking modified creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Mike Jordana"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ca736c7-35a9-48c7-b5a9-69b2a6e33ad0.jpg?1783923865"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
