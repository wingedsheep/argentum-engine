package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Savior of the Sleeping
 * {2}{W}
 * Creature — Human Knight
 * 2/3
 *
 * Vigilance
 * Whenever an enchantment you control is put into a graveyard from the battlefield, put a
 * +1/+1 counter on this creature.
 *
 * Same shape as Warehouse Tabby: the trigger fires on *any* enchantment you control reaching
 * the graveyard from the battlefield — destroyed, sacrificed, an Aura falling off, or a Role
 * token being replaced — so it uses the generic `leavesBattlefield` factory with an
 * [TriggerBinding.ANY] binding rather than a SELF-bound constant.
 */
val SaviorOfTheSleeping = card("Savior of the Sleeping") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Vigilance\n" +
        "Whenever an enchantment you control is put into a graveyard from the battlefield, " +
        "put a +1/+1 counter on this creature."
    power = 2
    toughness = 3

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Valera Lutfullina"
        flavorText = "Even lost in slumber, the citizens of Ardenvale dream of a tireless protector " +
            "standing guard over their homes."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b4979d9-fafb-4e0e-868f-f4772109d7a7.jpg?1783915126"
    }
}
