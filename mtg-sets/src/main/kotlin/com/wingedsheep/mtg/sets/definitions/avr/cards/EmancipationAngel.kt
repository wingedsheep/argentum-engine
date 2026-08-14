package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Emancipation Angel
 * {1}{W}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * When this creature enters, return a permanent you control to its owner's hand.
 *
 * The printed card does **not** say "target" — the permanent is chosen on resolution, which is why
 * this gathers and selects rather than binding a `TargetPermanent`. Targeting would move the choice
 * to announcement and would make your own shroud/hexproof permanents illegal picks; CR 115.10a —
 * only "target" creates a target, and a spell or ability can affect an untargetable permanent it
 * doesn't target. Same gather → select → move shape as [Ghoulraiser]'s ETB.
 */
val EmancipationAngel = card("Emancipation Angel") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nWhen this creature enters, return a permanent you control to its owner's hand."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(filter = GameObjectFilter.Any.youControl()),
                storeAs = "yourPermanents",
            ),
            SelectFromCollectionEffect(
                from = "yourPermanents",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "returned",
                prompt = "Choose a permanent you control to return to its owner's hand",
            ),
            MoveCollectionEffect(
                from = "returned",
                destination = CardDestination.ToZone(Zone.HAND),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Scott Chou"
        flavorText = "\"You have done your best. I give you leave to rest.\""
        imageUri =
            "https://cards.scryfall.io/normal/front/7/a/7a4bc00e-28ca-4152-b832-f36425d2b615.jpg?1783940736"
    }
}
