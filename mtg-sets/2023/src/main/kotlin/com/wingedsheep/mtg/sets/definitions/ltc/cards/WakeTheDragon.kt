package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wake the Dragon
 * {4}{B}{R}
 * Sorcery
 *
 * Create a 6/6 black and red Dragon creature token with flying, menace, and "Whenever this token
 * deals combat damage to a player, gain control of target artifact that player controls."
 * Flashback {6}{B}{R}
 *
 * - The token's trigger targets an artifact controlled by the player the Dragon hit
 *   (`controlledByTriggeringPlayer()` — "that player", not "any opponent"), and the control change
 *   is permanent.
 */
val WakeTheDragon = card("Wake the Dragon") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Create a 6/6 black and red Dragon creature token with flying, menace, and " +
        "\"Whenever this token deals combat damage to a player, gain control of target artifact " +
        "that player controls.\"\n" +
        "Flashback {6}{B}{R} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 6,
            toughness = 6,
            colors = setOf(Color.BLACK, Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING, Keyword.MENACE),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                    val artifact = target(TargetFilter(GameObjectFilter.Artifact.controlledByTriggeringPlayer()))
                    effect = Effects.GainControl(artifact)
                },
            ),
            imageUri = "https://cards.scryfall.io/normal/front/1/1/11335886-a422-42ff-be14-226602202603.jpg?1783916054",
        )
    }

    keywordAbility(KeywordAbility.flashback("{6}{B}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Henry Peters"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9cf5f78-0316-4c35-a40e-9bfba2e46ad8.jpg?1783916013"
    }
}
