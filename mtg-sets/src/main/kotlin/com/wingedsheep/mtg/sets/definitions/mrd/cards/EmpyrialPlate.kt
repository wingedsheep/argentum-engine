package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Empyrial Plate — Mirrodin #168
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each card in your hand.
 * Equip {2}
 *
 * Modelling notes:
 * - "your hand" is the *Equipment controller's* hand, not the equipped creature's controller's.
 *   That matters the moment the creature changes controller (Act of Treason, Domineer) while the
 *   Plate stays put. [DynamicAmounts.cardsInYourHand] resolves `Player.You` against the ability's
 *   source, which is the Plate, so the printed behaviour falls out for free.
 * - This is a Layer 7c *bonus* recomputed continuously, so the boost shrinks the instant you cast
 *   a spell in response — hence [GrantDynamicStatsEffect] rather than a snapshotted amount.
 * - The Plate itself is never in your hand while it's on the battlefield, so it never counts
 *   toward its own bonus.
 */
val EmpyrialPlate = card("Empyrial Plate") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each card in your hand.\nEquip {2}"

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.cardsInYourHand(),
            toughnessBonus = DynamicAmounts.cardsInYourHand()
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1735bbb-402e-4657-8ad0-df2c56d5ee01.jpg?1783944522"
    }
}
