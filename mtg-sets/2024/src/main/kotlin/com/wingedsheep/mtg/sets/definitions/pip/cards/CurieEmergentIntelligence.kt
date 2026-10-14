package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Curie, Emergent Intelligence — Fallout #30.
 *
 * "The exiled creature" is the permanent the cost exiled, copied as it last existed on the
 * battlefield (CR 608.2h, CR 707.2): a creature that was itself copying something is copied as
 * that, and its counters, Auras and non-copy effects are not (the 2024-03-08 rulings). The copy
 * keeps the draw trigger as a copy exception, so a second activation still has it.
 */
private fun curieDrawTrigger(): TriggeredAbility = grantedTriggeredAbility {
    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
    effect = Effects.DrawCards(DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.BasePower))
    description = "Whenever this creature deals combat damage to a player, draw cards equal to its base power."
}

val CurieEmergentIntelligence = card("Curie, Emergent Intelligence") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 1
    toughness = 3
    oracleText = "Whenever Curie deals combat damage to a player, draw cards equal to its base power.\n" +
        "{1}{U}, Exile another nontoken artifact creature you control: Curie becomes a copy of the exiled creature, " +
        "except it has \"Whenever this creature deals combat damage to a player, draw cards equal to its base power.\""
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.BasePower))
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            // The battlefield zone map is keyed by owner, so `youControl()` enforces "you control".
            Costs.ExilePermanentsFixed(
                1, GameObjectFilter.ArtifactCreature.nontoken().youControl(), excludeSelf = true,
            ),
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = EffectTarget.ExiledAsCost(),
            affected = EffectTarget.Self,
            exceptions = CopyExceptions(addedTriggeredAbilities = listOf(curieDrawTrigger())),
        )
        description = "{1}{U}, Exile another nontoken artifact creature you control: Curie becomes a copy of the " +
            "exiled creature, except it has \"Whenever this creature deals combat damage to a player, draw cards " +
            "equal to its base power.\""
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Daniel Romanovsky"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54870f19-96f3-4cde-ae44-a9c5bbb0dbc1.jpg?1783912413"
        ruling("2024-03-08", "Normally, a creature’s base power is the power printed on the card, or, for a token, the power set by the effect that created it. If another effect sets a creature’s power to a specific number or value, that becomes its base power. If an effect modifies a creature’s power without setting it, that is not included when determining its base power.")
        ruling("2024-03-08", "If a creature has a characteristic-defining ability that sets its power, indicated with a * or similar in the power and toughness box, that ability is taken into account when determining its base power.")
        ruling("2024-03-08", "If Curie is no longer on the battlefield when its first ability resolves, use its base power as it last existed on the battlefield to determine how many cards to draw.")
        ruling("2024-03-08", "Curie’s last ability refers to the exiled creature, not the exiled card. In most cases, this means Curie copies exactly what was printed on the original creature and nothing else, plus the additional ability. However, if the exiled creature was copying something else when it was last on the battlefield, Curie copies whatever that creature was copying, plus the additional ability.")
        ruling("2024-03-08", "Curie’s last ability won’t copy whether the exiled creature was tapped or untapped, whether it had any counters on it or Auras attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
        ruling("2024-03-08", "If the copied creature has {X} in its mana cost, X is 0.")
    }
}
