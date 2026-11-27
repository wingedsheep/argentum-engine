package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Primal Surge — Avacyn Restored #189
 * {8}{G}{G} · Sorcery · Mythic
 *
 * A do-while loop ([Effects.RepeatWhile], the Cultivator Colossus idiom). Each pass exiles the
 * top card of your library, then offers it — only if it's a permanent card — as an up-to-one
 * choice, so choosing nothing is the "you may" decline. The chosen card is moved onto the
 * battlefield.
 *
 * "If you do, repeat this process" reads whether the card actually *arrived*: the repeat
 * condition checks this pass's chosen card is now on the battlefield, so an Aura with nothing
 * legal to enchant (which stays in exile, CR 303.4g) ends the spell, as the 2012-05-01 ruling
 * requires — the Warp World stranded-Aura shape. A nonpermanent card, a decline, or an empty
 * library also leaves nothing on the battlefield and ends the loop. Triggers from the cards that
 * entered wait until the spell finishes resolving, as the engine always does.
 */
val PrimalSurge = card("Primal Surge") {
    manaCost = "{8}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Exile the top card of your library. If it's a permanent card, you may put it onto the " +
        "battlefield. If you do, repeat this process."

    spell {
        effect = Effects.RepeatWhile(
            body = Effects.Pipeline(
                descriptionOverride = "Exile the top card of your library. If it's a permanent card, you may " +
                    "put it onto the battlefield."
            ) {
                val top = gather(CardSource.TopOfLibrary(1))
                exile(top)
                val putting = chooseUpTo(
                    1,
                    from = filter(top, GameObjectFilter.Permanent),
                    prompt = "You may put the exiled permanent card onto the battlefield and repeat this process",
                    selectedLabel = "Put onto the battlefield",
                    // Named: the repeat condition reads it from outside the pipeline.
                    name = "primalSurgePut",
                )
                move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            },
            // "If you do, repeat this process" — only while this pass's card actually entered.
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.CollectionContainsMatch("primalSurgePut", GameObjectFilter.Any.onBattlefield())
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "David Rapoza"
        flavorText = "With Avacyn's return, the flow of life became a tidal wave."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4278f7bd-afff-4a1d-a0bb-bf9ce3ad5a2e.jpg?1783940663"
        ruling(
            "2012-05-01",
            "Repeating the process includes the instruction to repeat the process. That is, you'll keep " +
                "exiling cards and putting them onto the battlefield until you fail to put a permanent card " +
                "onto the battlefield."
        )
        ruling(
            "2012-05-01",
            "If you exile a permanent card that you can't put onto the battlefield (such as an Aura with " +
                "nothing legal to enchant) or one you don't want to put onto the battlefield, Primal Surge " +
                "finishes resolving."
        )
        ruling(
            "2012-05-01",
            "If putting any of those permanent cards onto the battlefield causes abilities to trigger, those " +
                "abilities will wait to go on the stack until Primal Surge has finished resolving. Starting " +
                "with the active player, each player puts their abilities on the stack in any order."
        )
    }
}
