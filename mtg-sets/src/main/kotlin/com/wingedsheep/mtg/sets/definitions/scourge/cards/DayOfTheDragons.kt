package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Day of the Dragons
 * {4}{U}{U}{U}
 * Enchantment
 *
 * When Day of the Dragons enters the battlefield, exile all creatures you control.
 * Then create that many 5/5 red Dragon creature tokens with flying.
 *
 * When Day of the Dragons leaves the battlefield, sacrifice all Dragons you control.
 * Then return the exiled cards to the battlefield under your control.
 */
val DayOfTheDragons = card("Day of the Dragons") {
    manaCost = "{4}{U}{U}{U}"
    typeLine = "Enchantment"
    oracleText = "When Day of the Dragons enters the battlefield, exile all creatures you control. Then create that many 5/5 red Dragon creature tokens with flying.\nWhen Day of the Dragons leaves the battlefield, sacrifice all Dragons you control. Then return the exiled cards to the battlefield under your control."

    // ETB: Exile all creatures you control, create that many Dragon tokens
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ExileGroupAndLink(GroupFilter.AllCreaturesYouControl)
            .then(
                CreateTokenEffect(
                    count = DynamicAmount.VariableReference("linked_exile_count"),
                    power = 5,
                    toughness = 5,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Dragon"),
                    keywords = setOf(Keyword.FLYING)
                )
            )
    }

    // LTB: Sacrifice all Dragons you control, return exiled cards
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = ForceSacrificeEffect(
            filter = GameObjectFilter.Creature.withSubtype("Dragon"),
            count = 100,
            target = EffectTarget.Controller
        ).then(Effects.ReturnLinkedExile())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/366a934c-eb01-48c6-8393-c2fe0708ff91.jpg?1562527571"
        ruling("2017-11-17", "Auras attached to the exiled creatures will be put into their owners' graveyards. Equipment attached to the exiled creatures will become unattached and remain on the battlefield.")
        ruling("2017-11-17", "Token creatures count toward Dragon token creation but won't return when the enchantment leaves the battlefield.")
        ruling("2017-11-17", "All of the nontoken creatures you exiled return to the battlefield when Day of the Dragons leaves the battlefield, even if some or all of the Dragon tokens had already left the battlefield.")
    }
}
