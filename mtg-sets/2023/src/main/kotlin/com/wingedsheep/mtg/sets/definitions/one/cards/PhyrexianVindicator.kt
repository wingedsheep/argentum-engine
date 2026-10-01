package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Phyrexian Vindicator — Phyrexia: All Will Be One #27.
 *
 * A static [PreventDamage] replacement on itself (CR 615) whose rest — taking place right after
 * the prevention (CR 615.5) — is a reflexive triggered ability: "When damage is prevented this way" goes on the stack after the prevention,
 * with its "any other target" chosen then, and deals the amount that application prevented
 * ([DynamicAmounts.preventedDamage], inherited by the reflexive trigger). Damage that can't be
 * prevented is dealt, nothing is prevented, and the reflexive trigger never fires.
 */
val PhyrexianVindicator = card("Phyrexian Vindicator") {
    manaCost = "{W}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Horror"
    power = 5
    toughness = 5
    oracleText = "Flying\nIf damage would be dealt to this creature, prevent that damage. " +
        "When damage is prevented this way, this creature deals that much damage to any other target."

    keywords(Keyword.FLYING)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Self),
            onPrevented = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "this creature deals that much damage to any other target"
            ) {
                val victim = target(TargetOther(Targets.Any))
                effect = Effects.DealDamage(DynamicAmounts.preventedDamage(), victim)
            }
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Denys Tsiperko"
        flavorText = "\"Behold your inevitable defeat.\"\n—Elesh Norn"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e18780ce-add4-4346-8028-4bc3b4099d71.jpg?1783918075"
        ruling(
            "2023-02-04",
            "If multiple replacement or prevention effects try to modify damage that would be dealt to a creature, the controller of the creature chooses the order in which they apply."
        )
    }
}
