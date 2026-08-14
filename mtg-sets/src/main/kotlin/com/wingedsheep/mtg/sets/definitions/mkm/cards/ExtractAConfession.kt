package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Extract a Confession {1}{B}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may collect evidence 6.
 * Each opponent sacrifices a creature of their choice. If evidence was collected, instead each
 * opponent sacrifices a creature with the greatest power among creatures they control.
 *
 * The "instead" is a resolution-time branch on the linked declaration, not two separate spells:
 * [collectEvidence] stamps `ChoiceSlot.EVIDENCE_COLLECTED` on the spell at cast time and
 * [Conditions.WasEvidenceCollected] reads it back here (CR 701.59c / CR 607 — linked abilities).
 * Both branches are the same edict shape and differ only in the filter, so the greatest-power
 * variant is [GameObjectFilter.Creature] narrowed by `hasGreatestPower()`, which scopes to
 * creatures *that permanent's controller* controls — per-opponent, exactly as the card reads.
 *
 * Each branch resolves through `Player.EachOpponent`, so the engine walks the opponents in turn
 * order and each picks their own creature before all of them are sacrificed simultaneously. A tie
 * for greatest power leaves several creatures matching the filter, and the choice among them
 * falls to that opponent — which is what the second ruling below asks for.
 */
val ExtractAConfession = card("Extract a Confession") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may collect evidence 6. (Exile " +
        "cards with total mana value 6 or greater from your graveyard.)\n" +
        "Each opponent sacrifices a creature of their choice. If evidence was collected, instead " +
        "each opponent sacrifices a creature with the greatest power among creatures they control."

    collectEvidence(6)

    spell {
        effect = ConditionalEffect(
            condition = Conditions.WasEvidenceCollected,
            effect = Effects.Sacrifice(
                GameObjectFilter.Creature.hasGreatestPower(),
                target = EffectTarget.PlayerRef(Player.EachOpponent)
            ),
            elseEffect = Effects.Sacrifice(
                GameObjectFilter.Creature,
                target = EffectTarget.PlayerRef(Player.EachOpponent)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Peter Polach"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/256c8b6e-4031-458b-8eb9-bbfe58405a0c.jpg?1783912899"
        ruling(
            "2024-02-02",
            "Starting with the next opponent in turn order (or, if you cast Extract a Confession " +
                "on an opponent's turn, starting with the opponent whose turn it is) and " +
                "proceeding in turn order, each opponent chooses a creature they control to " +
                "sacrifice. Then those creatures are sacrificed at the same time."
        )
        ruling(
            "2024-02-02",
            "If evidence was collected and an opponent has multiple creatures tied for the " +
                "greatest power, that player chooses which one to sacrifice."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
        ruling(
            "2024-02-02",
            "Once you've announced that you're casting a spell, players can't take actions until " +
                "you've finished doing so. Notably, opponents can't try to remove cards from your " +
                "graveyard to stop you from collecting evidence."
        )
    }
}
