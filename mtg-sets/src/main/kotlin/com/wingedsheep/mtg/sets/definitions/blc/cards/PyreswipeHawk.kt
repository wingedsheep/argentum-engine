package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pyreswipe Hawk
 * {3}{R}{R}
 * Creature — Elemental Bird, 4/4
 *
 * Flying, haste
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the
 *   greatest mana value among artifacts you control.
 * Whenever you expend 6, gain control of up to one target artifact for as long as
 *   you control this creature.
 */
val PyreswipeHawk = card("Pyreswipe Hawk") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Bird"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest mana value among artifacts you control.\n" +
        "Whenever you expend 6, gain control of up to one target artifact for as long as you control this creature. (You expend 6 as you spend your sixth total mana to cast spells during a turn.)"

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(
            power = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Artifact,
                aggregation = Aggregation.MAX,
                property = CardNumericProperty.MANA_VALUE
            ),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self
        )
    }

    triggeredAbility {
        trigger = Triggers.Expend(6)
        val artifact = target(
            "up to one target artifact",
            TargetPermanent(optional = true, filter = TargetFilter.Artifact)
        )
        effect = Effects.GainControl(
            target = artifact,
            duration = Duration.WhileSourceOnBattlefield("Pyreswipe Hawk")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cf88f89-9490-4477-b632-85c513e8c749.jpg?1721428266"
        ruling("2024-07-26", "The value of X is calculated only once, as Pyreswipe Hawk's third ability resolves.")
        ruling("2024-07-26", "If Pyreswipe Hawk leaves the battlefield before its last ability resolves, control of the target artifact won't change at all.")
        ruling("2024-07-26", "Abilities that trigger whenever you \"expend N\" only trigger when you reach that specific amount of mana spent on casting spells that turn. This can only happen once per turn.")
    }
}
