package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sigardian Savior (Innistrad: Midnight Hunt #34)
 * {3}{W}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * When this creature enters, if you cast it, return up to two target creature cards with mana value
 * 2 or less from your graveyard to the battlefield.
 *
 * `Conditions.WasCast` intervening-if over an "up to two target" graveyard requirement
 * (`TargetObject(count = 2, optional = true)`), each returned independently by
 * [Effects.ForEachTarget] — the Dewdrop Cure shape on an ETB trigger.
 */
val SigardianSavior = card("Sigardian Savior") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nWhen this creature enters, if you cast it, return up to two target creature cards " +
        "with mana value 2 or less from your graveyard to the battlefield."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2),
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
        )
        description = "When this creature enters, if you cast it, return up to two target creature cards " +
            "with mana value 2 or less from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "David Rapoza"
        flavorText = "\"Begone, moon-cursed! These folk are no prey of yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1a2b41a-34b0-428e-80ec-3ffd601093a2.jpg?1783925651"
    }
}
