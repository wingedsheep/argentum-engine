package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flensing Raptor
 * {2}{W}
 * Creature — Phyrexian Bird
 * 2/2
 * Flying
 * Toxic 1
 * When this creature enters, another target creature you control with toxic gets +1/+1 and gains
 * flying until end of turn.
 *
 * "With toxic" is `withKeyword(TOXIC)`, which matches any toxic N (printed or granted) off the
 * projected `TOXIC_<n>` keyword — the same check as [PorcelainZealot].
 */
val FlensingRaptor = card("Flensing Raptor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature enters, another target creature you control with toxic gets +1/+1 and " +
        "gains flying until end of turn."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC),
                excludeSelf = true
            )
        )
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.FLYING, creature)
        description = "When this creature enters, another target creature you control with toxic " +
            "gets +1/+1 and gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Abz J Harding"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/134aecf0-dc48-4fb3-8c8b-4e5272077856.jpg?1783918081"
    }
}
