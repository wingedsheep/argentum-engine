package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Heron's Grace Champion (Eldritch Moon #185)
 * {2}{G}{W}
 * Creature — Human Knight
 * 3/3
 *
 * Flash
 * Lifelink
 * When this creature enters, other Humans you control get +1/+1 and gain lifelink until end of turn.
 *
 * One group named once, two things said about it — [Patterns.Group.pumpAndGrantToAll]. "Humans" is
 * a bare tribal noun, so it reads permanents, and "other" excludes the Champion itself (it already
 * has lifelink and isn't pumped).
 */
val HeronsGraceChampion = card("Heron's Grace Champion") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Knight"
    oracleText = "Flash\n" +
        "Lifelink\n" +
        "When this creature enters, other Humans you control get +1/+1 and gain lifelink until end of turn."
    power = 3
    toughness = 3
    keywords(Keyword.FLASH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 1,
            keyword = Keyword.LIFELINK,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Human").youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Todd Lockwood"
        flavorText = "Sigarda was the last uncorrupted archangel, but she would not have to fight alone."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6448b2e2-01b2-4693-b41e-263909dc6a95.jpg"
    }
}
