package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eternal Dragon
 * {5}{W}{W}
 * Creature — Dragon Spirit
 * 5/5
 * Flying
 * {3}{W}{W}: Return Eternal Dragon from your graveyard to your hand.
 *   Activate only during your upkeep.
 * Plainscycling {2}
 */
val EternalDragon = card("Eternal Dragon") {
    manaCost = "{5}{W}{W}"
    typeLine = "Creature — Dragon Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying\n{3}{W}{W}: Return Eternal Dragon from your graveyard to your hand. Activate only during your upkeep.\nPlainscycling {2}"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}{W}{W}")
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    keywordAbility(KeywordAbility.Typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Justin Sweet"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0596928c-2b20-4dbb-aa78-3ab6c3ce0d72.jpg?1562524969"
    }
}
