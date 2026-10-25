package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon's Disciple
 * {1}{W}
 * Creature — Human Monk
 * 1/3
 * As this creature enters, you may reveal a Dragon card from your hand. If you do or if you
 * control a Dragon, this creature enters with a +1/+1 counter on it.
 * Dragons you control have ward {1}.
 *
 * The as-enters clause is an [OnEnterRun] gate: if you already control a Dragon the counter is
 * unconditional (no reveal is asked, since it couldn't change the outcome); otherwise the
 * resolution-time behold offers the reveal. With no Dragon on your battlefield, "behold a Dragon"
 * can only be met by revealing one from hand — exactly the printed reveal — and its `ifBeheld`
 * places the counter.
 */
val DragonsDisciple = card("Dragon's Disciple") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 3
    oracleText = "As this creature enters, you may reveal a Dragon card from your hand. If you do or " +
        "if you control a Dragon, this creature enters with a +1/+1 counter on it.\n" +
        "Dragons you control have ward {1}. (Whenever a Dragon you control becomes the target of a " +
        "spell or ability an opponent controls, counter it unless that player pays {1}.)"

    val counter = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    replacementEffect(
        OnEnterRun(
            Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Dragon")),
                then = counter,
                otherwise = Effects.Behold(
                    GameObjectFilter.Any.withSubtype("Dragon"),
                    ifBeheld = counter,
                ),
            )
        )
    )

    staticAbility {
        ability = GrantWard(
            WardCost.Mana("{1}"),
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Dragon").youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5e6b78-3e99-49f5-9eaf-bbb2e9789fbe.jpg?1783926533"
    }
}
