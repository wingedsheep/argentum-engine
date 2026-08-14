package com.wingedsheep.mtg.sets.definitions.ddq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pore Over the Pages
 * {3}{U}{U}
 * Sorcery
 * Draw three cards, untap up to two lands, then discard a card.
 *
 * Canonical printing is DDQ (2016-02-26), which precedes SOI (2016-04-08).
 *
 * The printed card does **not** say "target" — the lands are chosen on resolution, and they may be
 * *any* lands, not just yours. So this gathers every land on the battlefield and selects up to two
 * rather than binding a `TargetPermanent`: targeting would move the choice to announcement, make an
 * opponent's shroud/hexproof land an illegal pick, and let the spell be answered by removing a
 * "target". The three clauses are strictly ordered — draw, then untap, then discard — so the discard
 * can shed a card just drawn.
 */
val PoreOverThePages = card("Pore Over the Pages") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards, untap up to two lands, then discard a card."

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(3),
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(filter = GameObjectFilter.Land),
                storeAs = "lands",
            ),
            SelectFromCollectionEffect(
                from = "lands",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                chooser = Chooser.Controller,
                storeSelected = "toUntap",
                prompt = "Choose up to two lands to untap",
            ),
            ForEachInCollectionEffect(
                collection = "toUntap",
                effect = Effects.Untap(EffectTarget.Self),
            ),
            Patterns.Hand.discardCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Magali Villeneuve"
        flavorText =
            "\"I'm certain that the fate of Markov Manor is connected to these cryptoliths . . . " +
                "but with every page I turn, the less sure I am of the answer.\""
        imageUri =
            "https://cards.scryfall.io/normal/front/a/7/a7f9b8f0-f2b9-48ec-86c2-71d1419e396b.jpg?1783937850"
    }
}
