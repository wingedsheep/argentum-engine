package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Champion's Victory
 * {U}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Return target attacking creature to its owner's hand.
 *
 * The Portal-era combat-trick timing pair: `castOnlyDuring(DECLARE_ATTACKERS)` for the step and
 * `castOnlyIf(YouWereAttackedThisStep)` for the "you've been attacked" half — two separate cast
 * restrictions, because the step alone would let the attacking player cast it too.
 */
val ChampionsVictory = card("Champion's Victory") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText =
        "Cast this spell only during the declare attackers step and only if you've been attacked this step.\n" +
        "Return target attacking creature to its owner's hand."

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.attacking()))
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Hong Yan"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/451ef657-8590-497a-9d98-a732d4de6165.jpg"
    }
}
