package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Courtly Provocateur
 * {2}{U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Target creature attacks this turn if able.
 * {T}: Target creature blocks this turn if able.
 *
 * Both are requirements checked as the declare attackers / declare blockers step begins; the
 * creature's controller chooses what it attacks or which attacker it blocks.
 */
val CourtlyProvocateur = card("Courtly Provocateur") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature attacks this turn if able.\n" +
        "{T}: Target creature blocks this turn if able."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.MarkMustAttackThisTurn(creature)
        description = "Target creature attacks this turn if able."
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.MarkMustBlockThisTurn(creature)
        description = "Target creature blocks this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "James Ryman"
        flavorText = "\"My one regret is that I have mastered an art that leaves its audience unable to applaud.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba912207-a8bf-4ffb-9967-34029cb09f7f.jpg?1783940508"
        ruling("2012-07-01", "After the first ability resolves, the creature attacks only if it's able to do so as the declare attackers step begins. If there's a cost associated with having the creature attack, the player isn't forced to pay that cost.")
        ruling("2012-07-01", "The controller of the creature targeted by the first ability chooses which player or planeswalker that creature attacks.")
        ruling("2012-07-01", "After the second ability resolves, the creature blocks only if it's able to do so as the declare blockers step begins. If there's a cost associated with having the creature block, the player isn't forced to pay that cost.")
        ruling("2012-07-01", "The controller of the creature targeted by the second ability chooses which attacking creature that creature blocks.")
    }
}
