package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infernal Phantom
 * {3}{R}
 * Creature — Spirit
 * 2/3
 *
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room,
 * this creature gets +2/+0 until end of turn.
 * When this creature dies, it deals damage equal to its power to any target.
 *
 * The Eerie ability word has no rules meaning — it's modeled as two triggers (enchantment-enters
 * and Room-fully-unlocked), each a self-pump via [Effects.ModifyStats] on [EffectTarget.Self].
 * The dies trigger uses [DynamicAmounts.sourcePower] (last-known power on death, CR 603.6e/608.2h)
 * to deal damage to [Targets.Any].
 */
val InfernalPhantom = card("Infernal Phantom") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Eerie — Whenever an enchantment you control enters and whenever you fully " +
        "unlock a Room, this creature gets +2/+0 until end of turn.\n" +
        "When this creature dies, it deals damage equal to its power to any target."

    keywords(Keyword.EERIE)

    // Eerie trigger — part 1: whenever an enchantment you control enters
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Eerie — Whenever an enchantment you control enters, this creature gets +2/+0 until end of turn."
    }

    // Eerie trigger — part 2: whenever you fully unlock a Room
    triggeredAbility {
        trigger = Triggers.RoomFullyUnlocked
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Eerie — Whenever you fully unlock a Room, this creature gets +2/+0 until end of turn."
    }

    // When this creature dies, it deals damage equal to its power to any target.
    triggeredAbility {
        trigger = Triggers.Dies
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), anyTarget)
        description = "When this creature dies, it deals damage equal to its power to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Mike Sass"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0c5999f-a185-4fc7-86fa-c4e5b091e768.jpg?1726286374"
    }
}
