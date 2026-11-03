package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.Duration

/**
 * Gwyndala, the Progeny
 * {1}{U}
 * Legendary Creature — Vau-N'Akat Advisor
 * 1/1
 * When Gwyndala enters, create a colorless Equipment artifact token named Neuroflux Heirloom with
 * "Equipped creature gets +1/+1" and equip {2}. Attach it to Gwyndala.
 * {2}: Each Equipment token you control becomes a copy of target Equipment you control until end of turn.
 *
 * The token is the predefined `Neuroflux Heirloom`; it is published to [CREATED_TOKENS] and attached
 * to Gwyndala in the same resolution (the Elven Bow shell, mirrored). The copy ability is the
 * Absorb Identity group-copy shape over Equipment tokens you control. Attachment is not a copiable
 * value, so becoming a copy leaves each token attached where it was — a copied Heirloom stays
 * on its creature.
 */
val GwyndalaTheProgeny = card("Gwyndala, the Progeny") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Vau-N'Akat Advisor"
    power = 1
    toughness = 1
    oracleText = "When Gwyndala enters, create a colorless Equipment artifact token named Neuroflux Heirloom " +
        "with \"Equipped creature gets +1/+1\" and equip {2}. Attach it to Gwyndala.\n" +
        "{2}: Each Equipment token you control becomes a copy of target Equipment you control until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePredefinedToken("Neuroflux Heirloom") then
            Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                creatureTarget = EffectTarget.Self,
            )
        description = "When Gwyndala enters, create a colorless Equipment artifact token named Neuroflux " +
            "Heirloom with \"Equipped creature gets +1/+1\" and equip {2}. Attach it to Gwyndala."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val equipment = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.EQUIPMENT).youControl())
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = equipment,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.EQUIPMENT).token().youControl()
            ),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Pierre Raveneau"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a82f5db5-787e-4e4a-ab3c-4a488c6f4990.jpg?1793187993"
    }
}
