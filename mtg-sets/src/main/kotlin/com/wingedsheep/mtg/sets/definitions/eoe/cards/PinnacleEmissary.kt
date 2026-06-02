package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pinnacle Emissary
 * {1}{U}{R}
 * Artifact Creature — Robot, 3/3
 * Whenever you cast an artifact spell, create a 1/1 colorless Drone artifact creature token with flying
 * and "This token can block only creatures with flying."
 * Warp {U/R}
 */
val PinnacleEmissary = card("Pinnacle Emissary") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact Creature — Robot"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast an artifact spell, create a 1/1 colorless Drone artifact creature token with flying and \"This token can block only creatures with flying.\"\n" +
        "Warp {U/R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(spellFilter = GameObjectFilter.Artifact, player = Player.You),
            binding = TriggerBinding.ANY
        )
        effect = Effects.CreateDroneToken()
        description = "Whenever you cast an artifact spell, create a 1/1 colorless Drone artifact creature token with flying and \"This token can block only creatures with flying.\""
    }

    warp = "{U/R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Alejandro Pacheco"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c922347-f05f-40a4-bbee-6bc02a1e0de5.jpg?1752947469"
        ruling("2025-07-25", "Pinnacle Emissary's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack without resolving.")
    }
}
