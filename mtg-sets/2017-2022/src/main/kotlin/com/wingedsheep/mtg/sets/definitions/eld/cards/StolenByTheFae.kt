package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Stolen by the Fae
 * {X}{U}{U}
 * Sorcery
 * Return target creature with mana value X to its owner's hand. You create X 1/1 blue Faerie
 * creature tokens with flying.
 *
 * X is announced as the spell is cast; the target must have mana value exactly X
 * (`.manaValueEqualsX()`, as Repeal). If the target is illegal on resolution the whole spell
 * doesn't resolve, so no Faeries are created.
 */
val StolenByTheFae = card("Stolen by the Fae") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target creature with mana value X to its owner's hand. You create X 1/1 blue Faerie creature tokens with flying."

    spell {
        val t = target(TargetFilter.Creature.manaValueEqualsX())
        effect = Effects.ReturnToHand(t) then Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Faerie"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/b/c/bcd82cb0-ff4b-4f4d-b3d0-3ac53883b099.jpg?1783932483",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Ryan Alexander Lee"
        flavorText = "Denizens of the wilds don't always seek to harm intruders. Sometimes they simply misplace them."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98a7698-57fb-41f6-86d4-251c7d444c6a.jpg?1783932648"
        ruling("2019-10-04", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2019-10-04", "If the target creature is an illegal target by the time Stolen by the Fae tries to resolve, the spell won't resolve. You won't create any Faerie tokens.")
    }
}
