package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hellraiser Goblin — Gatecrash #95 (canonical printing)
 * {2}{R} · Creature — Goblin Berserker · 2/2
 *
 * Creatures you control have haste and attack each combat if able.
 */
val HellraiserGoblin = card("Hellraiser Goblin") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker"
    power = 2
    toughness = 2
    oracleText = "Creatures you control have haste and attack each combat if able."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    staticAbility {
        ability = MustAttack(GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Karl Kopinski"
        flavorText = "Don't let him lead the soldiers, but by all means let him lead the way."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/156941e7-9169-47aa-b04d-37ca78c54f7c.jpg?1783940124"
    }
}
