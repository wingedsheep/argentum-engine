package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aegis Sculptor — Tarkir: Dragonstorm #35
 * {3}{U} · Creature — Bird Wizard · 2/3
 *
 * Flying
 * Ward {2}
 * At the beginning of your upkeep, you may exile two cards from your graveyard. If you do,
 * put a +1/+1 counter on this creature.
 *
 * The upkeep ability is an optional "exile exactly two" pipeline gated by [IfYouDoEffect]: gather
 * your graveyard, let the player choose exactly two cards, move them to exile, and only add the
 * counter when both cards were actually exiled ([SuccessCriterion.CollectionNonEmpty] with
 * `min = 2`). With fewer than two cards in the graveyard the player can decline / cannot complete
 * the exile, so the counter is not put on.
 */
val AegisSculptor = card("Aegis Sculptor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, " +
        "counter it unless that player pays {2}.)\n" +
        "At the beginning of your upkeep, you may exile two cards from your graveyard. If you do, " +
        "put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING, Keyword.WARD)
    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = MayEffect(
            IfYouDoEffect(
                action = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(zone = Zone.GRAVEYARD),
                            storeAs = "graveyardCards"
                        ),
                        SelectFromCollectionEffect(
                            from = "graveyardCards",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                            storeSelected = "toExile",
                            selectedLabel = "Exile"
                        ),
                        MoveCollectionEffect(
                            from = "toExile",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        )
                    )
                ),
                ifYouDo = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 2)
            )
        )
        description = "At the beginning of your upkeep, you may exile two cards from your graveyard. " +
            "If you do, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19c1417a-9719-46f6-8749-d92b93ce0529.jpg?1743204100"
    }
}
