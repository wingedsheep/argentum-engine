package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Hunter's Blowgun (LCI #255) — {1} Artifact — Equipment (common)
 *
 * Equipped creature gets +1/+1.
 * Equipped creature has deathtouch during your turn. Otherwise, it has reach.
 * Equip {2}
 *
 * Implementation notes:
 * - The +1/+1 buff is a plain [ModifyStats] static scoped to [Filters.EquippedCreature] and
 *   is always active while the equipment is attached.
 * - Deathtouch is a [GrantKeyword] wrapped in a [ConditionalStaticAbility] gated by
 *   [Conditions.IsYourTurn] (via the `condition` field of the `staticAbility { }` block,
 *   which auto-wraps in [ConditionalStaticAbility]). Active only during the equipment
 *   controller's own turns.
 * - Reach uses the same pattern but gated by [Conditions.IsNotYourTurn], i.e. it applies
 *   on all opponents' turns ("Otherwise, it has reach").
 * - [equipAbility] wires up the Equip {2} activated ability (sorcery-speed attach).
 */
val HuntersBlowgun = card("Hunter's Blowgun") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Equipped creature has deathtouch during your turn. Otherwise, it has reach.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature gets +1/+1 at all times.
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // Equipped creature has deathtouch during the controller's turn.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.DEATHTOUCH, Filters.EquippedCreature)
    }

    // Equipped creature has reach during opponents' turns.
    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Hector Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3348abe7-6aa3-47f7-8203-a15f75007e33.jpg?1782694408"
    }
}
