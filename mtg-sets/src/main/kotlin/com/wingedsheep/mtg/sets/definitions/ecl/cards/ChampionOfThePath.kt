package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Champion of the Path
 * {3}{R}
 * Creature — Elemental Sorcerer
 * 7/3
 *
 * As an additional cost to cast this spell, behold an Elemental and exile it.
 * (Exile an Elemental you control or an Elemental card from your hand.)
 * Whenever another Elemental you control enters, it deals damage equal to its power to each opponent.
 * When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
val ChampionOfThePath = card("Champion of the Path") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Sorcerer"
    power = 7
    toughness = 3
    oracleText = "As an additional cost to cast this spell, behold an Elemental and exile it. " +
        "(Exile an Elemental you control or an Elemental card from your hand.)\n" +
        "Whenever another Elemental you control enters, it deals damage equal to its power to each opponent.\n" +
        "When this creature leaves the battlefield, return the exiled card to its owner's hand."

    additionalCost(AdditionalCost.BeholdAndExile(filter = Filters.WithSubtype("Elemental")))

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL).youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.DealDamage(
            amount = DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.TriggeringEntity
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e369cd31-3e22-47eb-bf6a-00d823651710.jpg?1767952132"

        ruling("2025-11-17", "If Champion of the Path is countered or otherwise fails to resolve, the beheld card will remain in exile indefinitely.")
        ruling("2025-11-17", "If the Elemental that caused Champion of the Path's second ability to trigger is no longer on the battlefield when that ability resolves, use that Elemental's power as it last existed on the battlefield to determine how much damage is dealt.")
        ruling("2025-11-17", "If an Elemental you control that isn't a creature enters (probably because it's a kindred permanent with the Elemental subtype), Champion of the Path's second ability will still trigger. If that Elemental still isn't a creature when the ability resolves, the ability won't deal any damage.")
    }
}
