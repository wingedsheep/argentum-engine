package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Screaming Shield — Ravnica Allegiance #239
 * {1} · Artifact — Equipment
 */
val ScreamingShield = card("Screaming Shield") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+3 and has \"{2}, {T}: Target player mills three cards.\"\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(0, 3)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
                val p = target(Targets.Player)
                effect = Patterns.Library.mill(3, p)
            }
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Titus Lunter"
        flavorText = "Shhh . . ."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebb42a80-0442-4850-b0e7-41182d5633ff.jpg?1783933621"
    }
}
