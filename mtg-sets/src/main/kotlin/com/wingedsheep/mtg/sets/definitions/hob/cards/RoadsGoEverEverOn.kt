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
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.BasicLand.withSubtype("Plains"),
                    ),
                    storeAs = "roadsSearchable",
                ),
                SelectFromCollectionEffect(
                    from = "roadsSearchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    storeSelected = "roadsExiled",
                    prompt = "Search your library for up to two basic Plains cards",
                ),
                MoveCollectionEffect(
                    from = "roadsExiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    linkToSource = true,
                ),
                ShuffleLibraryEffect(),
                Effects.GainLife(2),
            )
        )
    }

    sagaChapter(2) { effect = returnChosenRoad() }
    sagaChapter(3) { effect = returnChosenRoad() }

    sagaChapter(4) {
        val plainsCount = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype("Plains"),
        ).count()
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.YouAttack,
            targetRequirement = TargetCreature(filter = TargetFilter.Creature.youControl()),
            effect = Effects.ModifyStats(
                power = plainsCount,
                toughness = plainsCount,
                target = EffectTarget.ContextTarget(0),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c1ebd6-967f-4b8c-8f1f-442ce8c1da24.jpg?1784673434"
    }
}

private fun returnChosenRoad() = Effects.Composite(
    listOf(
        GatherCardsEffect(
            source = CardSource.FromLinkedExile(),
            storeAs = "roadsLinked",
        ),
        SelectFromCollectionEffect(
            from = "roadsLinked",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            storeSelected = "roadsReturned",
            prompt = "Choose a card exiled with Roads Go Ever, Ever On to put into its owner's hand",
        ),
        MoveCollectionEffect(
            from = "roadsReturned",
            destination = CardDestination.ToZone(Zone.HAND),
            unlinkFromSource = true,
        ),
    )
)
