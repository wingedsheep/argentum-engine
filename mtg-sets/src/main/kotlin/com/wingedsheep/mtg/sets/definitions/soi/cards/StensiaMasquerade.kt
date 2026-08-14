package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Stensia Masquerade — Shadows over Innistrad #184. */
val StensiaMasquerade = card("Stensia Masquerade") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Attacking creatures you control have first strike.\n" +
        "Whenever a Vampire you control deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "Madness {2}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Creature.attacking().youControl()),
        )
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            1,
            EffectTarget.TriggeringEntity,
        )
    }

    madness("{2}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48f1b024-d7d5-4e81-b016-06826e2b8bbf.jpg?1783937740"
        ruling("2016-04-08", "Losing or gaining first strike after first-strike damage has been dealt won't cause a creature to deal combat damage twice or to not deal combat damage.")
    }
}
