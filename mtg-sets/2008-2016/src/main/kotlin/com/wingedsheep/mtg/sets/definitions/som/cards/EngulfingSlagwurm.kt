package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Engulfing Slagwurm
 * {5}{G}{G}
 * Creature — Wurm
 * 7/7
 *
 * Whenever this creature blocks or becomes blocked by a creature, destroy that creature. You gain
 * life equal to that creature's toughness.
 *
 * "By **a** creature" is the per-partner shape of `Triggers.self.blocksOrBecomesBlocked(by)`: one
 * trigger per combat partner, each bound to that creature as [EffectTarget.TriggeringEntity]
 * (Corrosive Ooze's shape), which is exactly the ruling's "each trigger will be associated with a
 * specific creature".
 *
 * The toughness is frozen with `storeNumber` *before* the destroy (Weed Strangle's pattern), so the
 * life gain uses the creature's toughness as it last existed on the battlefield rather than its
 * printed toughness from the graveyard. An indestructible or regenerated creature stays on the
 * battlefield and still gives the life, as the rulings require.
 */
val EngulfingSlagwurm = card("Engulfing Slagwurm") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 7
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, destroy that creature. " +
        "You gain life equal to that creature's toughness."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.Pipeline {
            val toughness = storeNumber(DynamicAmounts.toughnessOf(EffectTarget.TriggeringEntity))
            run(Effects.Destroy(EffectTarget.TriggeringEntity))
            run(Effects.GainLife(toughness.amount))
        }
        description = "Whenever this creature blocks or becomes blocked by a creature, destroy that creature. " +
            "You gain life equal to that creature's toughness."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Jaime Jones"
        flavorText = "Its teeth exist only for decoration."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8aeabc4a-7b4f-4e3d-bcc7-423bb703563a.jpg?1783941718"
        ruling("2011-01-01", "Engulfing Slagwurm's ability triggers and resolves during the declare blockers step. Creatures destroyed this way will not deal combat damage.")
        ruling("2011-01-01", "If your Engulfing Slagwurm blocks or becomes blocked by multiple creatures, its ability triggers that many times. Each trigger will be associated with a specific creature. You choose which order to have the abilities resolve.")
        ruling("2011-01-01", "If Engulfing Slagwurm's ability resolves and the other creature is not destroyed (perhaps because it has already left the battlefield or it regenerates), you'll still gain life equal to that creature's toughness.")
        ruling("2011-01-01", "As each ability resolves, the amount of life you gain is equal to the appropriate creature's current toughness (if it's somehow still on the battlefield), or its toughness as it last existed on the battlefield (in all other cases).")
    }
}
