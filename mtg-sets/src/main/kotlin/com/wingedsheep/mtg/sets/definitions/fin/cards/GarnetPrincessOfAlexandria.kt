package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Garnet, Princess of Alexandria
 * {G}{W}
 * Legendary Creature — Human Noble Cleric
 * 2/2
 *
 * Lifelink
 * Whenever Garnet attacks, you may remove a lore counter from each of any number of Sagas you
 * control. Put a +1/+1 counter on Garnet for each lore counter removed this way.
 *
 * Composed from atoms (no new effect): on attack, gather the Sagas you control that still have
 * a lore counter, let the controller pick any number of them (the "you may … any number" — zero
 * is a legal choice), remove one lore counter from each chosen Saga via
 * [ForEachInCollectionEffect] (`EffectTarget.Self` binds to the iterated Saga), then put that
 * many +1/+1 counters on Garnet. The gather filters to Sagas that have a lore counter so every
 * chosen Saga removes exactly one — making "lore counters removed this way" equal to the number
 * of chosen Sagas ([DynamicAmount.DistinctEntitiesInCollections] over the chosen collection).
 * Removing a lore counter only lowers the count, so it never triggers a chapter ability.
 */
val GarnetPrincessOfAlexandria = card("Garnet, Princess of Alexandria") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Noble Cleric"
    oracleText = "Lifelink\n" +
        "Whenever Garnet attacks, you may remove a lore counter from each of any number of " +
        "Sagas you control. Put a +1/+1 counter on Garnet for each lore counter removed this way."
    power = 2
    toughness = 2
    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                // Gather the Sagas you control that still have a lore counter to remove.
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Enchantment.withSubtype("Saga").withCounter(Counters.LORE)
                    ),
                    storeAs = "garnetSagas"
                ),
                // "you may … any number" — the controller chooses 0 or more of them.
                SelectFromCollectionEffect(
                    from = "garnetSagas",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.Controller,
                    storeSelected = "garnetChosen",
                    storeRemainder = "garnetRest",
                    prompt = "Choose any number of Sagas to remove a lore counter from each",
                    selectedLabel = "Remove a lore counter",
                    remainderLabel = "Leave unchanged",
                    useTargetingUI = true
                ),
                // Remove one lore counter from each chosen Saga.
                ForEachInCollectionEffect(
                    collection = "garnetChosen",
                    effect = Effects.RemoveCounters(Counters.LORE, 1, EffectTarget.Self)
                ),
                // Put a +1/+1 counter on Garnet for each lore counter removed this way.
                Effects.AddDynamicCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    DynamicAmount.DistinctEntitiesInCollections(listOf("garnetChosen")),
                    EffectTarget.Self
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Daniel Correia"
        flavorText = "\"Someday I will be queen, but I will always be myself.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b883df14-8d7b-4f6a-9a6a-2f71f5b6ddda.jpg?1748706601"
    }
}
