package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Abzan Devotee
 * {1}{B}
 * Creature — Dog Cleric
 * 2/1
 *
 * {1}: Add {W}, {B}, or {G}. Activate only once each turn.
 * {2}{B}: Return this card from your graveyard to your hand.
 */
val AbzanDevotee = card("Abzan Devotee") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dog Cleric"
    power = 2
    toughness = 1
    oracleText = "{1}: Add {W}, {B}, or {G}. Activate only once each turn.\n" +
        "{2}{B}: Return this card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.WHITE, Color.BLACK, Color.GREEN))
        )
        description = "{1}: Add {W}, {B}, or {G}. Activate only once each turn."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{B}: Return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Forrest Imel"
        flavorText = "The Kin-Trees rediscovered after Dromoka's fall are tended by carefully chosen wardens."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66555946-e747-46fa-b1ac-b103a8edcd93.jpg?1743204231"
    }
}
