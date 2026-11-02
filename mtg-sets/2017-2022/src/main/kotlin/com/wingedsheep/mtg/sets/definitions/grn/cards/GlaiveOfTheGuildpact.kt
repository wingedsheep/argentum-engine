package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Glaive of the Guildpact
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+0 for each Gate you control and has vigilance and menace.
 * Equip {3}
 *
 * The bonus is a Layer 7c dynamic bonus ([GrantDynamicStats]) recomputed at projection, as on
 * Nightmare Lash; "you control" scopes to the Equipment's controller. "Gate" is the land subtype.
 */
val GlaiveOfTheGuildpact = card("Glaive of the Guildpact") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 for each Gate you control and has vigilance and menace. " +
        "(A creature with menace can't be blocked except by two or more creatures.)\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Gate")).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e19723ad-7bd2-49ee-a57a-ece99018f4e8.jpg?1783934107"
    }
}
