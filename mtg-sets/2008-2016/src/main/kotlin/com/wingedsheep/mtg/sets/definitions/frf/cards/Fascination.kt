package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fascination {X}{U}{U}
 * Sorcery
 *
 * Choose one —
 * • Each player draws X cards.
 * • Each player mills X cards.
 *
 * X is chosen on cast and read at resolution through [DynamicAmounts.xValue] by either mode.
 * Both modes visit every player in turn order via [Effects.ForEachPlayer]; the mill mode rebinds
 * the controller to the visited player, as Folio of Fancies does.
 */
val Fascination = card("Fascination") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Each player draws X cards.\n" +
        "• Each player mills X cards."

    spell {
        modal(chooseCount = 1) {
            mode("Each player draws X cards") {
                effect = Effects.ForEachPlayer(Player.Each, Effects.DrawCards(DynamicAmounts.xValue()))
            }
            mode("Each player mills X cards") {
                effect = Effects.ForEachPlayer(Player.Each, Patterns.Library.mill(DynamicAmounts.xValue()))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37c96615-0dcb-4108-978a-b41b43e12f38.jpg?1783938707"
    }
}
