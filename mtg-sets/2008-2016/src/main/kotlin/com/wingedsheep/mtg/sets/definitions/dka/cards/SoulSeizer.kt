package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Soul Seizer // Ghastly Haunting (Dark Ascension #50)
 *
 * "That player" is the damaged player, bound as the triggering player, so the target is a creature
 * *they* control (right in multiplayer). The target is chosen as the trigger goes on the stack; the
 * transform choice is made on resolution. If the target is illegal by then, the whole ability
 * doesn't resolve and Soul Seizer doesn't transform (2011-01-22 ruling).
 *
 * "If you do" gates on the transform actually happening ([SuccessCriterion.Transformed]), and the
 * may is skipped outright when Soul Seizer has left the battlefield — so a Soul Seizer that left in
 * response is never transformed or attached (2011-01-22 ruling). Once transformed it is an Aura, and
 * [Effects.AttachEquipment] attaches the source to the target in the same resolution, before
 * state-based actions look at an unattached Aura.
 */
private val SoulSeizerFront = card("Soul Seizer") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature deals combat damage to a player, you may transform it. If you do, attach it " +
        "to target creature that player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val host = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Transform(EffectTarget.Self),
                then = Effects.AttachEquipment(host),
                successCriterion = SuccessCriterion.Transformed,
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            prompt = "Transform Soul Seizer and attach it to the target creature?",
        )
        description = "When this creature deals combat damage to a player, you may transform it. If you do, " +
            "attach it to target creature that player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f500cb95-d5ea-4cf2-920a-f1df45a9059b.jpg?1783940842"
        ruling(
            "2011-01-22",
            "You choose the target for Soul Seizer's triggered ability when that ability triggers and goes on " +
                "the stack. You choose whether or not to transform it when that ability resolves. If the creature " +
                "is an illegal target by that time, the ability doesn't resolve and none of its effects happen. " +
                "You can't have Soul Seizer transform.",
        )
        ruling(
            "2011-01-22",
            "If Soul Seizer leaves the battlefield in response to its triggered ability, you won't be able to " +
                "transform it and it won't return to the battlefield or become attached to the target.",
        )
        ruling(
            "2011-01-22",
            "If Soul Seizer has any +1/+1 counters on it when it transforms, they remain on Ghastly Haunting. " +
                "They won't affect the power or toughness of the enchanted creature, however.",
        )
    }
}

private val GhastlyHaunting = card("Ghastly Haunting") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nYou control enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Lucas Graciano"
        flavorText = "The body often twists and flails spasmodically as the spirit explores its new home."
        imageUri = "https://cards.scryfall.io/normal/back/f/5/f500cb95-d5ea-4cf2-920a-f1df45a9059b.jpg?1783940842"
    }
}

val SoulSeizer: CardDefinition = CardDefinition.doubleFacedPermanent(SoulSeizerFront, GhastlyHaunting)
