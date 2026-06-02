package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Roving Actuator
 * {3}{R}
 * Artifact Creature — Robot
 * 3/4
 *
 * Void — When this creature enters, if a nonland permanent left the battlefield this turn
 * or a spell was warped this turn, exile up to one target instant or sorcery card with mana
 * value 2 or less from your graveyard. Copy it. You may cast the copy without paying its mana
 * cost.
 *
 * Composed entirely from existing primitives — Shiko-style exile → copy → may-cast pipeline
 * (CR 707.12) wrapped in a Void-gated ETB trigger ([Conditions.Void]). The "up to one" wording
 * surfaces as `TargetObject(optional = true)` so the controller may decline targeting if no
 * legal card exists.
 */
val RovingActuator = card("Roving Actuator") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot"
    power = 3
    toughness = 4
    oracleText = "Void — When this creature enters, if a nonland permanent left the battlefield " +
        "this turn or a spell was warped this turn, exile up to one target instant or sorcery " +
        "card with mana value 2 or less from your graveyard. Copy it. You may cast the copy " +
        "without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.Void
        description = "Void — When this creature enters, if a nonland permanent left the " +
            "battlefield this turn or a spell was warped this turn, exile up to one target " +
            "instant or sorcery card with mana value 2 or less from your graveyard. Copy it. " +
            "You may cast the copy without paying its mana cost."
        val exiledCard = target(
            "instant or sorcery card with mana value 2 or less from your graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.InstantOrSorcery.manaValueAtMost(2).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Composite(
            Effects.Move(exiledCard, Zone.EXILE),
            Effects.CopyCardIntoCollection(exiledCard, storeAs = "copy"),
            MayEffect(
                Effects.CastFromCollectionWithoutPayingCost("copy"),
                descriptionOverride = "You may cast the copy without paying its mana cost.",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1111173b-ea49-4de4-b5b0-07d768c626b9.jpg?1752947187"
    }
}
