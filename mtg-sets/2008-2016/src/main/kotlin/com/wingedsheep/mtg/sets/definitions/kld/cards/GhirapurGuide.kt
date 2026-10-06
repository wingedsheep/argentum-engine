package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ghirapur Guide
 * {2}{G}
 * Creature — Elf Scout
 * 3/2
 *
 * {2}{G}: Target creature you control can't be blocked by creatures with power 2 or less this turn.
 *
 * The targeted twin of Verdant Outrider: a durational [CantBeBlockedBy] granted to the chosen
 * creature. The restriction is consulted only at declare-blockers time, so activating after a
 * block doesn't undo it, and pumping a blocker after it blocked doesn't either (both rulings).
 */
val GhirapurGuide = card("Ghirapur Guide") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 3
    toughness = 2
    oracleText = "{2}{G}: Target creature you control can't be blocked by creatures with power 2 or less this turn."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantStaticAbility(
            ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)),
            target = creature,
            duration = Duration.EndOfTurn
        )
        description = "{2}{G}: Target creature you control can't be blocked by creatures with power 2 or less this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Scott Murphy"
        flavorText = "Fairgoers were delighted to find that even outside the fairgrounds, Ghirapur was a city of wonders."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92b6ad4a-3701-4ee8-8e1e-46cdab8e730f.jpg?1783937179"

        ruling(
            "2018-07-13",
            "Once a creature with power 2 or less has blocked a creature, activating Ghirapur Guide's " +
                "ability won't change or undo that block."
        )
        ruling(
            "2018-07-13",
            "Once a creature with power 3 or greater has blocked the target creature, changing the power " +
                "of the blocking creature won't change or undo that block."
        )
    }
}
