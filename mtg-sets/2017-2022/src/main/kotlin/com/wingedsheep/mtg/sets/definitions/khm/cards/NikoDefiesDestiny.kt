package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Niko Defies Destiny (Kaldheim #226)
 * {1}{W}{U} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — You gain 2 life for each foretold card you own in exile.
 * II — Add {W}{U}. Spend this mana only to foretell cards or cast spells that have foretell.
 * III — Return target card with foretell from your graveyard to your hand.
 *
 * Chapter I counts cards still foretold in your exile (`foretold()`); a card
 * exiled face down some other way doesn't count. Chapter II's mana pays the foretell special
 * action's {2} or any spell whose card has foretell, from hand or cast foretold from exile.
 */
private val foretellSpend = ManaRestriction.AnyOf(
    listOf(ManaRestriction.ForetellOnly, ManaRestriction.SpellsWithKeywordOnly(Keyword.FORETELL))
)

val NikoDefiesDestiny = card("Niko Defies Destiny") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — You gain 2 life for each foretold card you own in exile.\n" +
        "II — Add {W}{U}. Spend this mana only to foretell cards or cast spells that have foretell.\n" +
        "III — Return target card with foretell from your graveyard to your hand."

    sagaChapter(1) {
        effect = Effects.GainLife(
            DynamicAmounts.count(Player.You, Zone.EXILE, GameObjectFilter.Any.foretold()) * 2
        )
    }

    sagaChapter(2) {
        effect = Effects.AddMana(Color.WHITE, restriction = foretellSpend) then
            Effects.AddMana(Color.BLUE, restriction = foretellSpend)
    }

    sagaChapter(3) {
        val card = target(
            TargetFilter(GameObjectFilter.Any.withKeyword(Keyword.FORETELL).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19312f53-5b9d-4e76-91e8-65f444bb68c9.jpg?1783928191"
    }
}
