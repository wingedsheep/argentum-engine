package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Intimidation
 * {2}{B}{B}{B}
 * Enchantment
 *
 * Creatures you control have fear.
 *
 * Cover of Darkness with a controller scope instead of a chosen-type one: the printed phrase is
 * "Creatures **you control**", so the [GroupFilter] carries the `youControl()` controller
 * predicate rather than the default attached-creature scope [GrantKeyword] uses on Auras.
 *
 * Granted fear is engine-live — `BlockEvasionRules.FearRule` reads the keyword out of projected
 * state, so the restriction applies to every creature the Aura-less enchantment covers, including
 * ones that enter after it.
 */
val Intimidation = card("Intimidation") {
    manaCost = "{2}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have fear. (They can't be blocked except by artifact creatures and/or black creatures.)"

    staticAbility {
        ability = GrantKeyword(Keyword.FEAR, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Terese Nielsen"
        flavorText = "\"If they move, kill them. In fact, kill one now to make sure the other understands.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b9e1724-91cf-422e-909b-ddb69a6f9f76.jpg"
    }
}
