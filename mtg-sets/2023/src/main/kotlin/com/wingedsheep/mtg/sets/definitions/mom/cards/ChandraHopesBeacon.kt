package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone

/**
 * Chandra, Hope's Beacon
 * {4}{R}{R}
 * Legendary Planeswalker — Chandra
 * Whenever you cast an instant or sorcery spell, copy it. You may choose new targets for the copy.
 * This ability triggers only once each turn.
 * +2: Add two mana in any combination of colors.
 * +1: Exile the top five cards of your library. Until the end of your next turn, you may cast an
 * instant or sorcery spell from among those exiled cards.
 * −X: Chandra deals X damage to each of up to two targets.
 *
 * The +1 is a `singleUse` grant over the filtered pile: casting any one of the exiled instants or
 * sorceries spends the permission for the rest of them.
 */
val ChandraHopesBeacon = card("Chandra, Hope's Beacon") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 5
    oracleText = "Whenever you cast an instant or sorcery spell, copy it. You may choose new targets " +
        "for the copy. This ability triggers only once each turn.\n" +
        "+2: Add two mana in any combination of colors.\n" +
        "+1: Exile the top five cards of your library. Until the end of your next turn, you may cast " +
        "an instant or sorcery spell from among those exiled cards.\n" +
        "−X: Chandra deals X damage to each of up to two targets."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        oncePerTurn = true
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
    }

    loyaltyAbility(+2) {
        effect = Effects.AddManaInAnyCombination(DynamicAmounts.fixed(2))
        description = "Add two mana in any combination of colors."
    }

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(5)))
            move(exiled, CardDestination.ToZone(Zone.EXILE))
            val spells = filter(exiled, GameObjectFilter.InstantOrSorcery)
            run(Effects.GrantMayPlayFromExile(
                from = spells,
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                nonLandOnly = true,
                singleUse = true
            ))
        }
        description = "Exile the top five cards of your library. Until the end of your next turn, " +
            "you may cast an instant or sorcery spell from among those exiled cards."
    }

    loyaltyAbilityX {
        targets(AnyTarget(count = 2, minCount = 0, optional = true))
        effect = Effects.ForEachTarget(
            Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.ContextTarget(0))
        )
        description = "Chandra deals X damage to each of up to two targets."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "134"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a146ea07-ec1c-448d-b67a-dd9f9e27c2e0.jpg?1783916996"
        ruling("2023-04-14", "A copy of a spell is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2023-04-14", "If you copy a spell, you control the copy. It will resolve before the original spell does.")
        ruling("2023-04-14", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2023-04-14", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
        ruling("2023-04-14", "For the second loyalty ability, the spell you cast must be an instant or sorcery spell, although the exiled card doesn't necessarily have to be. For example, if you exile a creature card that has an Adventure, you can cast that Adventure spell.")
    }
}
