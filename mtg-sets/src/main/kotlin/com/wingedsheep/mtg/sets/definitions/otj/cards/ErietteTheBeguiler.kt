package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Eriette, the Beguiler
 * {1}{W}{U}{B}
 * Legendary Creature — Human Warlock
 * 4/4
 *
 * Lifelink
 * Whenever an Aura you control becomes attached to a nonland permanent an opponent controls with
 * mana value less than or equal to that Aura's mana value, gain control of that permanent for as
 * long as that Aura is attached to it.
 *
 * Modeling:
 * - The trigger is [Triggers.becomesAttached] with an ANY binding (it watches *any* Aura you
 *   control, not just Eriette), gated by the attached-to filter "nonland permanent an opponent
 *   controls with mana value ≤ the Aura's mana value". The mana-value comparison uses
 *   [GameObjectFilter.manaValueAtMostEntity] with [EntityReference.Triggering] = the attaching Aura
 *   (the trigger matcher exposes the attachment as the comparison reference).
 * - The payoff gains control of [EffectTarget.AttachedToTriggeringPermanent] (the permanent the
 *   Aura attached to) for [Duration.WhileSourceAttachedToAffected] — the control effect is sourced
 *   from the *Aura*, so it ends the instant the Aura leaves, detaches, or re-attaches elsewhere
 *   (CR 611.2b).
 */
val ErietteTheBeguiler = card("Eriette, the Beguiler") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n" +
        "Whenever an Aura you control becomes attached to a nonland permanent an opponent controls " +
        "with mana value less than or equal to that Aura's mana value, gain control of that " +
        "permanent for as long as that Aura is attached to it."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.becomesAttached(
            attachmentFilter = GameObjectFilter.Enchantment.withSubtype("Aura"),
            attachmentController = Player.You,
            attachedToFilter = GameObjectFilter.NonlandPermanent
                .opponentControls()
                .manaValueAtMostEntity(EntityReference.Triggering),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.GainControl(
            EffectTarget.AttachedToTriggeringPermanent,
            duration = Duration.WhileSourceAttachedToAffected,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f46c133a-7ae4-431b-88f2-ec606a7baf69.jpg?1712356086"
    }
}
