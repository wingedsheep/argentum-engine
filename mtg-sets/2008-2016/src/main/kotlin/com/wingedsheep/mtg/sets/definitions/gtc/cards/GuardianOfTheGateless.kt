package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian of the Gateless
 * {4}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * This creature can block any number of creatures.
 * Whenever this creature blocks, it gets +1/+1 until end of turn for each creature it's blocking.
 *
 * The bonus counts the creatures it's blocking as the trigger resolves (the `blockedBySource()`
 * battlefield count, read live from combat state), and is locked in from then on. The blocks
 * trigger fires once however many attackers it blocks.
 */
val GuardianOfTheGateless = card("Guardian of the Gateless") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "This creature can block any number of creatures.\n" +
        "Whenever this creature blocks, it gets +1/+1 until end of turn for each creature it's blocking."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanBlockAnyNumber()
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        val blocked = DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.blockedBySource()).count()
        effect = Effects.ModifyStats(
            power = blocked,
            toughness = blocked,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86940635-7001-4ecf-b4ee-25aa1e2d81fc.jpg"
    }
}
