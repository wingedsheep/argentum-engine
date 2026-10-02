package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wastescape Battlemage
 * {1}{C}
 * Creature — Eldrazi Wizard
 * 2/2
 *
 * Kicker {G} and/or {1}{U}
 * When you cast this spell, if it was kicked with its {G} kicker, exile target artifact or
 *   enchantment an opponent controls.
 * When you cast this spell, if it was kicked with its {1}{U} kicker, return target creature an
 *   opponent controls to its owner's hand.
 *
 * - "Kicker [A] and/or [B]" is two kicker abilities (CR 702.33b), declared in printed order; each
 *   may be paid independently and the cast is offered as "Kicked {G}", "Kicked {1}{U}", or both.
 * - Each cast trigger is linked to its own kicker (CR 702.33f) through
 *   [Conditions.WasKickedWithFirstKicker] / [Conditions.WasKickedWithSecondKicker] as an
 *   intervening "if", so an unpaid kicker's trigger never goes on the stack or asks for a target.
 */
val WastescapeBattlemage = card("Wastescape Battlemage") {
    manaCost = "{1}{C}"
    colorIdentity = "GU"
    typeLine = "Creature — Eldrazi Wizard"
    power = 2
    toughness = 2
    oracleText = "Kicker {G} and/or {1}{U}\n" +
        "When you cast this spell, if it was kicked with its {G} kicker, exile target artifact or " +
        "enchantment an opponent controls.\n" +
        "When you cast this spell, if it was kicked with its {1}{U} kicker, return target creature an " +
        "opponent controls to its owner's hand."

    keywordAbility(KeywordAbility.kicker("{G}"))
    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.WasKickedWithFirstKicker
        val permanent = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment.opponentControls()))
        effect = Effects.Exile(permanent)
        description = "When you cast this spell, if it was kicked with its {G} kicker, exile target " +
            "artifact or enchantment an opponent controls."
    }

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.WasKickedWithSecondKicker
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.ReturnToHand(creature)
        description = "When you cast this spell, if it was kicked with its {1}{U} kicker, return " +
            "target creature an opponent controls to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bc119b8-429c-4ab6-adba-b65b03810e98.jpg?1783911303"
    }
}
