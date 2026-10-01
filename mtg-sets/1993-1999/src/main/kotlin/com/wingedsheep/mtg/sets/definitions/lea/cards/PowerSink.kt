package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

val PowerSink = card("Power Sink") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}. If that player doesn't, they tap all lands with mana abilities they control and lose all unspent mana."

    spell {
        target(TargetFilter.SpellOnStack)
        // Rebind the player before countering removes the target spell from the stack.
        effect = Effects.ForEachPlayer(
            Player.ControllerOf("target spell"),
            Effects.MayPay(
                cost = Effects.PayDynamicMana(DynamicAmounts.xValue()),
                then = Effects.Nothing,
                otherwise = Effects.CounterSpell() then Effects.Pipeline {
                    val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land.withManaAbility()))
                    run(Effects.TapCollection(lands))
                } then Effects.LoseUnspentMana()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b342dd3-09b9-4108-bf12-a65d4cef4eb9.jpg?1783948703"
        ruling("2010-03-01", "Only lands that actually have mana abilities will get tapped. This includes basic lands and lands with mana abilities printed on them, as well as lands which have been granted a mana ability by some effect.")
        ruling("2004-10-04", "Does not increase the mana cost of the spell. It just requires a separate expenditure in order for it to succeed.")
        ruling("2004-10-04", "When this spell resolves, you either pay X mana or let your lands become tapped. The lands that become tapped are not “tapped for mana”. If you choose to pay, you may pay the X mana using whatever mana abilities you want to use.")
    }
}
