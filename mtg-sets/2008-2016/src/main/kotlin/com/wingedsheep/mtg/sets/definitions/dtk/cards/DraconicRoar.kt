package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Draconic Roar — Dragons of Tarkir #134 (canonical printing)
 * {1}{R} · Instant
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * Draconic Roar deals 3 damage to target creature. If you revealed a Dragon card or controlled a
 * Dragon as you cast this spell, Draconic Roar deals 3 damage to that creature's controller.
 *
 * The reveal is the unnamed optional additional cost (`AdditionalCostWasPaid`); "controlled a Dragon
 * as you cast this spell" is a `captureAtCast`, frozen as the spell finishes being cast. A copy wasn't
 * cast, so it keeps the bonus only through a copied reveal (per the rulings).
 */
val DraconicRoar = card("Draconic Roar") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "Draconic Roar deals 3 damage to target creature. If you revealed a Dragon card or controlled a " +
        "Dragon as you cast this spell, Draconic Roar deals 3 damage to that creature's controller."
    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, creature) then Effects.If(
            condition = Conditions.Any(
                Conditions.AdditionalCostWasPaid,
                Conditions.CapturedAtCast("controlledDragon"),
            ),
            then = Effects.DealDamage(3, EffectTarget.TargetController),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cf5591c-46e3-4904-8b4e-4f1f84d3118f.jpg?1783938591"
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the “Dragon bonus” only if a Dragon card was revealed as an additional cost. The copy wasn’t cast, so whether you controlled a Dragon won’t matter.")
        ruling("2015-02-25", "You can’t reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
        ruling("2015-02-25", "If you don’t reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won’t get the bonus.")
    }
}
