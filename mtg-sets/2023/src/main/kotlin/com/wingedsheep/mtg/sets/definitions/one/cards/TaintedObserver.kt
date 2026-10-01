package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Tainted Observer
 * {1}{G}{U}
 * Creature — Phyrexian Bird
 * 2/3
 *
 * Flying
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * Whenever another creature you control enters, you may pay {2}. If you do, proliferate.
 */
val TaintedObserver = card("Tainted Observer") {
    manaCost = "{1}{G}{U}"
    typeLine = "Creature — Phyrexian Bird"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "Whenever another creature you control enters, you may pay {2}. If you do, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.MayPay(ManaCost.parse("{2}"), Effects.Proliferate())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9cba67a-b714-49b0-9797-24aca283f162.jpg?1783917996"
    }
}
