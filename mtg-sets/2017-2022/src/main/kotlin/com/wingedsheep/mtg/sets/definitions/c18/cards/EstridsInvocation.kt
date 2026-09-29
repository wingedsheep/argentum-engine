package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination

val EstridsInvocation = card("Estrid's Invocation") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment"
    oracleText = "You may have this enchantment enter as a copy of an enchantment you control, except it has \"At the beginning of your upkeep, you may exile this enchantment. If you do, return it to the battlefield under its owner's control.\""

    replacementEffect(EntersAsCopy(
        copyFilter = GameObjectFilter.Enchantment.youControl(),
        exceptions = CopyExceptions(addedTriggeredAbilities = listOf(
            grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.May(Effects.Pipeline {
                    val self = gather(CardSource.Self)
                    val exiled = moveTracked(self, CardDestination.ToZone(Zone.EXILE))
                    move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
                })
            }
        ))
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04c01143-f7fc-4874-87fd-59d1432c2bbb.jpg?1783934343"
        ruling("2018-07-13", "If Estrid's Invocation doesn't copy an enchantment as it enters the battlefield, it won't have the ability to exile it at the beginning of your upkeep. You can't have it copy itself to get this ability.")
        ruling("2018-07-13", "Once Estrid's Invocation returns, it's considered a new object with no relation to the object that it was. You must choose an enchantment that's currently on the battlefield to copy (or to not copy anything). Auras that were attached to it will be put into their owners' graveyards. Any counters that were on it cease to exist.")
        ruling("2018-07-13", "Estrid's Invocation copies exactly what was printed on the original enchantment (unless that enchantment is copying something else or is a token; see below). It doesn't copy whether that enchantment is tapped or untapped, whether it has any counters on it or any Auras attached to it, or any non-copy effects that have changed its types, color, or so on.")
        ruling("2018-07-13", "If the chosen enchantment is an Aura, you choose what it enchants just before Estrid's Invocation enters the battlefield. The chosen recipient must be able to legally be enchanted by the Aura Estrid's Invocation will be. This doesn't target the player or permanent it will enchant, so an opponent's permanent with hexproof may be chosen this way.")
        ruling("2018-07-13", "If the chosen enchantment is an Aura but Estrid's Invocation won't be able to legally enchant anything, Estrid's Invocation remains in its current zone and doesn't enter the battlefield. If Estrid's Invocation is on the stack, it's put into its owner's graveyard.")
        ruling("2018-07-13", "If Estrid's Invocation somehow enters the battlefield at the same time as another enchantment, it can't become a copy of that enchantment. You may choose only an enchantment that's already on the battlefield.")
    }
}
