package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Greater Auramancy
 * {1}{W}
 * Enchantment
 *
 * Other enchantments you control have shroud.
 * Enchanted creatures you control have shroud.
 *
 * Two group [GrantKeyword] statics. The first excludes this enchantment itself ("other"), so a
 * second Greater Auramancy is what gives the first one shroud (ruling). "Enchanted creatures you
 * control" is A Tale for the Ages' group — a creature you control with any Aura attached.
 */
val GreaterAuramancy = card("Greater Auramancy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Other enchantments you control have shroud. (A permanent with shroud can't be the target of spells or abilities.)\n" +
        "Enchanted creatures you control have shroud."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.SHROUD,
            filter = GroupFilter(GameObjectFilter.Enchantment.youControl()).other(),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.SHROUD,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().enchanted()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Chuck Lukacs"
        flavorText = "A delicate veil of glamers can prove as sturdy as iron."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1177787f-bf1e-438b-9615-97d03b04128c.jpg?1783942768"
        ruling("2008-05-01", "If you have two on the battlefield, they’ll each grant the other one shroud.")
    }
}
