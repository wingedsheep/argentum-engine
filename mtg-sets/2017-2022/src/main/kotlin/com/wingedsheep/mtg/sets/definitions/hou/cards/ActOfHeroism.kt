package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Act of Heroism
 * {1}{W}
 * Instant
 * Untap target creature. It gets +2/+2 until end of turn and can block an additional creature
 * this turn.
 *
 * The extra block is a granted "this creature can block an additional creature" static
 * ([CanBlockAdditionalForCreatureGroup] scoped to the holder) lasting until end of turn. Each
 * resolution adds its own grant, so multiple copies stack as the rulings require.
 */
val ActOfHeroism = card("Act of Heroism") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Untap target creature. It gets +2/+2 until end of turn and can block an additional creature this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature) then
            Effects.ModifyStats(2, 2, creature) then
            Effects.GrantStaticAbility(
                CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source()),
                creature,
                Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Magali Villeneuve"
        flavorText = "\"On every plane, there are those who run toward danger.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bebc06f6-305d-4a5a-9be7-fbc6d8cd9c72.jpg?1783936069"
        ruling("2018-03-16", "You can cast Act of Heroism even if the target creature won't be able to block right away, perhaps because you're the attacking player.")
        ruling("2018-03-16", "Untapping an attacking creature doesn't remove it from combat.")
        ruling("2018-03-16", "Act of Heroism can target an untapped creature. It still gets +2/+2 and can block an additional creature.")
        ruling("2018-03-16", "The effects of Act of Heroism are cumulative. If multiples resolve targeting the same creature, that creature can block that many additional creatures this turn.")
    }
}
