package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Harvestguard Alseids
 * {2}{W}
 * Enchantment Creature — Nymph
 * 2/3
 *
 * Constellation — Whenever this creature or another enchantment you control enters, prevent all
 * damage that would be dealt to target creature this turn.
 *
 * The Alseids is itself an enchantment, so one unbound watcher over enchantments you control
 * (`Triggers.a`) covers its own entry too (Eidolon of Blossoms' shape). The shield is a
 * recipient-side, all-damage, end-of-turn [Effects.PreventDamage] on the targeted creature.
 */
val HarvestguardAlseids = card("Harvestguard Alseids") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Nymph"
    power = 2
    toughness = 3
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, prevent " +
        "all damage that would be dealt to target creature this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventDamage(target = creature)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "prevent all damage that would be dealt to target creature this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Igor Kieryluk"
        flavorText = "Not all shields are forged of iron or bronze."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b9256e2-b59c-4997-91c1-4c565746d53d.jpg?1783939459"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
