package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Aid from the Cowl
 * {3}{G}{G}
 * Enchantment
 *
 * Revolt — At the beginning of your end step, if a permanent left the battlefield under your
 * control this turn, reveal the top card of your library. If it's a permanent card, you may put it
 * onto the battlefield. Otherwise, you may put it on the bottom of your library.
 *
 * Revolt is an intervening-if on [Conditions.YouHadPermanentLeaveBattlefieldThisTurn]. The reveal
 * is Cabaretti Ascendancy's pipeline: an up-to-one permanent-filtered split for the battlefield,
 * then an up-to-one choice to bottom whatever wasn't put onto the battlefield — per the ruling, a
 * declined permanent card may also go to the bottom, and an unchosen card stays on top.
 */
val AidFromTheCowl = card("Aid from the Cowl") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Revolt — At the beginning of your end step, if a permanent left the battlefield under your " +
        "control this turn, reveal the top card of your library. If it's a permanent card, you may put it " +
        "onto the battlefield. Otherwise, you may put it on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            val (toBattlefield, notPut) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Permanent,
                prompt = "Put the permanent card onto the battlefield?",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave it"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            val toBottom = chooseUpTo(
                1,
                from = notPut,
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
        description = "Revolt — At the beginning of your end step, if a permanent left the battlefield under " +
            "your control this turn, reveal the top card of your library. If it's a permanent card, you may " +
            "put it onto the battlefield. Otherwise, you may put it on the bottom of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/565cb7fc-cf90-4020-84fe-05ec7b18709d.jpg?1783936747"
        ruling("2017-02-09", "If the revealed card is not a permanent card or if you choose not to put it onto the battlefield, you may put it on the bottom of your library. If you don't, it remains on top of your library.")
        ruling("2017-02-09", "If a permanent put onto the battlefield this way has an ability that triggers at the beginning of your end step, it won't trigger during this end step.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
