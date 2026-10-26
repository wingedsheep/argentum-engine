package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Esior, Wardwing Familiar — Commander Legends #67 (canonical printing)
 * {1}{U} · Legendary Creature — Bird · 1/3
 *
 * Flying
 * Spells your opponents cast that target one or more commanders you control cost {3} more to cast.
 * Partner (You can have two commanders if both have partner.)
 *
 * The tax is Kasmina's opponents-cast-targeting shape over "a commander you control" — the
 * commander designation (CR 903.3) is read off the object, so a token copy of a commander is not
 * one. It is charged once however many of your commanders the spell targets (ruling). Partner is a
 * deck-construction ability and does nothing during play.
 */
val EsiorWardwingFamiliar = card("Esior, Wardwing Familiar") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Bird"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Spells your opponents cast that target one or more commanders you control cost {3} more to cast.\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(
                GroupFilter(GameObjectFilter.Any.youControl().commander())
            ),
            modification = CostModification.IncreaseGeneric(3),
        )
    }

    keywordAbility(KeywordAbility.partner())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86958821-76eb-43a5-974b-7c945e826a66.jpg?1783928863"
        ruling("2020-11-10", "Spells that target more than one commander you control cost only {3} more to cast.")
    }
}
