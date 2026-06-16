package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Miriam, Herd Whisperer
 * {G}{W}
 * Legendary Creature — Human Druid
 * 3/2
 *
 * During your turn, Mounts and Vehicles you control have hexproof.
 * Whenever a Mount or Vehicle you control attacks, put a +1/+1 counter on it.
 *
 * The static grant uses [Conditions.IsYourTurn] to scope the [GrantKeyword] to your own turn. The
 * affected group is *any permanent* you control with the Mount or Vehicle subtype — uncrewed Vehicles
 * are noncreature artifacts but still get hexproof, so the filter is `Any` (not `Creature`).
 *
 * The attack trigger fires for any Mount or Vehicle you control declaring as an attacker
 * ([Triggers.attacks] with ANY binding) and puts a +1/+1 counter on that attacker via
 * [EffectTarget.TriggeringEntity]. Only crewed Vehicles / Mounts can actually attack, so the trigger's
 * filter and the attack rules naturally agree.
 */
val MiriamHerdWhisperer = card("Miriam, Herd Whisperer") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Legendary Creature — Human Druid"
    power = 3
    toughness = 2
    oracleText = "During your turn, Mounts and Vehicles you control have hexproof.\n" +
        "Whenever a Mount or Vehicle you control attacks, put a +1/+1 counter on it."

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter(GameObjectFilter.Any.withAnySubtype("Mount", "Vehicle").youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Any.withAnySubtype("Mount", "Vehicle").youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a Mount or Vehicle you control attacks, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Viko Menezes"
        flavorText = "\"Anything willing to bear your weight and save your feet a trip across the hot " +
            "sands deserves respect. Show some.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfa7750a-7c32-4413-b762-62e24d992c6b.jpg?1712356167"
    }
}
