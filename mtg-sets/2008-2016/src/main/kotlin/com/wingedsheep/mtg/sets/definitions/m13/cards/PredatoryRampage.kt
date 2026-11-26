package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Predatory Rampage
 * {3}{G}{G}
 * Sorcery
 * Creatures you control get +3/+3 until end of turn. Each creature your opponents control blocks
 * this turn if able.
 *
 * Canonical printing: Magic 2013, the card's earliest printing. The pump locks in the creatures you
 * control as it resolves; the blocking requirement is a rule-modifying [Effects.MustBlockGroup] that
 * also reaches creatures arriving under an opponent's control later in the turn (both per the rulings).
 */
val PredatoryRampage = card("Predatory Rampage") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +3/+3 until end of turn. Each creature your opponents control " +
        "blocks this turn if able."

    spell {
        effect = Patterns.Group.modifyStatsForAll(3, 3, GroupFilter.AllCreaturesYouControl) then
            Effects.MustBlockGroup(GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Wayne England"
        flavorText = "\"It is the fate of the weak to fall.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e054ea5-3657-4198-9715-6acc0e362da3.jpg?1783940471"
        ruling(
            "2023-07-28",
            "Each creature controlled by an opponent blocks only if it's able to do so as the declare blockers " +
                "step begins. If, at that time, the creature is tapped, is affected by a spell or ability that " +
                "says it can't block, or no creatures are attacking that player, a planeswalker controlled by " +
                "that player, or a battle that player protects, then it doesn't block. If there's a cost " +
                "associated with having the creature block, the player isn't forced to pay that cost, so it " +
                "doesn't block in that case either."
        )
        ruling(
            "2012-07-01",
            "Only creatures you control when Predatory Rampage resolves will get +3/+3. Creatures that enter or " +
                "that you gain control of later in the turn are unaffected."
        )
        ruling(
            "2012-07-01",
            "Each creature your opponents control blocks if able, even if that creature wasn't on the " +
                "battlefield or wasn't controlled by an opponent when Predatory Rampage resolved."
        )
        ruling("2012-07-01", "Your opponent still chooses which attacking creature each creature they control blocks.")
    }
}
