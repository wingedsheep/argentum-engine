package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Foul-Tongue Invocation — Dragons of Tarkir #102 (canonical printing)
 * {2}{B} · Instant
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * Target player sacrifices a creature of their choice. If you revealed a Dragon card or controlled a
 * Dragon as you cast this spell, you gain 4 life.
 *
 * The reveal is the unnamed optional additional cost (`AdditionalCostWasPaid`); "controlled a Dragon
 * as you cast this spell" is a `captureAtCast`, frozen as the spell finishes being cast. A copy wasn't
 * cast, so it keeps the bonus only through a copied reveal (per the rulings). The life gain doesn't
 * depend on the sacrifice, so it still happens when the target player controls no creatures.
 */
val FoulTongueInvocation = card("Foul-Tongue Invocation") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "Target player sacrifices a creature of their choice. If you revealed a Dragon card or controlled a " +
        "Dragon as you cast this spell, you gain 4 life."
    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
        val player = target(Targets.Player)
        effect = Effects.Sacrifice(filter = GameObjectFilter.Creature, count = 1, target = player) then Effects.If(
            condition = Conditions.Any(
                Conditions.AdditionalCostWasPaid,
                Conditions.CapturedAtCast("controlledDragon"),
            ),
            then = Effects.GainLife(4),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac6d021a-7bb8-4173-9a2f-e4a1d21fda8e.jpg?1783938597"
        ruling("2015-02-25", "If you don’t reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won’t get the bonus.")
        ruling("2015-02-25", "You may cast Foul-Tongue Invocation targeting a player who controls no creatures. If you qualify for the “Dragon bonus,” you’ll just gain 4 life.")
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the “Dragon bonus” only if a Dragon card was revealed as an additional cost. The copy wasn’t cast, so whether you controlled a Dragon won’t matter.")
        ruling("2015-02-25", "You can’t reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
    }
}
