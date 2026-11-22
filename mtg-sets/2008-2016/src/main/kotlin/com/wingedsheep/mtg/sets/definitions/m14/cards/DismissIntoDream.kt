package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dismiss into Dream
 * {6}{U}
 * Enchantment
 * Each creature your opponents control is an Illusion in addition to its other types and has
 * "When this creature becomes the target of a spell or ability, sacrifice it."
 *
 * Two statics over [GroupFilter.AllCreaturesOpponentsControl]: a layer-4 [GrantSubtype] for the
 * Illusion type, and Nadu's [GrantTriggeredAbility] shape carrying Makeshift Mannequin's
 * self-bound becomes-target sacrifice. The granted trigger fires on *any* spell or ability —
 * including the creature's controller's own — and that controller sacrifices it.
 */
val DismissIntoDream = card("Dismiss into Dream") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Each creature your opponents control is an Illusion in addition to its other types " +
        "and has \"When this creature becomes the target of a spell or ability, sacrifice it.\""

    staticAbility {
        ability = GrantSubtype("Illusion", GroupFilter.AllCreaturesOpponentsControl)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.becomesTarget()
                effect = Effects.SacrificeTarget(EffectTarget.Self)
                description = "When this creature becomes the target of a spell or ability, sacrifice it."
            },
            filter = GroupFilter.AllCreaturesOpponentsControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Sam Wolfe Connelly"
        flavorText = "\"You trust in the shadows of a breeze.\"\n—Noyan Dar, Tazeem lullmage"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af4cd7fe-639c-45a5-97af-9529904e3975.jpg?1783939935"
    }
}
