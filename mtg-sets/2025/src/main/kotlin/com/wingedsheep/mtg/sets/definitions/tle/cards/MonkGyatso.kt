package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Monk Gyatso
 * {3}{W}
 * Legendary Creature — Human Monk
 * 3/3
 *
 * Whenever another creature you control becomes the target of a spell or ability, you may airbend
 * that creature. (Exile it. While it's exiled, its owner may cast it for {2} rather than its mana
 * cost.)
 *
 * The trigger is the subject-first `Triggers.another(...).becomesTarget()` (any controller's spell
 * or ability). "That creature" isn't a target, so the airbend is
 * [Effects.AirbendTriggeringPermanent] — the triggering creature, airbent only if it is still on
 * the battlefield when the "may" is answered (CR 701.65a; CR 400.7 for one that left in response).
 * Exiling it makes the targeting spell or ability lose that target.
 */
val MonkGyatso = card("Monk Gyatso") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Monk"
    power = 3
    toughness = 3
    oracleText = "Whenever another creature you control becomes the target of a spell or ability, you may " +
        "airbend that creature. (Exile it. While it's exiled, its owner may cast it for {2} rather than its mana cost.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).becomesTarget()
        effect = Effects.May(Effects.AirbendTriggeringPermanent())
        description = "Whenever another creature you control becomes the target of a spell or ability, you may airbend that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Masateru Ikeda"
        flavorText = "\"My ancient cake-making technique isn't the only thing on your mind, is it, Aang?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e27ed185-eb2b-42dd-b48e-3c8d4456268b.jpg?1783904834"
        ruling("2025-10-02", "Tokens exiled this way will cease to exist and cannot be cast.")
        ruling("2025-10-02", "Lands exiled this way cannot be played from exile. (Lands that were animated by earthbend will be returned to the battlefield tapped when they're exiled.)")
    }
}
