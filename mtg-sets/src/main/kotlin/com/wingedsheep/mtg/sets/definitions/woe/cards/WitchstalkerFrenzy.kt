package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Witchstalker Frenzy
 * {3}{R}
 * Instant
 *
 * This spell costs {1} less to cast for each creature that attacked this turn.
 * Witchstalker Frenzy deals 5 damage to target creature.
 *
 * The reduction is [CostReductionSource.CreaturesThatAttackedThisTurn], a turn-history count
 * rather than a battlefield scan. That distinction is the whole point of the card: it is a trick
 * you cast *after* blockers or damage, when the attackers that made it cheap may already be dead.
 * Counting `Creature.attackedThisTurn()` on the live battlefield would silently un-discount the
 * spell exactly when it matters, so the source reads the per-player attacker sets the engine
 * unions at each declare-attackers step instead.
 *
 * Both rulings fall out of the rail rather than needing card-specific handling:
 * - Cost *reduction*, not a cost change: the mana cost and mana value stay {3}{R} / 4, so a
 *   Witchstalker Frenzy exiled or copied by a "mana value 4 or less" effect still qualifies.
 * - [CostModification.ReduceGenericBy] only ever eats generic mana, so however many creatures
 *   attacked, the cost bottoms out at {R}.
 */
val WitchstalkerFrenzy = card("Witchstalker Frenzy") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each creature that attacked this turn.\n" +
        "Witchstalker Frenzy deals 5 damage to target creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CreaturesThatAttackedThisTurn()
            ),
        )
    }

    spell {
        val creature = target("target creature", TargetCreature())
        effect = Effects.DealDamage(5, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Pascal Quidault"
        flavorText = "The witchstalkers circled the armored figure, drawn to the corrupting stink " +
            "of dark magic. Kellan and Ruby fled to the sound of savage howls and snapping jaws."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/649025a7-79d1-4d7c-b1db-d46bcf5a1ae2.jpg?1783915086"

        ruling(
            "2023-09-01",
            "Witchstalker Frenzy's first ability doesn't change its mana cost or mana value, only " +
                "the total cost you pay. Specifically, the mana value of Witchstalker Frenzy is " +
                "always 4."
        )
        ruling(
            "2023-09-01",
            "Witchstalker Frenzy's first ability can't reduce its cost to less than {R}."
        )
    }
}
