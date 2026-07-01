package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Destined Confrontation
 * {2}{W}{W}
 * Sorcery
 * Each player chooses any number of creatures they control with total power 4 or less, then
 * sacrifices all other creatures they control.
 *
 * A per-player "keep a capped subset, sacrifice the rest" (APNAP, CR 101.4). Composed inside
 * [ForEachPlayerEffect] — within the loop the iterated player is the controller, so they gather
 * and choose among *their own* creatures and `Chooser.Controller` resolves to them. The
 * "total power 4 or less" cap is the aggregate [SelectionRestriction.TotalPowerAtMost] (the power
 * analogue of `TotalManaValueAtMost`), validated server-side against each creature's projected
 * power; the unchosen remainder is sacrificed via [MoveType.Sacrifice] (CR 701.21a). Mirrors the
 * per-player pile structure of Bend or Break.
 */
val DestinedConfrontation = card("Destined Confrontation") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses any number of creatures they control with total power 4 or " +
        "less, then sacrifices all other creatures they control."

    spell {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                // 1. Gather the creatures this player controls.
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "creatures"
                ),
                // 2. This player keeps any number with total power 4 or less; the rest are the remainder.
                SelectFromCollectionEffect(
                    from = "creatures",
                    selection = SelectionMode.ChooseAnyNumber,
                    restrictions = listOf(SelectionRestriction.TotalPowerAtMost(4)),
                    storeSelected = "kept",
                    storeRemainder = "sacrificed",
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose any number of creatures you control with total power 4 or less. " +
                        "The rest are sacrificed.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                ),
                // 3. Sacrifice all the creatures this player did not keep.
                MoveCollectionEffect(
                    from = "sacrificed",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Joshua Raphael"
        flavorText = "\"Just you and me, brother. The showdown that was always meant to be.\"\n—Azula"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4605cf7-03bc-4a7f-b50a-49b83b09b54d.jpg?1764119971"
    }
}
