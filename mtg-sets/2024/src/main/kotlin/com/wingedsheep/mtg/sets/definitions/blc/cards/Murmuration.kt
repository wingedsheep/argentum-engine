package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Murmuration — Bloomburrow Commander #10
 * {4}{W} · Enchantment
 *
 * Birds you control get +1/+1 and have vigilance.
 * At the beginning of your end step, for each spell you've cast this turn, create a 1/2 blue Bird
 * creature token with flying named Storm Crow.
 *
 * The lord is a +1/+1 [ModifyStats] plus a vigilance [GrantKeyword] over Birds you control. The end
 * step trigger counts the controller's cast history at resolution ([DynamicAmounts.spellsCastThisTurn]),
 * and the named tokens are Birds, so they get the lord's bonus too.
 */
val Murmuration = card("Murmuration") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Birds you control get +1/+1 and have vigilance.\n" +
        "At the beginning of your end step, for each spell you've cast this turn, create a 1/2 blue " +
        "Bird creature token with flying named Storm Crow."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.BIRD).youControl())
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.BIRD).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            count = DynamicAmounts.spellsCastThisTurn(),
            power = 1,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            name = "Storm Crow",
            imageUri = "https://cards.scryfall.io/normal/front/d/4/d4498fac-e345-4cea-8d7e-7e0759600f33.jpg?1783909758"
        )
        description = "At the beginning of your end step, for each spell you've cast this turn, create a " +
            "1/2 blue Bird creature token with flying named Storm Crow."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Xabi Gaztelua"
        flavorText = "Once the crow birdfolk realized augurs were using the shapes of their murmurations " +
            "for divination, they decided to have some fun with it."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/561c9c4e-942e-40e8-b5f2-ac709a502f18.jpg?1783910736"
    }
}
