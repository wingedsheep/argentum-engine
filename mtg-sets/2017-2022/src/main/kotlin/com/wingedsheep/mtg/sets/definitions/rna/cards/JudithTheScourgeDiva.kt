package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Judith, the Scourge Diva — Ravnica Allegiance #185
 * {1}{B}{R} · Legendary Creature — Human Shaman · 2 / 2
 *
 * The death trigger has no "other": Judith dying fires it too (she is a nontoken creature you
 * control), and the damage is dealt by her last-known self.
 */
val JudithTheScourgeDiva = card("Judith, the Scourge Diva") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Other creatures you control get +1/+0.\n" +
        "Whenever a nontoken creature you control dies, Judith deals 1 damage to any target."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter.OtherCreaturesYouControl,
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Wesley Burt"
        flavorText = "At the end of the show, she stands alone on a stage brilliant with blood."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a742125-730d-4082-bfd8-5feb7733def4.jpg?1783933645"
    }
}
