package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rosecot Knight
 * {4}{W}
 * Creature — Human Knight
 * 3/4
 *
 * Vigilance
 * When this creature enters, look at the top six cards of your library. You may reveal an
 * artifact or enchantment card from among them and put it into your hand. Put the rest on the
 * bottom of your library in a random order. If you didn't put a card into your hand this way,
 * put a +1/+1 counter on this creature.
 *
 * The "didn't put a card into your hand" rider reads the kept collection: an empty pick (declined,
 * or no artifact/enchantment among the six) adds the counter.
 */
val RosecotKnight = card("Rosecot Knight") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Vigilance\nWhen this creature enters, look at the top six cards of your library. You may reveal an artifact or enchantment card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. If you didn't put a card into your hand this way, put a +1/+1 counter on this creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.ArtifactOrEnchantment,
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom"
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
            run(Effects.If(
                condition = Conditions.Not(whenMatches(kept)),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Drew Baker"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ae7df20-1ccf-459b-b81c-ab8763c77ccf.jpg?1783911296"
    }
}
