package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Roads Go Ever, Ever On
 * {1}{W}
 * Enchantment — Saga
 *
 * I — Search your library for up to two basic Plains cards, exile them, then shuffle. You gain 2 life.
 * II, III — Put a card exiled with this Saga into its owner's hand.
 * IV — Whenever you attack this turn, target creature you control gets +1/+1 until end of turn for
 * each Plains you control.
 *
 * Chapter I links the selected cards to the Saga. Chapters II and III gather that live linked pile
 * and let the player choose one card rather than taking an arbitrary entry. Chapter IV installs an
 * event-based delayed trigger; its target and Plains count are chosen/evaluated when that trigger
 * fires, not when the chapter resolves.
 */
val RoadsGoEverEverOn = card("Roads Go Ever, Ever On") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Search your library for up to two basic Plains cards, exile them, then shuffle. You gain 2 life.\n" +
        "II, III — Put a card exiled with this Saga into its owner's hand.\n" +
        "IV — Whenever you attack this turn, target creature you control gets +1/+1 until end of " +
        "turn for each Plains you control."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val roadsSearchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.BasicLand.withSubtype("Plains"),
                ),
                search = true
            )
            val roadsExiled = chooseUpTo(
                2,
                from = roadsSearchable,
                prompt = "Search your library for up to two basic Plains cards"
            )
            exile(roadsExiled, linkToSource = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(Effects.GainLife(2))
        }
    }

    sagaChapter(2) { effect = returnChosenRoad() }
    sagaChapter(3) { effect = returnChosenRoad() }

    sagaChapter(4) {
        val plainsCount = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype("Plains"),
        ).count()
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.attacks(),
        ) {
            val creature = target(TargetFilter.Creature.youControl())
            effect = Effects.ModifyStats(
                power = plainsCount,
                toughness = plainsCount,
                target = creature,
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c1ebd6-967f-4b8c-8f1f-442ce8c1da24.jpg?1784673434"
    }
}

private fun returnChosenRoad() = Effects.Pipeline {
    val roadsLinked = gather(CardSource.FromLinkedExile())
    val roadsReturned = chooseExactly(
        1,
        from = roadsLinked,
        prompt = "Choose a card exiled with Roads Go Ever, Ever On to put into its owner's hand"
    )
    move(roadsReturned, CardDestination.ToZone(Zone.HAND), unlinkFromSource = true)
}
