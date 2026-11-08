package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step

/**
 * Weapons Vendor
 * {3}{W}
 * Creature — Human Artificer
 * 2/2
 * When this creature enters, draw a card.
 * At the beginning of combat on your turn, if you control an Equipment, you may pay {1}. When
 *   you do, attach target Equipment you control to target creature you control.
 *
 * The combat ability is the Spellbook Vendor shape: an intervening-"if" gates the trigger on
 * controlling an Equipment, then the optional {1} payment ([Effects.MayPay]) gates an
 * [Effects.ReflexiveTrigger] — the "when you do" ability, which chooses its targets as it goes on
 * the stack after the payment (Scryfall ruling). The reflexive payoff reuses
 * [Effects.AttachTargetEquipmentToCreature], moving the
 * chosen Equipment onto the chosen creature.
 */
val WeaponsVendor = card("Weapons Vendor") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, draw a card.\n" +
        "At the beginning of combat on your turn, if you control an Equipment, you may pay {1}. " +
        "When you do, attach target Equipment you control to target creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
        )
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "attach target Equipment you control " +
                    "to target creature you control.",
            ) {
                val equipment = target(
                    TargetFilter(baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
                )
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Mushk Rizvi"
        flavorText = "\"Man in your line of work needs weapons, no? Why not try that one on for size?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9e6b374-3e44-4df7-b0a3-4ef98dc08267.jpg?1748705905"
    }
}
