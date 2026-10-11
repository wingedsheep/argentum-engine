package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Binding the Old Gods
 * {2}{B}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target nonland permanent an opponent controls.
 * II — Search your library for a Forest card, put it onto the battlefield tapped, then shuffle.
 * III — Creatures you control gain deathtouch until end of turn.
 *
 * Chapter II finds any card with the Forest land type (basic or not, e.g. a snow-covered Forest or
 * a dual land), not just a basic Forest.
 */
val BindingTheOldGods = card("Binding the Old Gods") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target nonland permanent an opponent controls.\n" +
        "II — Search your library for a Forest card, put it onto the battlefield tapped, then shuffle.\n" +
        "III — Creatures you control gain deathtouch until end of turn."

    sagaChapter(1) {
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Destroy(permanent)
    }

    sagaChapter(2) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    sagaChapter(3) {
        effect = Patterns.Group.grantKeywordToAll(Keyword.DEATHTOUCH, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d93ee644-d7d7-48d8-a04b-fb479b74edb0.jpg?1783928200"
    }
}
