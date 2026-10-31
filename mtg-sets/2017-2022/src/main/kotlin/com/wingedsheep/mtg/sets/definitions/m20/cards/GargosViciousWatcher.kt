package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gargos, Vicious Watcher (M20 #172)
 * {3}{G}{G}{G}  Legendary Creature — Hydra  8/7
 *
 * Vigilance
 * Hydra spells you cast cost {4} less to cast.
 * Whenever a creature you control becomes the target of a spell, Gargos fights up to one target
 * creature you don't control.
 *
 * The cost reduction is a battlefield static over Hydra spells you cast, so Gargos never reduces
 * its own cost, and it only shaves generic mana. The fight trigger is the ANY-bound, spells-only
 * `becomesTarget` over creatures you control (Venerated Rotpriest's shape), not batched: a spell
 * targeting two of your creatures triggers twice. `optional = true` is the "up to one"; if Gargos
 * has left the battlefield by resolution, [Effects.Fight] deals no damage either way.
 */
val GargosViciousWatcher = card("Gargos, Vicious Watcher") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Hydra"
    power = 8
    toughness = 7
    oracleText = "Vigilance\n" +
        "Hydra spells you cast cost {4} less to cast.\n" +
        "Whenever a creature you control becomes the target of a spell, Gargos fights up to one " +
        "target creature you don't control."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.HYDRA)),
            modification = CostModification.ReduceGeneric(4),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesTarget(spellsOnly = true)
        val foe = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "Whenever a creature you control becomes the target of a spell, Gargos fights " +
            "up to one target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e446e90-6e31-43ed-bcb1-a01422b503c0.jpg?1783932967"
        ruling(
            "2019-07-12",
            "Gargos's cost-reduction ability affects only generic mana in a Hydra's cost. For example, if " +
                "you cast a Hydra spell with mana cost {2}{G}{G}{G}, it will cost {G}{G}{G} to cast, not {G}."
        )
        ruling(
            "2019-07-12",
            "If a spell has {X} in its mana cost, you choose the value for X before applying cost " +
                "reductions. For example, if you cast a Hydra spell with mana cost {X}{G}{G} and choose for " +
                "X to be 6, you pay {2}{G}{G}. Any of that Hydra's abilities that refer to X still uses 6 as " +
                "the value for X regardless of what you actually paid."
        )
        ruling(
            "2019-07-12",
            "Gargos's controller chooses which creature (if any) Gargos will fight, not the controller of the spell."
        )
        ruling(
            "2019-07-12",
            "If a spell targets one creature you control multiple times, Gargos's ability triggers once. If " +
                "that spell targets multiple creatures you control, Gargos's ability triggers once for each of " +
                "those creatures."
        )
        ruling(
            "2019-07-12",
            "Gargos's triggered ability resolves before the spell that caused it to trigger. It resolves " +
                "even if that spell is countered."
        )
        ruling(
            "2019-07-12",
            "If the target creature is an illegal target when Gargos's last ability tries to resolve, the " +
                "ability doesn't resolve. If Gargos is no longer on the battlefield, the target creature won't " +
                "deal or be dealt damage."
        )
    }
}
