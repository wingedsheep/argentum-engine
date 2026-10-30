package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Labyrinth of Skophos
 * Land
 * {T}: Add {C}.
 * {4}, {T}: Remove target attacking or blocking creature from combat.
 *
 * Modern removal from combat: [Effects.RemoveFromCombat] keeps its default
 * `unblockSoleBlockedAttackers = false`, so an attacker whose blocker is removed stays blocked
 * (the second ruling), and the removed creature's tapped status is untouched (the first ruling).
 */
val LabyrinthOfSkophos = card("Labyrinth of Skophos") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}, {T}: Remove target attacking or blocking creature from combat."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val creature = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.RemoveFromCombat(creature)
        description = "Remove target attacking or blocking creature from combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Jonas De Ro"
        flavorText = "For centuries, the minotaur polis of Skophos was unknown to humans. It remains unexplored."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff0dd221-2660-43f6-8e28-8000e3fe72a4.jpg?1783931509"
        ruling("2020-01-24", "A creature removed from combat by Labyrinth of Skophos remains tapped if it was tapped.")
        ruling(
            "2020-01-24",
            "If a blocking creature is removed from combat, the creature it blocked remains blocked. Unless that " +
                "creature has trample, it won't deal combat damage."
        )
        ruling(
            "2020-01-24",
            "Once creatures attack or block, they remain attacking or blocking creatures through the end of combat " +
                "step unless removed from combat (by Labyrinth of Skophos or a similar effect)."
        )
    }
}
