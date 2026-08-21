package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Lightning Spear — Modern Horizons 2 #134
 * {1}{R} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and has trample.
 * {2}{R}, Sacrifice this Equipment: It deals 3 damage to any target.
 * Equip {1}
 *
 * A cheap red Equipment that converts into a Lava Spike once the creature holding it is dead or
 * outclassed.
 *
 * The printed line "gets +1/+0 **and** has trample" is one sentence but two continuous
 * modifications in different layers — the power bonus in layer 7c, the keyword grant in layer 6 —
 * so it is authored as two [staticAbility] blocks rather than a fused one, matching the corpus
 * shape (see `Knife`). Both use [Filters.EquippedCreature], the attached-creature group filter, so
 * they follow the Equipment as it moves and switch off the moment it is unattached.
 *
 * The damage ability's "It" is the Equipment, and it sacrifices itself as part of the cost, so the
 * source is already in the graveyard on resolution — that is fine, damage still uses last-known
 * information for the source's characteristics.
 *
 * "Equip {1}" goes through [equipAbility], never a hand-rolled activated ability: the helper sets
 * `equipCost` *and* emits the properly flagged equip ability, which is what Forge Anew-style
 * "equip costs less"/"equip at instant speed" effects key off.
 */
val LightningSpear = card("Lightning Spear") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has trample.\n" +
        "{2}{R}, Sacrifice this Equipment: It deals 3 damage to any target.\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(+1, +0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.SacrificeSelf)
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(3, t)
        description = "{2}{R}, Sacrifice this Equipment: It deals 3 damage to any target."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Steven Belledin"
        flavorText = "When wielded, it begs to be released."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f72fcda4-feb4-46e4-87d9-88bd95474fe9.jpg?1783926842"
    }
}
