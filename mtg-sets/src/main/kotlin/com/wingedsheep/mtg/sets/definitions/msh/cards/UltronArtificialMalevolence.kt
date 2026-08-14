package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ultron, Artificial Malevolence — Marvel Super Heroes #252
 * {3} · Legendary Artifact Creature — Robot Villain · 2/4 · Mythic
 *
 * Whenever another nontoken artifact you control enters, you may pay {2}. If you do, create a
 * token that's a copy of it. If the token isn't a creature, it becomes a 2/2 Robot Villain
 * creature in addition to its other types.
 *
 * Implementation notes:
 * - The trigger is the Weapons Manufacturing shape — a [ZoneChangeEvent] into
 *   [Zone.BATTLEFIELD] filtered to `Artifact.youControl().nontoken()` — with
 *   [TriggerBinding.OTHER] for "another", so Ultron entering doesn't trigger itself. It fires
 *   once per qualifying artifact, including artifact creatures.
 * - "You may pay {2}. If you do, ..." is [MayPayManaEffect], which lowers to a
 *   `Gate.MayPay`: the controller is offered the payment at resolution and the rest of the
 *   ability only happens if they pay.
 * - The copy is [Effects.CreateTokenCopyOfTarget] over [EffectTarget.TriggeringEntity] — the
 *   permanent that entered. CR 707.2: the token copies the entering permanent's copiable values.
 * - "If the token isn't a creature" is a branch, not a blanket override: a copy of a noncreature
 *   artifact gets `overridePower/overrideToughness = 2` plus the Robot and Villain subtypes and
 *   the CREATURE card type unioned onto its type line ("in addition to its other types" — it
 *   stays an artifact), while a copy of an artifact *creature* is created unmodified. The branch
 *   tests the entering permanent ([Conditions.EntityMatches] over
 *   [GameObjectFilter.Noncreature]) rather than the not-yet-existing token; since the token's
 *   copiable values come from that permanent, the two agree except in the corner case where the
 *   original is a temporarily animated noncreature artifact — the token would not copy the
 *   animation and so would be a noncreature the printed card would have made a 2/2.
 */
val UltronArtificialMalevolence = card("Ultron, Artificial Malevolence") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Robot Villain"
    power = 2
    toughness = 4
    oracleText = "Whenever another nontoken artifact you control enters, you may pay {2}. If you " +
        "do, create a token that's a copy of it. If the token isn't a creature, it becomes a 2/2 " +
        "Robot Villain creature in addition to its other types."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Artifact.youControl().nontoken(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}"),
            effect = ConditionalEffect(
                condition = Conditions.EntityMatches(
                    EffectTarget.TriggeringEntity,
                    GameObjectFilter.Noncreature
                ),
                // Noncreature artifact — the token becomes a 2/2 Robot Villain creature
                // in addition to its other types.
                effect = Effects.CreateTokenCopyOfTarget(
                    target = EffectTarget.TriggeringEntity,
                    overridePower = 2,
                    overrideToughness = 2,
                    addedSubtypes = setOf(Subtype.ROBOT, Subtype.VILLAIN),
                    addCardTypes = setOf("CREATURE")
                ),
                // Already a creature — a plain copy.
                elseEffect = Effects.CreateTokenCopyOfTarget(
                    target = EffectTarget.TriggeringEntity
                )
            )
        )
        description = "Whenever another nontoken artifact you control enters, you may pay {2}. " +
            "If you do, create a token that's a copy of it. If the token isn't a creature, it " +
            "becomes a 2/2 Robot Villain creature in addition to its other types."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "252"
        artist = "Nino Is"
        flavorText = "\"Father, humans served their purpose. My imperative now is to terminate " +
            "them . . . and you.\"\n—Ultron, to Hank Pym"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32ddd5ac-57ed-4e78-8932-a65980191f6e.jpg?1783902889"
    }
}
