package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Glacial Dragonhunt — Tarkir: Dragonstorm #188
 * {U}{R} · Sorcery · Uncommon
 *
 * Draw a card, then you may discard a card. When you discard a nonland card this way,
 * Glacial Dragonhunt deals 3 damage to target creature.
 * Harmonize {4}{U}{R}
 *
 * The "when you discard a nonland card this way" clause is a reflexive trigger: its target is
 * chosen only after — and only if — a nonland card is actually discarded. We model that exactly
 * by composing pipeline primitives rather than declaring a cast-time spell target:
 *   1. draw a card;
 *   2. gather the hand, let the controller discard UP TO one card (the printed "you may"), and
 *      move the choice to the graveyard as a discard;
 *   3. only when the discarded collection contains a [GameObjectFilter.Nonland] card, prompt for
 *      a target creature (on-battlefield targeting UI) and deal 3 damage to it.
 * If no card is discarded, or a land is discarded, no creature is chosen and no damage is dealt.
 */
val GlacialDragonhunt = card("Glacial Dragonhunt") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Draw a card, then you may discard a card. When you discard a nonland card this " +
        "way, Glacial Dragonhunt deals 3 damage to target creature.\n" +
        "Harmonize {4}{U}{R} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. " +
        "Then exile this spell.)"

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "discarded",
                    prompt = "You may discard a card"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                    moveType = MoveType.Discard
                ),
                // Reflexive trigger: only when a nonland was discarded do we pick a target creature
                // and deal it 3 damage.
                ConditionalOnCollectionEffect(
                    collection = "discarded",
                    filter = GameObjectFilter.Nonland,
                    ifNotEmpty = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.BattlefieldMatching(
                                    filter = GameObjectFilter.Creature,
                                    player = Player.Each
                                ),
                                storeAs = "creatures"
                            ),
                            SelectFromCollectionEffect(
                                from = "creatures",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                chooser = Chooser.Controller,
                                storeSelected = "damageTarget",
                                prompt = "Choose a creature to deal 3 damage to",
                                useTargetingUI = true
                            ),
                            Effects.DealDamage(
                                3,
                                EffectTarget.PipelineTarget("damageTarget"),
                                damageSource = EffectTarget.Self
                            )
                        )
                    )
                )
            )
        )
    }

    keywordAbility(KeywordAbility.harmonize("{4}{U}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Igor Grechanyi"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95994c88-e404-4a4f-8be6-b99d703d4609.jpg?1743204732"
    }
}
