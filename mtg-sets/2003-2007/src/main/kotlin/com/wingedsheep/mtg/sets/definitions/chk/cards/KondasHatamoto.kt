package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Konda's Hatamoto
 * {1}{W}
 * Creature — Human Samurai
 * 1/2
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 * As long as you control a legendary Samurai, this creature gets +1/+2 and has vigilance.
 *
 * Bushido is display-only vocabulary, so it is lowered to its two triggers exactly as in
 * [NumaiOutcast]. The lord half is a [ConditionalStaticAbility] over a composite of a self
 * stat bonus and a self keyword grant, gated on controlling a legendary Samurai *permanent*
 * (the Hatamoto itself is not legendary, so it never satisfies its own condition).
 */
val KondasHatamoto = card("Konda's Hatamoto") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 2
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)\n" +
        "As long as you control a legendary Samurai, this creature gets +1/+2 and has vigilance. " +
        "(Attacking doesn't cause this creature to tap.)"

    keywordAbility(KeywordAbility.bushido(1))

    // Bushido 1, half one: "Whenever this creature blocks …"
    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Bushido 1"
    }

    // Bushido 1, half two: "… or becomes blocked, it gets +1/+1 until end of turn."
    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Bushido 1"
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 1, toughnessBonus = 2, filter = GroupFilter.source()),
                    GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
                )
            ),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.legendary().withSubtype(Subtype.SAMURAI)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83425cf6-f6ad-4302-8f07-099eabdd4b40.jpg?1783944335"
    }
}
