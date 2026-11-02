package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Undercity Uprising
 * {2}{B}{G}
 * Sorcery
 * Creatures you control gain deathtouch until end of turn. Then target creature you control fights
 * target creature you don't control. (Each deals damage equal to its power to the other.)
 */
val UndercityUprising = card("Undercity Uprising") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Creatures you control gain deathtouch until end of turn. Then target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val yourCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        // The deathtouch grant lands first, so the fighting creature you control has deathtouch
        // when it deals its fight damage.
        effect = Patterns.Group.grantKeywordToAll(Keyword.DEATHTOUCH, Filters.Group.creaturesYouControl) then
            Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Magali Villeneuve"
        flavorText = "\"Now it's your turn to hide.\"\n—Vraska"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1e08bf-a5d6-48bb-9f7f-4711f9ec88d7.jpg?1783934117"
    }
}
