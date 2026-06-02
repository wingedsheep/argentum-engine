package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dispelling Exhale
 * {1}{U}
 * Instant
 *
 * As an additional cost to cast this spell, you may behold a Dragon. (You may choose a
 * Dragon you control or reveal a Dragon card from your hand.)
 * Counter target spell unless its controller pays {2}. If a Dragon was beheld, counter that
 * spell unless its controller pays {4} instead.
 *
 * Implementation note: the optional behold (no real cost component) is modelled at
 * resolution time as a gather of your Dragons followed by a `ChooseUpTo(1)` selection
 * ("you may behold") storing the chosen Dragon under "beheld". The "{2}" vs "{4}" tax is
 * selected by a [ConditionalEffect] gated on [Conditions.CollectionContainsMatch] of that store.
 */
val DispellingExhale = card("Dispelling Exhale") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may behold a Dragon. " +
        "(You may choose a Dragon you control or reveal a Dragon card from your hand.)\n" +
        "Counter target spell unless its controller pays {2}. If a Dragon was beheld, " +
        "counter that spell unless its controller pays {4} instead."

    spell {
        target("target spell", Targets.Spell)
        effect = Effects.Composite(
            listOf(
                // Optional behold: gather your Dragons and choose up to one of them.
                GatherCardsEffect(
                    source = CardSource.FromMultipleZones(
                        zones = listOf(Zone.BATTLEFIELD, Zone.HAND),
                        player = Player.You,
                        filter = Filters.WithSubtype("Dragon")
                    ),
                    storeAs = "beholdable"
                ),
                SelectFromCollectionEffect(
                    from = "beholdable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "beheld",
                    prompt = "You may behold a Dragon"
                ),
                RevealCollectionEffect(from = "beheld"),
                ConditionalEffect(
                    condition = Conditions.CollectionContainsMatch("beheld"),
                    effect = Effects.CounterUnlessPays("{4}"),
                    elseEffect = Effects.CounterUnlessPays("{2}")
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c9af3f1-711e-42ae-803a-1100eba3fb13.jpg?1743204126"
    }
}
